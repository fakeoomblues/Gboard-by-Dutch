import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.6.1"
}

dependencies {
    implementation("org.jsoup:jsoup:1.23.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("isolated")
    relocate("org.jsoup", "nl.johninnl.medium.vendor.jsoup")
    relocate("okhttp3", "nl.johninnl.medium.vendor.okhttp3")
    relocate("okio", "nl.johninnl.medium.vendor.okio")
    relocate("kotlin", "nl.johninnl.medium.vendor.kotlin")
    relocate("org.jetbrains.annotations", "nl.johninnl.medium.vendor.annotations")
    relocate("org.intellij.lang.annotations", "nl.johninnl.medium.vendor.intellij")
    exclude("META-INF/versions/**", "META-INF/*.kotlin_module", "module-info.class")
    mergeServiceFiles()
}

configurations.create("isolated") {
    isCanBeConsumed = true
    isCanBeResolved = false
}
artifacts { add("isolated", tasks.named("shadowJar")) }
