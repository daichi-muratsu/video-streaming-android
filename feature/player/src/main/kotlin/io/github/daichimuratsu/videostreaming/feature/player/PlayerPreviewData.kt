package io.github.daichimuratsu.videostreaming.feature.player

import io.github.daichimuratsu.videostreaming.core.domain.model.License
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoDuration
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

/**
 * 再生画面のプレビューで使う動画。
 *
 * 文字の長さで見た目が変わるので、実際のデータ（`data/videos.json`のBig Buck Bunny）と同じ文言にする
 */
internal val previewVideo = Video(
    id = VideoId("big-buck-bunny"),
    title = "Big Buck Bunny",
    description = "森に住む大きなウサギと、いたずら好きな3匹の動物たちの物語。",
    creator = "Blender Foundation",
    duration = VideoDuration(596.seconds),
    publishedAt = LocalDate.of(2008, 5, 30),
    thumbnailUrl = "",
    streamUrl = "",
    tags = listOf("animation", "comedy"),
    license = License(
        name = "CC BY 3.0",
        url = "https://creativecommons.org/licenses/by/3.0/",
        attribution = "(c) copyright 2008, Blender Foundation / www.bigbuckbunny.org",
        changes = "HLS形式に変換し、3つの画質（360p / 720p / 1080p）に再エンコード。" +
            "サムネイルは本編の1場面を16:9に切り抜いたもの",
    ),
)
