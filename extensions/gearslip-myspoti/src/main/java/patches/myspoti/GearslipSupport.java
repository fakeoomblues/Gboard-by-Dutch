package patches.myspoti;

/** Extends capability membership only; Myspoti retains its original caller UID checks. */
public final class GearslipSupport {
    private GearslipSupport() {}

    public static String getConfiguredPackageNames() {
        return "app.seb3thehacker.gearslip";
    }

    public static boolean isConfiguredPackage(String packageName) {
        if (packageName == null) return false;
        for (String configured : getConfiguredPackageNames().split(",")) {
            if (packageName.equals(configured.trim())) return true;
        }
        return false;
    }
}
