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
}
