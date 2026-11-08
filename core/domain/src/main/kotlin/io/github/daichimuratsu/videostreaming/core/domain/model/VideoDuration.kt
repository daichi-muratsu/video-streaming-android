package io.github.daichimuratsu.videostreaming.core.domain.model

import kotlin.time.Duration

/**
 * 動画の長さを表す値オブジェクト。
 *
 * 長さがわからないとき（`Player`の`C.TIME_UNSET`）は、この型を作らない。変換する側で`null`にする
 */
@JvmInline
value class VideoDuration(val value: Duration) {
    init {
        // 0以下の長さは正しく作れば起きない。0だと再生位置の割合も計算できないので、作った時点で止める
        require(value.isPositive()) { "動画の長さが0以下になっている（値：$value）" }
    }
}
