package io.github.daichimuratsu.videostreaming.core.domain.model

import java.time.LocalDate

/**
 * 動画を表すエンティティ。`id`で見分ける。
 *
 * 再生位置は持たない。動画の情報（JSON）と再生位置（データベース）は取得元が異なるため、
 * 一覧画面では`VideoWithWatchStatus`で組み合わせる
 */
data class Video(
    val id: VideoId,
    val title: String,
    val description: String,
    val creator: String,
    val duration: VideoDuration,
    val publishedAt: LocalDate,
    val thumbnailUrl: String,
    val streamUrl: String,
    /** 関連動画（同じタグの動画）を探すのに使う */
    val tags: List<String>,
    val license: License,
) {
    /**
     * `videos`から、この動画の関連動画を選ぶ。
     *
     * タグが1つ以上共通する動画を、共通のタグが多い順に並べる。共通のタグが多いほど、内容が近いとみなすため。
     * この動画自身は含めない。タグは完全に一致するときだけ共通とみなし、1本の中で重なったタグは1つと数える
     */
    fun relatedVideosIn(videos: List<Video>): List<Video> {
        val ownTags = tags.toSet()
        return videos
            .filter { it.id != id }
            .map { video -> video to (video.tags.toSet() intersect ownTags).size }
            .filter { (_, sharedCount) -> sharedCount > 0 }
            // `sortedByDescending`は安定なソートなので、共通のタグの数が同じ動画は、もとの順のまま並ぶ
            .sortedByDescending { (_, sharedCount) -> sharedCount }
            .map { (video, _) -> video }
    }
}
