package io.github.daichimuratsu.videostreaming.core.domain.fake

import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.repository.PlaybackPositionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * テスト用の`PlaybackPositionRepository`。
 *
 * `:core:data`の実装（Room）と同じく、保存するたびに`observePositions`へ新しい値を出力するため、`MutableStateFlow`で持つ
 */
class FakePlaybackPositionRepository(
    initial: Map<VideoId, PlaybackPosition> = emptyMap(),
) : PlaybackPositionRepository {
    private val positions = MutableStateFlow(initial)

    override fun observePositions(): Flow<Map<VideoId, PlaybackPosition>> = positions

    override suspend fun getPosition(id: VideoId): PlaybackPosition? = positions.value[id]

    override suspend fun savePosition(id: VideoId, position: PlaybackPosition) {
        positions.update { it + (id to position) }
    }
}
