package io.github.daichimuratsu.videostreaming.core.domain.model

/**
 * 動画のIDを表す値オブジェクト。
 *
 * タイトルなど、ほかの文字列と取り違えないように型を分ける
 */
@JvmInline
value class VideoId(val value: String) {
    init {
        // 空のIDでは動画を見分けられない。正しく作れば起きないので、バグとして作った時点で止める
        require(value.isNotBlank()) { "動画のIDが空になっている（値：\"$value\"）" }
    }
}
