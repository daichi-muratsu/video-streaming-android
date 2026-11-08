package io.github.daichimuratsu.videostreaming.core.domain.usecase

import io.github.daichimuratsu.videostreaming.core.domain.model.VideoWithWatchStatus
import io.github.daichimuratsu.videostreaming.core.domain.model.WatchStatus
import io.github.daichimuratsu.videostreaming.core.domain.repository.PlaybackPositionRepository
import io.github.daichimuratsu.videostreaming.core.domain.repository.VideoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * 動画の一覧に、それぞれの視聴状態を組み合わせて取得するUseCase。
 *
 * 再生位置を保存するたびに、新しい一覧を出力する。
 * 動画の一覧の取得に失敗した場合は、例外をそのまま伝える（受け止めるのは呼び出し側のViewModel）
 */
class GetVideoListWithWatchStatusUseCase @Inject constructor(
    private val videoRepository: VideoRepository,
    private val positionRepository: PlaybackPositionRepository,
) {
    operator fun invoke(): Flow<List<VideoWithWatchStatus>> = flow {
        // 動画の情報はアプリの中で変わらないので、1回だけ取得する
        val videos = videoRepository.getVideos()
        emitAll(
            positionRepository.observePositions().map { positions ->
                videos.map { video ->
                    VideoWithWatchStatus(video, WatchStatus.of(positions[video.id], video.duration))
                }
            },
        )
    }
}
