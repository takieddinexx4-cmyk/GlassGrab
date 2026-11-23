import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("glassgrab.android.library")
        pluginManager.apply("glassgrab.android.compose")
        pluginManager.apply("glassgrab.android.hilt")
        dependencies {
            add("implementation", project(":core:common"))
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:design"))
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
            add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
            add("implementation", libs.findLibrary("coil-compose").get())
        }
    }
}
