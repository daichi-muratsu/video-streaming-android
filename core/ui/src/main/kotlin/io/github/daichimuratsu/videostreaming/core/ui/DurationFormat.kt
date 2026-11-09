package io.github.daichimuratsu.videostreaming.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import kotlin.time.Duration

/**
 * 長さや再生位置を、`9:56`や`1:02:03`の形の文字列にする。
 *
 * 動画アプリで見慣れた形にするため、1時間未満は「分:秒」、1時間以上は「時:分:秒」にする。
 * 秒未満は切り捨てる。再生位置を表示するときに、まだ経過していない秒を表示しないため。
 * 一覧画面と再生画面で同じ形にするため、featureモジュールではなく`:core:ui`に置く
 */
fun formatDuration(duration: Duration): String =
    duration.toComponents { hours, minutes, seconds, _ ->
        // `String.format`はロケールで数字の書き方が変わるので使わず、ゼロ埋めは`padStart`で行う
        val paddedSeconds = seconds.toString().padStart(2, '0')
        if (hours > 0) {
            "$hours:${minutes.toString().padStart(2, '0')}:$paddedSeconds"
        } else {
            "$minutes:$paddedSeconds"
        }
    }

/**
 * 長さや再生位置を、読み上げ用の「9分56秒」の形の文字列にする。
 *
 * `formatDuration`の「9:56」は、時刻として読み上げられるため。
 * 文言を`strings.xml`の書式から`stringResource`で作るので、Composable関数にする
 */
@Composable
@ReadOnlyComposable
fun durationDescription(duration: Duration): String =
    duration.toComponents { hours, minutes, seconds, _ ->
        if (hours > 0) {
            stringResource(
                R.string.core_ui_duration_with_hours_description,
                hours,
                minutes,
                seconds,
            )
        } else {
            stringResource(R.string.core_ui_duration_description, minutes, seconds)
        }
    }
