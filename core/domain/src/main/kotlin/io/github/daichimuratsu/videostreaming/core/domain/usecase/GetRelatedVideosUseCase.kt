package io.github.daichimuratsu.videostreaming.core.domain.usecase

import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.repository.VideoRepository
import javax.inject.Inject

/**
 * 再生画面に表示する関連動画を取得するUseCase。
 *
 * 選び方は`Video.relatedVideosIn`に任せ、ここでは動画の一覧の取得と組み合わせる
 */
class GetRelatedVideosUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
) {
    /**
     * `video`の関連動画を返す。
     *
     * 一覧の取得に失敗した場合は、例外を投げる
     */
    suspend operator fun invoke(video: Video): List<Video> =
        video.relatedVideosIn(videoRepository.getVideos())
}
