extension { name = "extensions/nexuslauncher.mpe" }
android {
    namespace = "app.morphe.extension.nexuslauncher"
    defaultConfig { minSdk = 28 }
}
dependencies {
    // Shizuku/Shevery binder proxy: lets search_ui calls run as shell (uid 2000), which is
    // the only principal that holds MANAGE_SEARCH_UI.
    api("dev.rikka.shizuku:api:13.1.5")
    api("dev.rikka.shizuku:provider:13.1.5")
    // reflection-based access to the members the launcher links against as a system app
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
}
