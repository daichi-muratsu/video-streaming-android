package io.github.daichimuratsu.videostreaming.core.domain.usecase

import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.WatchStatus
import io.github.daichimuratsu.videostreaming.core.domain.repository.PlaybackPositionRepository
import javax.inject.Inject
import kotlin.time.Duration

/**
 * 動画の再生を始める位置を決めるUseCase。
 *
 * 一覧画面の表示と食い違わないように、`WatchStatus.of`の判定から決める
 */
class GetResumePositionUseCase @Inject constructor(
    private val positionRepository: PlaybackPositionRepository,
) {
    /** 視聴途中なら保存した再生位置を、未視聴・視聴済みなら最初の位置を返す。 */
    suspend operator fun invoke(video: Video): PlaybackPosition {
        val position = positionRepository.getPosition(video.id)
        return when (WatchStatus.of(position, video.duration)) {
            // 視聴途中と判定されるのは、再生位置が保存されている場合だけ
            is WatchStatus.InProgress -> checkNotNull(position)

            WatchStatus.NotStarted, WatchStatus.Watched -> PlaybackPosition(Duration.ZERO)
        }
    }
}
