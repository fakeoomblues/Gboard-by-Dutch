package nl.johninnl.wuplaycar;

/** Match packages resolved from the Binder caller UID, regardless of signing key. */
public final class HostPackages {
    public static boolean allowed(String[] packages) {
        if(packages!=null)for(String name:packages)
            if("app.seb3thehacker.gearslip".equals(name)||"app.seb3thehacker.gearslip.dev".equals(name))return true;
        return false;
    }
    private HostPackages(){}
}
