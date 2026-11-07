package io.github.daichimuratsu.videostreaming.buildlogic

import com.android.build.api.dsl.CommonExtension

/**
 * Android Lintの設定。
 * 警告が増えても気づけるよう、警告をエラーとして扱い、CIで失敗させる
 */
internal fun CommonExtension.configureAndroidLint() {
    lint.apply {
        warningsAsErrors = true
        // 次の警告は、CIを止めずにレポートに情報として残す
        informational +=
            listOf(
                // ネットワーク上の最新のバージョンと比べるため、コードを変えなくても結果が変わる。
                // Google Mavenのライブラリは`GradleDependency`、Maven Centralのライブラリは`NewerVersionAvailable`が見る
                "AndroidGradlePluginVersion",
                "GradleDependency",
                "NewerVersionAvailable",
                // lifecycle 2.11.0に入っているLintのチェックが、今のLintにないAPIを参照している
                "ObsoleteLintCustomCheck",
            )
    }
}
