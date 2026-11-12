package io.github.daichimuratsu.videostreaming.feature.player

import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.repository.PlaybackPositionRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch

/**
 * `PlaybackPositionTracker`が読み取る、再生位置と再生中かどうかを表すinterface。
 *
 * `Player`のうち、保存に使う再生位置と再生中かどうかだけに絞る。
 * Trackerを`Player`とミリ秒の値に依存させず、Fakeに替えてテストするため
 */
internal interface PlaybackProgress {
    /** 現在の再生位置。 */
    val currentPosition: Duration

    /** 再生中かどうか。受け取り始めたときに現在の値を出力し、そのあとは変わるたびに出力する。 */
    val isPlaying: Flow<Boolean>
}

/**
 * 再生位置を、再生中の一定の間隔・再生が止まったとき・画面を離れるときに保存するクラス。
 *
 * 一時停止・戻るなどの操作では止めたときに保存するので、位置がずれない。
 * 一定の間隔の保存は、アプリがクラッシュしたとき・OSがプロセスを終了したときに、失う再生位置の長さを抑えるため
 */
internal class PlaybackPositionTracker(
    private val videoId: VideoId,
    private val progress: PlaybackProgress,
    private val repository: PlaybackPositionRepository,
    // 画面より長く生きるスコープ。画面を離れたあとも、書き込みを最後まで終えるため
    private val saveScope: CoroutineScope,
) {
    /**
     * 再生中かどうかを受け取り続け、保存する。
     *
     * 呼び出したスコープが止まるまで戻らない。画面のスコープで呼び、画面を離れたら止める
     */
    suspend fun track() {
        progress.isPlaying
            // 再生を始める前の「再生中ではない」では保存しない。保存すると、前に保存した位置を0で上書きするため
            .dropWhile { isPlaying -> !isPlaying }
            // 止まったら、一定の間隔の保存を止める。再開したら、間隔を最初から数え直す
            .collectLatest { isPlaying ->
                if (isPlaying) {
                    while (true) {
                        delay(SAVE_INTERVAL)
                        save()
                    }
                } else {
                    save()
                }
            }
    }

    /**
     * 現在の再生位置を保存する。
     *
     * 位置は呼び出したスレッドで読み、書き込みだけを`saveScope`で行う。`Player`はメインスレッドから読む必要があるため
     */
    fun save() {
        val position = PlaybackPosition(progress.currentPosition)
        saveScope.launch { repository.savePosition(videoId, position) }
    }

    private companion object {
        // クラッシュしたときに失う再生位置の長さ（最大でこの長さ）と、書き込みの回数の釣り合いで決めた
        val SAVE_INTERVAL = 10.seconds
    }
}
