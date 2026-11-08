package io.github.daichimuratsu.videostreaming.core.data.network.model

import io.github.daichimuratsu.videostreaming.core.domain.model.License
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

/** ライセンスのDTOを、Domain層の`License`に変換する。 */
internal fun LicenseDto.toDomain(): License = License(
    name = name,
    url = url,
    attribution = attribution,
    changes = changes,
)
