plugins {
    alias(libs.plugins.videostreaming.android.feature)
}

android {
    namespace = "io.github.daichimuratsu.videostreaming.feature.videolist"
}

dependencies {
    // サムネイルを表示するため。画像を使うのは一覧画面だけなので、Convention Pluginではなくここで追加する
    implementation(libs.coil.compose)
}
