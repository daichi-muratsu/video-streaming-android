package io.github.daichimuratsu.videostreaming.feature.player.fake

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.test.utils.StubPlayer

/**
 * テスト用の`Player`。設定された`MediaItem`と再生を始める位置・`prepare`・自動で再生するか・解放を記録し、再生位置と再生中かどうかをテストから変える。
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

    /** 最後に`MediaItem`を設定したときの、再生を始める位置（ミリ秒）。設定していなければ`null`。 */
    var startPositionMs: Long? = null
        private set

    // `setMediaItem(MediaItem, Long)`は、`BasePlayer`の中でこの関数を呼ぶ
    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ) {
        setMediaItemsList += mediaItems
        this.startPositionMs = startPositionMs
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

    /** `currentPosition`が返す再生位置（ミリ秒）。 */
    var currentPositionMs = 0L

    /** 登録されている`Player.Listener`。リスナーを外したかを、テストで確かめるために使う。 */
    val listeners = mutableListOf<Player.Listener>()

    // 解放したあとに読んだら失敗させる。ViewModelが、解放より先に再生位置を保存しているかを確かめるため
    override fun getCurrentPosition(): Long {
        check(releaseCount == 0) { "解放したあとに再生位置を読んだ" }
        return currentPositionMs
    }

    override fun addListener(listener: Player.Listener) {
        listeners += listener
    }

    override fun removeListener(listener: Player.Listener) {
        listeners -= listener
    }

    // `isPlaying`は`BasePlayer`の`final`の関数で、再生の状態・`playWhenReady`・再生を抑える理由から決まる。
    // そのため`isPlaying`ではなく、この3つを上書きする
    private var playbackStateValue = Player.STATE_IDLE

    override fun getPlaybackState(): Int = playbackStateValue

    override fun getPlayWhenReady(): Boolean = playWhenReadyValue

    override fun getPlaybackSuppressionReason(): Int = Player.PLAYBACK_SUPPRESSION_REASON_NONE

    /** 再生中かどうかを変え、本物の`Player`と同じく、登録されているリスナーに知らせる。 */
    fun changeIsPlaying(isPlaying: Boolean) {
        playbackStateValue = Player.STATE_READY
        playWhenReadyValue = isPlaying
        listeners.toList().forEach { it.onIsPlayingChanged(isPlaying) }
    }
}
