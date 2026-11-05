extension { name = "extensions/volkskrant.mpe" }
android {
 namespace = "nl.johninnl.volkskrant"
 defaultConfig { minSdk = 32 }
 testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies { testImplementation("junit:junit:4.13.2") }
configurations.matching { it.name == "releaseRuntimeClasspath" || it.name == "debugRuntimeClasspath" }
 .configureEach { exclude(group = "org.jetbrains.kotlin") }
