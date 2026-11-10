plugins {
    alias(libs.plugins.videostreaming.android.feature)
}

android {
    namespace = "io.github.daichimuratsu.videostreaming.feature.player"
}

dependencies {
    // 動画を再生するため。HLSを再生するには、`media3-exoplayer-hls`も要る
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    // 映像をComposeで表示し（`PlayerSurface`）、操作ボタンの状態を`Player`から受け取るため
    implementation(libs.androidx.media3.ui.compose)
    // 全画面で、`Activity`の向きを固定し（`LocalActivity`）、「戻る」で全画面を終える（`BackHandler`）ため
    implementation(libs.androidx.activity.compose)
    // 全画面で、システムバーを隠す（`WindowInsetsControllerCompat`）ため
    implementation(libs.androidx.core.ktx)

    // テストで、`Player`のFakeを`StubPlayer`から作るため
    testImplementation(libs.androidx.media3.test.utils)
}
