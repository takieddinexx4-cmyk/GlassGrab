plugins {
    alias(libs.plugins.glassgrab.android.library)
    alias(libs.plugins.glassgrab.android.hilt)
    alias(libs.plugins.glassgrab.android.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.glassgrab.core.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:engine"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
}
