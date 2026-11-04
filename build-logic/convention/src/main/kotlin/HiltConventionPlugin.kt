import io.github.daichimuratsu.videostreaming.buildlogic.libs
import io.github.daichimuratsu.videostreaming.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Hiltを使うAndroidモジュール用（`:app`・`:core:data`・`:feature:*`）。コード生成にはKSPを使う */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")

            dependencies {
                add("implementation", libs.library("hilt-android"))
                add("ksp", libs.library("hilt-compiler"))
            }
        }
    }
}
