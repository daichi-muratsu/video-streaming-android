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
                // Composeを使うモジュールで、追加の設定なしにCompose UI Testを書けるようにする。
                // モジュールごとに書くと、追加を忘れたときに端末のテストが起動時にクラッシュするため
                add("androidTestImplementation", libs.library("androidx-compose-ui-test-junit4"))
                add("androidTestImplementation", libs.library("androidx-test-runner"))
                // `@RunWith(AndroidJUnit4::class)`に使う。`ui-test-junit4`からも推移的に入るが、
                // Version Catalogより古いバージョンになるため、明示して追加する
                add("androidTestImplementation", libs.library("androidx-junit"))
                // Compose UI Testは、UIがアイドル状態になるのを待つのにEspressoを使う。`ui-test-junit4`から
                // 推移的に入る3.5.0は、Android 17の実機で`InputManager.getInstance`が見つからず失敗するため、明示して追加する
                add("androidTestImplementation", libs.library("androidx-espresso-core"))
                // `createComposeRule()`が起動する空の`ComponentActivity`を、デバッグのビルドのマニフェストに追加する
                add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
                // Composeのベストプラクティスをチェックする、Slack製のLintルール
                add("lintChecks", libs.library("compose-lint-checks"))
            }
        }
    }
}
