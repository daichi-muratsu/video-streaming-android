package io.github.daichimuratsu.videostreaming.core.data.fake

import io.github.daichimuratsu.videostreaming.core.data.network.model.LicenseDto
import io.github.daichimuratsu.videostreaming.core.data.network.model.VideoDto

/**
 * テスト用の`VideoDto`を作る。
 *
 * テストごとに確かめたい項目だけを指定し、ほかの項目は正しい値で埋めるため
 */
internal fun testVideoDto(
    id: String = "big-buck-bunny",
    durationSeconds: Int = 596,
    publishedAt: String = "2008-05-30",
): VideoDto = VideoDto(
    id = id,
    title = "タイトル（$id）",
    description = "説明（$id）",
    creator = "作者（$id）",
    durationSeconds = durationSeconds,
    publishedAt = publishedAt,
    thumbnailUrl = "https://example.com/$id/thumbnail.jpg",
    streamUrl = "https://example.com/$id/master.m3u8",
    tags = listOf("animation"),
    license = LicenseDto(
        name = "CC BY 3.0",
        url = "https://creativecommons.org/licenses/by/3.0/",
        attribution = "表記（$id）",
        changes = "改変（$id）",
    ),
)
