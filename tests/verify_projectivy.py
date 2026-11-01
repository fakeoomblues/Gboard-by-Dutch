"""Check all five Projectivy 4.73 effects in a real emitted APK decode."""
import argparse
from pathlib import Path
import re
from brabant_smali import canonical

TARGETS = [
    ("com/spocky/projengmenu/utils/PTUtils", "e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", None),
    ("yg1", "a(Z)V", None),
    ("t7", "c()V", None),
    ("ng1", "a(Ljava/lang/String;)Ljava/net/URL;", "https://firebaseinstallations.googleapis.com/v1/"),
    ("pt3", "a()Ljava/net/URL;", "firebase-settings.crashlytics.com"),
]


def body(root, owner, signature):
    files = list(root.glob("smali*/" + owner + ".smali"))
    assert len(files) == 1, owner
    methods = re.findall(r"^\.method .*?^\.end method", files[0].read_text(), re.M | re.S)
    return next(m for m in methods if m.splitlines()[0].endswith(" " + signature))


def verify(original, patched):
    for owner, signature, host in TARGETS:
        before = body(original, owner, signature)
        after = body(patched, owner, signature)
        if host is None:
            assert canonical(before).code[0] != "return-void", "Original already patched"
            assert canonical(after).code[0] == "return-void", owner + " still collects"
        else:
            assert before.count('"' + host + '"') == 1, "Original URL changed"
            expected = before.replace('"' + host + '"', '"localhost"')
            assert canonical(after).digest() == canonical(expected).digest(), owner + " URL edit differs"
        print("PASS:", owner + "->" + signature)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("original", type=Path)
    parser.add_argument("patched", type=Path)
    args = parser.parse_args()
    verify(args.original, args.patched)
