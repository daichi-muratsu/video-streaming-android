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

    // テストで、`Player`のFakeを`StubPlayer`から作るため
    testImplementation(libs.androidx.media3.test.utils)
}
