import com.android.build.api.dsl.LibraryExtension
import io.github.daichimuratsu.videostreaming.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `:core:data`・`:core:ui`・`:feature:*`用。Androidライブラリとしてビルドする */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
            }
        }
    }
}
