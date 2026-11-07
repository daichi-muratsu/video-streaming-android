import com.diffplug.gradle.spotless.SpotlessTask

// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Convention Pluginが適用するプラグインを、ここで読み込む（適用は各モジュールで行う）
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // `:core:domain`用。KGP本体がVersion Catalogのバージョンで読み込まれるため、
    // AGP 9の組み込みKotlin（KGP 2.2.10）もこのバージョンに上がる
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

// すべてのモジュールとbuild-logicのファイルを、ルートでまとめてktlintでチェックする。
// ktlintのルールは`.editorconfig`で設定する。
// 対象はモジュールのフォルダから書く。`**`で全体をたどると、ビルド中の`build/`まで読みに行って失敗するため
spotless {
    val moduleDirs = listOf("app", "core/*", "feature/*", "build-logic/convention")
    kotlin {
        target(moduleDirs.map { "$it/src/**/*.kt" })
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target(listOf("*.kts", "build-logic/*.kts") + moduleDirs.map { "$it/*.kts" })
        ktlint(libs.versions.ktlint.get())
    }
}

// Spotlessの不具合の回避策（https://github.com/diffplug/spotless/issues/3067）。
// cleanタスクが、Spotlessが使っているクラスローダーを閉じてしまうため、Spotlessのタスクはcleanの後に実行する。
// Spotlessをルートだけで適用しているので、対象のcleanタスクはルートの1つだけになる
tasks.withType<SpotlessTask>().configureEach {
    mustRunAfter(tasks.named("clean"))
}
