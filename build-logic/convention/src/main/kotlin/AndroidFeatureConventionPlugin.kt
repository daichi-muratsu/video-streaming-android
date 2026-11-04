import io.github.daichimuratsu.videostreaming.buildlogic.libs
import io.github.daichimuratsu.videostreaming.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * featureモジュール用。library・compose・hiltのConvention Pluginを適用し、featureモジュール共通の依存関係を追加する。
 * featureモジュール同士の依存関係はここに追加しない（画面遷移は`:app`で組み立てる）
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("videostreaming.android.library")
            pluginManager.apply("videostreaming.android.compose")
            pluginManager.apply("videostreaming.hilt")

            dependencies {
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:ui"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            }
        }
    }
}
