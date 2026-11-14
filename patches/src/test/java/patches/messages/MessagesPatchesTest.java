package patches.messages;

import app.morphe.patcher.patch.ApkFileType;
import app.morphe.patcher.patch.AppTarget;
import app.morphe.patcher.patch.Compatibility;
import app.morphe.patcher.patch.Patch;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/** Inspect the compiled Messages patches, as consumed by Manager and the bundle generator. */
public class MessagesPatchesTest {
    private static final List<Patch<?>> PATCHES = List.of(
        CarHostPatchKt.getCarHostPatch(),
        ScreenShareWarningPatchKt.getScreenShareWarningPatch(),
        ChangePackageNamePatchKt.getChangePackageNamePatch(),
        CustomBrandingPatchKt.getCustomBrandingPatch());

    @Test public void everyPatchTargetsTheExperimentalPhoneBuild() {
        for (Patch<?> patch : PATCHES) {
            assertFalse(patch.getName() + " is off by default while experimental", patch.getDefault());
            assertTrue(patch.getName() + " says it is experimental", patch.getDescription().startsWith("Experimental."));
            List<Compatibility> apps = patch.getCompatibility();
            assertNotNull(apps);
            assertEquals(1, apps.size());
            Compatibility app = apps.get(0);
            assertEquals("Google Messages", app.getName());
            assertEquals("com.google.android.apps.messaging", app.getPackageName());
            assertEquals(ApkFileType.APKM, app.getApkFileType());
            assertEquals("Google's current and original Messages signers are pinned", Set.of(
                "3284b53cbc699d6adf924945cc5a26dc5a0066b1d6767ffcd7bca44d0ea9fab8",
                "cc75526d6c0f80ac3b3a84eb44838440dbbe6b124443a7c7d7bd19182ac022c9"), Set.copyOf(app.getSignatures()));
            assertEquals(1, app.getTargets().size());
            AppTarget target = app.getTargets().get(0);
            assertEquals("messages.android_20261106_00_RC01.phone_dynamic", target.getVersion());
            assertEquals(Set.of(328483063), new HashSet<>(target.getVersionCodes().values()));
            assertEquals(32, target.getMinSdk().intValue());
            assertEquals("Universal archive: every architecture shares the build", 4, target.getVersionCodes().size());
            assertTrue("Released as experimental", target.isExperimental());
        }
    }

    @Test public void everyPatchSurvivesPlayServicesRefusingTheCopy() {
        // Any one of the patches re-signs Messages, and a re-signed copy crashed at start-up.
        Patch<?> fix = PlayServicesRefusalPatchKt.getPlayServicesRefusalPatch();
        for (Patch<?> patch : PATCHES) {
            assertTrue(patch.getName() + " includes the Play services refusal fix", patch.getDependencies().contains(fix));
        }
    }

    @Test public void anyHostIsAdmittedWithoutAHostList() {
        Patch<?> patch = CarHostPatchKt.getCarHostPatch();
        assertEquals("Allow any Android Auto host", patch.getName());
        assertTrue("Admission must not be configurable through a host list", patch.getOptions().isEmpty());
        assertTrue(patch.getDescription().contains("whatever its package name or signing key"));
        assertTrue(patch.getDescription().contains("Google's Android Auto keeps working"));
    }

    @Test public void screenShareWarningSaysWhatItGivesUp() {
        Patch<?> patch = ScreenShareWarningPatchKt.getScreenShareWarningPatch();
        assertEquals("Disable screen-share warning", patch.getName());
        assertTrue(patch.getDescription().contains("also turns the warning off for real screen sharing"));
    }

    @Test public void packageNameDefaultsToTheMorphePackageAndRejectsGoogleNames() {
        Patch<?> patch = ChangePackageNamePatchKt.getChangePackageNamePatch();
        assertEquals("Change package name", patch.getName());
        var option = patch.getOptions().get("packageName");
        assertEquals("app.morphe.android.apps.messaging", option.getDefault());
        for (String bad : List.of("com.google.android.apps.messaging", "com.google.android.apps.messaging.car",
                "com.google.android.messages", "nodots", "1bad.start")) {
            try {
                patch.getOptions().set("packageName", bad);
                fail("Accepted " + bad);
            } catch (Exception expected) {
                // rejected by the validator
            }
        }
        patch.getOptions().set("packageName", "nl.example.messages");
        assertEquals("nl.example.messages", option.getValue());
        patch.getOptions().set("packageName", "app.morphe.android.apps.messaging");
    }

    @Test public void identitiesMoveUnderTheNewPackage() {
        String old = "com.google.android.apps.messaging";
        String now = "app.morphe.android.apps.messaging";
        assertEquals(now, ChangePackageNamePatchKt.movedIdentity(old, old, now));
        assertEquals(now + ".shared.datamodel.BugleContentProvider",
            ChangePackageNamePatchKt.movedIdentity(old + ".shared.datamodel.BugleContentProvider", old, now));
        assertEquals(now + ":locationsharing.messagingextension.bubbles",
            ChangePackageNamePatchKt.movedIdentity(old + ":locationsharing.messagingextension.bubbles", old, now));
        // RCS, IMS and Compose identities in Google's wider namespace move too, or the two apps would collide.
        assertEquals(now + ".rcs.client.businessinfo",
            ChangePackageNamePatchKt.movedIdentity("com.google.android.rcs.client.businessinfo", old, now));
        assertEquals(now + ".messages.rcs.PROVISIONING_EVENT",
            ChangePackageNamePatchKt.movedIdentity("com.google.android.messages.rcs.PROVISIONING_EVENT", old, now));
        // A look-alike that is not under the package is not ours.
        assertEquals(now + ".apps.messagingx",
            ChangePackageNamePatchKt.movedIdentity("com.google.android.apps.messagingx", old, now));
        try {
            ChangePackageNamePatchKt.movedIdentity("org.example.provider", old, now);
            fail("Moved an identity outside Google's namespace");
        } catch (Exception expected) {
            // unexpected identities fail the patch
        }
    }

    @Test public void brandingBundlesEveryIconResource() throws Exception {
        Patch<?> patch = CustomBrandingPatchKt.getCustomBrandingPatch();
        assertEquals("Custom branding", patch.getName());
        assertEquals("Messages AA", patch.getOptions().get("customName").getDefault());
        assertEquals("aa", patch.getOptions().get("appIcon").getDefault());
        for (String resource : CustomBrandingPatchKt.getMessagesBrandingResources()) {
            try (InputStream in = MessagesPatchesTest.class.getResourceAsStream("/messages/branding/aa/" + resource)) {
                assertNotNull("Missing " + resource, in);
                assertTrue("Empty " + resource, in.readAllBytes().length > 0);
            }
        }
    }
}
