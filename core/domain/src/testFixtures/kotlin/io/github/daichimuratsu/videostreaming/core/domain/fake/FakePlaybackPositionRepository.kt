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

    /** `observePositions`を受け取っている数。画面が見えないあいだに受け取りが止まることを、テストで確かめるために使う。 */
    val subscriptionCount: Int
        get() = positions.subscriptionCount.value

    override fun observePositions(): Flow<Map<VideoId, PlaybackPosition>> = positions

    /** `getPosition`で投げる例外。取得の失敗を再現するときに入れる。 */
    var getPositionError: Throwable? = null

    override suspend fun getPosition(id: VideoId): PlaybackPosition? {
        getPositionError?.let { throw it }
        return positions.value[id]
    }

    override suspend fun savePosition(id: VideoId, position: PlaybackPosition) {
        positions.update { it + (id to position) }
    }
}
