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
)
