import io.github.daichimuratsu.videostreaming.buildlogic.library
import io.github.daichimuratsu.videostreaming.buildlogic.libs
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

                // `hilt-android`は、古い`androidx.core`などを経由して`annotation-experimental`の1.3.1を持ち込む。
                // 1.3.1のLintのチェックには、Kotlinのファイルにも`RequiresOptIn`のチェックを行う不具合がある（1.4.1で修正）。
                // この不具合で、Android Studioが`@Serializable`のクラスに`InternalSerializationApi`のエラーを表示するため、バージョンだけを上げる
                constraints {
                    add("implementation", libs.library("androidx-annotation-experimental"))
                }
            }
        }
    }
}
