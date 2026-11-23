plugins {
    alias(libs.plugins.glassgrab.android.library)
    alias(libs.plugins.glassgrab.android.hilt)
}

android {
    namespace = "com.glassgrab.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
