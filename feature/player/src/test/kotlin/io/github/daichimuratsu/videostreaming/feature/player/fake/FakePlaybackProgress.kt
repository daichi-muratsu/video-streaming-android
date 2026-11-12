package io.github.daichimuratsu.videostreaming.feature.player.fake

import io.github.daichimuratsu.videostreaming.feature.player.PlaybackProgress
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * テスト用の`PlaybackProgress`。再生位置は`clock`の値を返し、再生中かどうかはテストから変える。
 *
 * 再生位置をテストの仮想の時間と同じにすると、保存された位置から、いつ保存されたかがわかるため
 */
internal class FakePlaybackProgress(private val clock: () -> Duration) : PlaybackProgress {
    override val currentPosition: Duration
        get() = clock()

    override val isPlaying = MutableStateFlow(false)
}
