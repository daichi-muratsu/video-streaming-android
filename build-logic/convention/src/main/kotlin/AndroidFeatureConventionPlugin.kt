import io.github.daichimuratsu.videostreaming.buildlogic.library
import io.github.daichimuratsu.videostreaming.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

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
                // 画面をMaterial 3の部品で作るため。`:core:ui`は、テーマの関数の引数などに`material3`の型を出していないので、`api`で渡さずにここで追加する
                add("implementation", libs.library("androidx-compose-material3"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))

                // ViewModelのテストで、RepositoryのFakeを使い、`viewModelScope`のMainをテスト用のディスパッチャーに差し替え、`StateFlow`をTurbineで受け取るため
                add("testImplementation", testFixtures(project(":core:domain")))
                add("testImplementation", libs.library("kotlinx-coroutines-test"))
                add("testImplementation", libs.library("turbine"))
            }
        }
    }
}
