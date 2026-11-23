plugins {
    alias(libs.plugins.glassgrab.android.feature)
}

android {
    namespace = "com.glassgrab.feature.player"
}

dependencies {
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
}
