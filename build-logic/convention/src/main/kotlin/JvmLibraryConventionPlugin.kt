import io.github.daichimuratsu.videostreaming.buildlogic.configureKotest
import io.github.daichimuratsu.videostreaming.buildlogic.configureKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `:core:domain`用。Android非依存のKotlin/JVMライブラリとしてビルドする。
 * Androidプラグインを適用しないため、Android APIを使うとコンパイルエラーになる
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlinJvm()
            configureKotest()
        }
    }
}
