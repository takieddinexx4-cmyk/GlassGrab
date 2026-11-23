plugins {
    alias(libs.plugins.glassgrab.android.library)
    alias(libs.plugins.glassgrab.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.glassgrab.core.domain"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
