package io.github.daichimuratsu.videostreaming.core.domain.model

import kotlin.time.Duration.Companion.seconds

/**
 * 動画の視聴状態（未視聴・視聴途中・視聴済み）を表す型。
 *
 * 一覧画面の表示と、再生を始める位置の両方で使う。両者が食い違わないように、判定は`of`の1か所にまとめる
 */
sealed interface WatchStatus {
    /** 未視聴。再生位置が保存されていないか、10秒未満で再生をやめた */
    data object NotStarted : WatchStatus

    /** 視聴途中。10秒以上、90%未満まで再生した */
    data class InProgress(
        /** 再生した割合。0.0以上、0.9未満 */
        val progress: Double,
    ) : WatchStatus

    /** 視聴済み。90%以上まで再生した。次は最初から再生する */
    data object Watched : WatchStatus

    companion object {
        // 3本の動画のエンドクレジットは、80〜84%の位置から始まる。
        // 95%だとクレジットの途中で再生をやめたときに続きから再生され、80%だと物語の最後を再生していなくても視聴済みになる
        private const val WATCHED_RATIO = 0.9

        // 誤って開いてすぐ閉じた動画を、視聴途中にしないため
        private val NOT_STARTED_LIMIT = 10.seconds

        /**
         * 保存した再生位置と動画の長さから、状態を決める。
         *
         * 長さが10秒÷0.9（約11.1秒）より短い動画では、90%以上と10秒未満が同時に成り立つ。
         * 短い動画を最後まで再生したなら、視聴したとみなせるので、90%以上を先に確かめる
         */
        fun of(position: PlaybackPosition?, duration: VideoDuration): WatchStatus {
            if (position == null) return NotStarted
            // 再生位置が長さを超える（割合が1.0より大きい）こともあるが、90%以上なので視聴済みになる
            val ratio = position.value / duration.value
            return when {
                ratio >= WATCHED_RATIO -> Watched
                position.value < NOT_STARTED_LIMIT -> NotStarted
                else -> InProgress(ratio)
            }
        }
    }
}
