package io.github.daichimuratsu.videostreaming.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

internal const val COMPILE_SDK = 37
internal const val MIN_SDK = 26 // PiP（ピクチャー・イン・ピクチャー）がAPI 26以上で使えるため
internal const val TARGET_SDK = 37

/** JVMツールチェーンのバージョン。Android公式のJavaバージョン対応表で、最新の対応がJava 17のため */
private const val JAVA_TOOLCHAIN = 17

/** Androidアプリ・ライブラリ共通の設定。applicationとlibraryのConvention Pluginから呼ぶ */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk { version = release(COMPILE_SDK) }
        defaultConfig.minSdk = MIN_SDK
        defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    configureKotlinJvm()
}

/** JVMツールチェーンを設定する。sourceCompatibility・targetCompatibility・jvmTargetも、このバージョンに揃う */
internal fun Project.configureKotlinJvm() {
    extensions.configure<KotlinBaseExtension> {
        jvmToolchain(JAVA_TOOLCHAIN)
    }
}
