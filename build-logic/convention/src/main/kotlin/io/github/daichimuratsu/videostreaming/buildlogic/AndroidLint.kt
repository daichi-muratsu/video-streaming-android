package io.github.daichimuratsu.videostreaming.buildlogic

import com.android.build.api.dsl.CommonExtension

/**
 * Android Lintの設定。
 * 警告が増えても気づけるよう、警告をエラーとして扱い、ビルドを失敗させる（手元でもCIでも）
 */
internal fun CommonExtension.configureAndroidLint() {
    lint.apply {
        warningsAsErrors = true
        // 次の警告は、CIを止めずにレポートに情報として残す。
        // 警告はまず直す。ここに追加するのは、コードを変えなくても結果が変わる・自分では直せない、のような理由があるときだけ
        informational +=
            listOf(
                // ネットワーク上の最新のバージョンと比べるため、コードを変えなくても結果が変わる。
                // Google Mavenのライブラリは`GradleDependency`、Maven Centralのライブラリは`NewerVersionAvailable`が見る
                "AndroidGradlePluginVersion",
                "GradleDependency",
                "NewerVersionAvailable",
                // lifecycle 2.11.0に入っているLintのチェックが、今のLintにないAPIを参照しているため。
                // ほかのライブラリのチェックがスキップされてもHintになるので、AGPを上げたときはレポートを確かめる。
                // lifecycleが直ったら外す
                "ObsoleteLintCustomCheck",
            )
    }
}
