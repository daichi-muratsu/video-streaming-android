import com.android.build.api.dsl.ApplicationExtension
import io.github.daichimuratsu.videostreaming.buildlogic.TARGET_SDK
import io.github.daichimuratsu.videostreaming.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `:app`用。Androidアプリとしてビルドする */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = TARGET_SDK
            }
        }
    }
}
