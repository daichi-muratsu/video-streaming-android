plugins {
    alias(libs.plugins.videostreaming.android.library)
    alias(libs.plugins.videostreaming.android.compose)
}

android {
    namespace = "io.github.daichimuratsu.videostreaming.core.ui"
}

dependencies {
    // `VideoCard`の引数に`Video`・`WatchStatus`が出てくるので、使う側にも`:core:domain`の型が見えるように`api`にする
    api(project(":core:domain"))
    implementation(libs.androidx.compose.material3)
    // `VideoCard`のサムネイルを表示するため
    implementation(libs.coil.compose)
}
