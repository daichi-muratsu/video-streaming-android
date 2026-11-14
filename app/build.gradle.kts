plugins {
    alias(libs.plugins.videostreaming.android.application)
    alias(libs.plugins.videostreaming.android.compose)
    alias(libs.plugins.videostreaming.hilt)
    // Navigation 3の画面のキーを、プロセスが終了しても戻せるように保存するため
    alias(libs.plugins.kotlin.serialization)
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
    // featureモジュールと同じテーマを使うため
    implementation(project(":core:ui"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // 画面遷移。ViewModelを画面（`NavEntry`）ごとに作り、画面をバックスタックから外したときに破棄するため、
    // `lifecycle-viewmodel-navigation3`も追加する
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Coilの`ImageLoader`をアプリで1つ作り、Data層と同じ`OkHttpClient`で画像を取得するため
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)
}
