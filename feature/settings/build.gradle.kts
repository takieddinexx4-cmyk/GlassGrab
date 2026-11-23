plugins {
    alias(libs.plugins.glassgrab.android.feature)
}

android {
    namespace = "com.glassgrab.feature.settings"
}

dependencies {
    implementation(libs.androidx.appcompat)
}
