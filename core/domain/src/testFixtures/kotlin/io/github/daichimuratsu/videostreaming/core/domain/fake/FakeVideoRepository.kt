package io.github.daichimuratsu.videostreaming.core.domain.fake

import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.repository.VideoRepository

/**
 * テスト用の`VideoRepository`。
 *
 * `error`に例外を入れると、取得の失敗を再現するために、その例外を投げる。
 * テストの途中で`error`を`null`に戻せるようにし、失敗したあとの再読み込みで取得できることを確かめる
 */
class FakeVideoRepository(
    private val videos: List<Video>,
    var error: Throwable? = null,
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
