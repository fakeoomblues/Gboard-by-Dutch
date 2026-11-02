package patches.myspoti;

import app.morphe.patcher.patch.ApkFileType;
import app.morphe.patcher.patch.AppTarget;
import app.morphe.patcher.patch.Compatibility;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Inspect compiled patch metadata, as consumed by Manager and the bundle generator. */
public class CompatibilityTest {
    @Test public void anyVersionRemainsRestrictedToTheMyspotiApkPackage() {
        var patch = GearslipSupportPatchKt.getGearslipMyspotiSupport();
        assertEquals("Gearslip personal library", patch.getName());
        List<Compatibility> apps = patch.getCompatibility();
        assertNotNull(apps);
        assertEquals(1, apps.size());
        Compatibility app = apps.get(0);
        assertEquals("app.myspoti.stable", app.getPackageName());
        assertEquals(ApkFileType.APK, app.getApkFileType());
        assertEquals("Declare one unrestricted version target", 1, app.getTargets().size());
        AppTarget target = app.getTargets().get(0);
        assertNull("Any must not filter by version name", target.getVersion());
        assertNull("Any must not filter by build code or ABI", target.getVersionCodes());
        assertFalse(target.isExperimental());
    }
}
