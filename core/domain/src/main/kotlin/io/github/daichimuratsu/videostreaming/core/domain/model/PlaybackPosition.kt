package io.github.daichimuratsu.videostreaming.core.domain.model

import kotlin.time.Duration

/**
 * 再生位置（動画の最初からの経過時間）を表す値オブジェクト。
 *
 * 秒（JSON）とミリ秒（`Player`）の取り違えを防ぐため、中身は`Duration`にする
 */
@JvmInline
value class PlaybackPosition(val value: Duration) {
    init {
        // 負の位置は正しく作れば起きないので、バグとして作った時点で止める。
        // 動画の長さを超える値は止めない。JSONの長さは整数の秒なので、実際の動画が少し長いと超えることがある
        require(!value.isNegative()) { "再生位置が負になっている（値：$value）" }
    }
}
