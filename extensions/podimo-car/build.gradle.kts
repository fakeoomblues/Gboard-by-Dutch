extension {
    name = "extensions/podimo-car.mpe"
}

android {
    namespace = "nl.johninnl.podimo.car"
    buildFeatures { buildConfig = false }
}

// The extension is plain Java. Without this the Kotlin Gradle plugin's default
// stdlib dependency gets dexed into the extension, adding ~935 kotlin.* classes
// to every patched Podimo build.
configurations.configureEach {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains", module = "annotations")
}
