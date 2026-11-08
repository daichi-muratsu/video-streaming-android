package io.github.daichimuratsu.videostreaming.core.data.network.model

import kotlinx.serialization.Serializable

/**
 * `videos.json`の全体を表すDTO。
 *
 * JSONの最上位が配列ではなく`videos`を持つオブジェクトなので、その形のまま受け取る
 */
@Serializable
internal data class VideoListDto(
    val videos: List<VideoDto>,
)
