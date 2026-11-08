package io.github.daichimuratsu.videostreaming.feature.videolist

import io.github.daichimuratsu.videostreaming.core.domain.model.License
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoDuration
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoWithWatchStatus
import io.github.daichimuratsu.videostreaming.core.domain.model.WatchStatus
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

/**
 * プレビューで表示する一覧。未視聴・視聴途中・視聴済みを1本ずつ含む。
 *
 * `Video`は項目が多く、プレビューごとに書くと長くなるので、1か所にまとめ、各プレビューで使う。
 * 値は、実際の`videos.json`の3本に合わせる
 */
internal val previewVideos: List<VideoWithWatchStatus> = listOf(
    VideoWithWatchStatus(
        video = previewVideo(
            id = "big-buck-bunny",
            title = "Big Buck Bunny",
            seconds = 596,
            published = "2008-05-30",
        ),
        status = WatchStatus.NotStarted,
    ),
    VideoWithWatchStatus(
        video = previewVideo(
            id = "sintel",
            title = "Sintel",
            seconds = 888,
            published = "2010-09-30",
        ),
        status = WatchStatus.InProgress(0.42),
    ),
    VideoWithWatchStatus(
        video = previewVideo(
            id = "tears-of-steel",
            title = "Tears of Steel",
            seconds = 734,
            published = "2012-09-26",
        ),
        status = WatchStatus.Watched,
    ),
)

// 一覧に表示しない項目（説明・タグ・ライセンスなど）は、固定の値にする
private fun previewVideo(id: String, title: String, seconds: Int, published: String) = Video(
    id = VideoId(id),
    title = title,
    description = "",
    creator = "Blender Foundation",
    duration = VideoDuration(seconds.seconds),
    publishedAt = LocalDate.parse(published),
    thumbnailUrl = "https://example.com/$id/thumbnail.jpg",
    streamUrl = "https://example.com/$id/master.m3u8",
    tags = emptyList(),
    license = License(
        name = "CC BY 3.0",
        url = "https://creativecommons.org/licenses/by/3.0/",
        attribution = "Blender Foundation",
        changes = "",
    ),
)
