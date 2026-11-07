import com.android.build.api.dsl.CommonExtension
import io.github.daichimuratsu.videostreaming.buildlogic.library
import io.github.daichimuratsu.videostreaming.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Composeを使うモジュール用（`:app`・`:core:ui`・`:feature:*`）。
 * `android { }`の設定を変更するため、`videostreaming.android.application`または
 * `videostreaming.android.library`より後に適用する
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.getByType<CommonExtension>().buildFeatures.compose = true

            dependencies {
                val bom = platform(libs.library("androidx-compose-bom"))
                add("implementation", bom)
                add("androidTestImplementation", bom)
                add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
                add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
                // Composeのベストプラクティスをチェックする、Slack製のLintルール
                add("lintChecks", libs.library("compose-lint-checks"))
            }
        }
    }
}
