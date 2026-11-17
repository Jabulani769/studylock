plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}

kotlin {
    jvmToolchain(21)
}
