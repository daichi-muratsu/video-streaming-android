package io.github.daichimuratsu.videostreaming.core.domain.fake

import io.github.daichimuratsu.videostreaming.core.domain.model.License
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoDuration
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import java.time.LocalDate
import kotlin.time.Duration

/** テスト用の`Video`を作る。テストで使うIDと長さ以外は、固定の値にする。 */
fun testVideo(id: String, duration: Duration): Video = Video(
    id = VideoId(id),
    title = "タイトル",
    description = "説明",
    creator = "作者",
    duration = VideoDuration(duration),
    publishedAt = LocalDate.of(2008, 5, 30),
    thumbnailUrl = "https://example.com/$id/thumbnail.jpg",
    streamUrl = "https://example.com/$id/master.m3u8",
    tags = listOf("animation"),
    license = License(
        name = "CC BY 3.0",
        url = "https://creativecommons.org/licenses/by/3.0/",
        attribution = "作者",
        changes = "変更なし",
    ),
)
