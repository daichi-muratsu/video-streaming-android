package io.github.daichimuratsu.videostreaming.feature.videolist

import kotlin.time.Duration

/**
 * 長さや再生位置を、`9:56`や`1:02:03`の形の文字列にする。
 *
 * 動画アプリで見慣れた形にするため、1時間未満は「分:秒」、1時間以上は「時:分:秒」にする。
 * 秒未満は切り捨てる。再生位置を表示するときに、まだ経過していない秒を表示しないため
 */
internal fun formatDuration(duration: Duration): String =
    duration.toComponents { hours, minutes, seconds, _ ->
        // `String.format`はロケールで数字の書き方が変わるので使わず、ゼロ埋めは`padStart`で行う
        val paddedSeconds = seconds.toString().padStart(2, '0')
        if (hours > 0) {
            "$hours:${minutes.toString().padStart(2, '0')}:$paddedSeconds"
        } else {
            "$minutes:$paddedSeconds"
        }
    }
