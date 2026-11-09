package io.github.daichimuratsu.videostreaming.feature.player.fake

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.test.utils.StubPlayer

/**
 * テスト用の`Player`。設定された`MediaItem`・`prepare`・自動で再生するか・解放を記録する。
 *
 * ExoPlayerはAndroidの環境がないと作れないため、すべての関数が例外を投げる`StubPlayer`を元にし、ViewModelが使う関数だけを上書きする。
 * プロパティの名前は、`Player`の関数と重ならないようにする。
 * `var mediaItem`にすると、Kotlinが`setMediaItem`という関数を作り、`Player.setMediaItem`と衝突するため
 */
@OptIn(UnstableApi::class)
class FakePlayer : StubPlayer() {
    val setMediaItemsList = mutableListOf<MediaItem>()
    var prepareCount = 0
        private set
    var playWhenReadyValue = false
        private set
    var releaseCount = 0
        private set

    // `setMediaItem(MediaItem)`は、`BasePlayer`の中でこの関数を呼ぶ
    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        setMediaItemsList += mediaItems
    }

    override fun prepare() {
        prepareCount++
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        playWhenReadyValue = playWhenReady
    }

    override fun release() {
        releaseCount++
    }
}
