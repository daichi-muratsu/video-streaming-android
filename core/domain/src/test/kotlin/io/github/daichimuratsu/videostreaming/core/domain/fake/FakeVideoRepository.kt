package io.github.daichimuratsu.videostreaming.core.domain.fake

import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.repository.VideoRepository

/**
 * テスト用の`VideoRepository`。
 *
 * `error`を指定すると、取得の失敗を再現するため、その例外を投げる
 */
class FakeVideoRepository(
    private val videos: List<Video>,
    private val error: Throwable? = null,
) : VideoRepository {
    override suspend fun getVideos(): List<Video> {
        error?.let { throw it }
        return videos
    }

    override suspend fun getVideo(id: VideoId): Video? {
        error?.let { throw it }
        return videos.find { it.id == id }
    }
}
