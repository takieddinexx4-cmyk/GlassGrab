plugins {
    alias(libs.plugins.glassgrab.android.library)
    alias(libs.plugins.glassgrab.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.glassgrab.core.design"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.haze)
    implementation(libs.haze.blur)

    testImplementation(libs.junit4)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
