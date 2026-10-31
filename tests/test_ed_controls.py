"""Portable ED regression checks; private APKs are not required."""
import re
import tempfile
import unittest
from pathlib import Path

from ad_onboarding import parse, digest
from verify_ad import verify_filters
from verify_ed import OWNER, active_controls, restored


class EdControlTests(unittest.TestCase):
    def method(self, body):
        return parse(".method public run()Z\n.locals 2\n" + body + "\n.end method", "run()Z")

    def test_tracking_and_consent_include_only_their_audited_dependencies(self):
        flags = active_controls({"Skip consent screens"})
        self.assertTrue(flags["ads"] and flags["tracking"] and flags["consent"])
        self.assertFalse(flags["premium"] or flags["full"] or flags["marketing"])
        flags = active_controls({"Read full articles"})
        self.assertTrue(flags["full"] and flags["marketing"])
        self.assertFalse(flags["ads"] or flags["tracking"] or flags["filters"])

    def test_selected_typed_prefix_preserves_original_cfg(self):
        original = self.method("const/4 v0, 0x1\nif-eqz v0, :done\nconst/4 v0, 0x0\n:done\nreturn v0")
        emitted = self.method("const/4 v0, 0x0\nreturn v0\nconst/4 v0, 0x1\nif-eqz v0, :done\nconst/4 v0, 0x0\n:done\nreturn v0")
        row = {"class": "fixture", "signature": "run()Z", "control": "tracking", "prefix": ["const/4 v0, 0x0", "return v0"], "sites": []}
        self.assertEqual(digest(restored(emitted, row, active_controls({"Disable tracking"}))), digest(original.canonical()))
        self.assertNotEqual(digest(restored(emitted, row, active_controls(set()))), digest(original.canonical()))
        forged = self.method("const/4 v0, 0x1\nreturn v0")
        with self.assertRaises(AssertionError):
            restored(forged, row, active_controls({"Disable tracking"}))

    def test_constructor_branch_must_not_bypass_disabled_flag(self):
        original = self.method("if-eqz p0, :write\nconst/4 v0, 0x1\n:write\niput-boolean v0, p0, Lfixture;->enabled:Z\nreturn v0")
        safe = self.method("if-eqz p0, :write\nconst/4 v0, 0x1\n:write\nconst/4 v0, 0x0\niput-boolean v0, p0, Lfixture;->enabled:Z\nreturn v0")
        bypass = self.method("if-eqz p0, :write\nconst/4 v0, 0x1\nconst/4 v0, 0x0\n:write\niput-boolean v0, p0, Lfixture;->enabled:Z\nreturn v0")
        row = {"class": "fixture", "signature": "run()Z", "control": "ads", "prefix": [], "sites": [{"index": 2, "old": ["iput-boolean v0, p0, Lfixture;->enabled:Z"], "new": ["const/4 v0, 0x0", "iput-boolean v0, p0, Lfixture;->enabled:Z"]}]}
        active = active_controls({"Hide ads"})
        self.assertEqual(digest(restored(safe, row, active)), digest(original.canonical()))
        # A branch directly to iput skips the forced value. Preservation must
        # retain this distinction even though all opcode text is otherwise equal.
        self.assertNotEqual(digest(restored(bypass, row, active)), digest(original.canonical()))

    def fixture(self, ads, premium):
        source = (Path(__file__).parents[1] / "patches/src/main/kotlin/patches/ed/ContentFiltersPatch.kt").read_text()
        methods = []
        model, owner = "Lcom/kubus/module/network/model/", "L" + OWNER + ";"
        for name, result, params, locals_, body in re.findall(r'helper\("(morphe\w+)", "([^"]+)", (listOf\([^\n]+?\)|emptyList\(\)), (\d+), """(.*?)"""\)', source, re.S):
            values = re.findall(r'"([^"]+)"', params)
            body = body.replace("${MODEL}", model).replace("$FILTER", owner).replace("${'$'}", "$")
            values = [s.replace("${MODEL}", model) for s in values]
            result = result.replace("${MODEL}", model)
            methods.append(f".method public static {name}(" + "".join(values) + ")" + result + "\n.locals " + locals_ + "\n" + body + "\n.end method")
        self.assertGreater(len(methods), 5)
        for name, enabled in [("morpheAds", ads), ("morphePremium", premium)]:
            methods.append(f".method public static {name}()Z\n.locals 1\nconst/4 v0, 0x{int(enabled):x}\nreturn v0\n.end method")
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        root = Path(temp.name)
        path = root / "smali" / (OWNER + ".smali")
        path.parent.mkdir(parents=True)
        path.write_text(".class public " + owner + "\n.super Ljava/lang/Object;\n" + "\n".join(methods))
        return root

    def test_ed_emitted_dto_policy_for_every_independent_flag_combination(self):
        for ads, premium in [(False, False), (True, False), (False, True), (True, True)]:
            with self.subTest(ads=ads, premium=premium):
                verify_filters(self.fixture(ads, premium), ads, premium)


if __name__ == "__main__":
    unittest.main()
