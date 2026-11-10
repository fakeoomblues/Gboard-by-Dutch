package patches.maps.gearslip;

/**
 * Any Android Auto compatible host may drive the car screens (Google's Android Auto, DriveDeck,
 * Gearslip, MyCar, Flywheel, BetterAuto, AndroidAuto, AARE, ...). A host claim is only accepted when
 * the Binder caller actually owns that package, so one app cannot pose as another.
 */
final class HostPolicy {
    // DriveDeck/Gearslip render a turn whose distance Maps has not produced yet; other hosts may
    // rebuild the model with the car library's builder, which requires a distance.
    private static final String[] UNKNOWN_DISTANCE_HOSTS = {
        "app.seb3thehacker.gearslip", "app.seb3thehacker.gearslip.dev", "app.johninnl.drivedeck", "app.johninnl.drivedeck.dev",
        "app.johninnl.drivedeck.litert", "app.johninnl.drivedeck.wireless"
    };
    private HostPolicy() {}

    static boolean permits(String packageName, int uid, String[] ownedPackages) {
        return uid >= 0 && packageName != null && contains(ownedPackages, packageName);
    }

    static boolean rendersUnknownTurnDistance(String packageName) {
        return contains(UNKNOWN_DISTANCE_HOSTS, packageName);
    }

    private static boolean contains(String[] values, String candidate) {
        if (values != null && candidate != null) for (String value : values) if (candidate.equals(value)) return true;
        return false;
    }
}
