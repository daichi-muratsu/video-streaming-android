plugins {
    `kotlin-dsl`
}

group = "io.github.daichimuratsu.videostreaming.buildlogic"

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Convention PluginからAGPとKGPのAPIを参照するため。実行時はルートで読み込んだ本体を使うので、compileOnlyにする
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.videostreaming.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.videostreaming.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.videostreaming.jvm.library.get().pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.videostreaming.android.compose.get().pluginId
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = libs.plugins.videostreaming.hilt.get().pluginId
            implementationClass = "HiltConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.videostreaming.android.feature.get().pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }
    }
}
