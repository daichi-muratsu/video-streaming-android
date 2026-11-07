plugins {
    alias(libs.plugins.videostreaming.android.application)
    alias(libs.plugins.videostreaming.android.compose)
    alias(libs.plugins.videostreaming.hilt)
}

android {
    namespace = "io.github.daichimuratsu.videostreaming"

    defaultConfig {
        applicationId = "io.github.daichimuratsu.videostreaming"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
}

dependencies {
    // 画面遷移を`:app`で組み立てるため、featureモジュールに依存する
    implementation(project(":feature:videolist"))
    implementation(project(":feature:player"))
    // HiltでRepositoryのinterfaceと実装を結び付けるため、Data層にも依存する
    implementation(project(":core:data"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
