package io.github.daichimuratsu.videostreaming.core.data.network.model

import kotlinx.serialization.Serializable

/**
 * `videos.json`の動画のライセンスを表すDTO。
 *
 * 項目に既定値を付けない理由は`VideoDto`と同じ
 */
@Serializable
internal data class LicenseDto(
    val name: String,
    val url: String,
    val attribution: String,
    val changes: String,
)
