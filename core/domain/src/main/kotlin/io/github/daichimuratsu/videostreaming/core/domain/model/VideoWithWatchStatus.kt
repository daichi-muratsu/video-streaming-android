package io.github.daichimuratsu.videostreaming.core.domain.model

/**
 * 動画と、その視聴状態の組み合わせを表す。
 *
 * 動画の情報と再生位置は取得元が異なるため、`Video`に視聴状態を持たせず、一覧画面に渡すときに組み合わせる
 */
data class VideoWithWatchStatus(
    val video: Video,
    val status: WatchStatus,
)
