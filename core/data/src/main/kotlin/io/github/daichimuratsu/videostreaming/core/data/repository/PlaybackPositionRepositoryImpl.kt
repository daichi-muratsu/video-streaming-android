package io.github.daichimuratsu.videostreaming.core.data.repository

import io.github.daichimuratsu.videostreaming.core.data.database.PlaybackPositionDao
import io.github.daichimuratsu.videostreaming.core.data.database.model.toDomain
import io.github.daichimuratsu.videostreaming.core.data.database.model.toEntity
import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.repository.PlaybackPositionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 端末のデータベース（Room）に再生位置を保存する`PlaybackPositionRepository`の実装。 */
internal class PlaybackPositionRepositoryImpl @Inject constructor(
    private val dao: PlaybackPositionDao,
) : PlaybackPositionRepository {
    // Roomはテーブルが変わるたびに一覧を出力し直すので、保存するたびに新しい`Map`になる
    override fun observePositions(): Flow<Map<VideoId, PlaybackPosition>> =
        dao.observeAll().map { entities ->
            entities.associate { VideoId(it.videoId) to it.toDomain() }
        }

    override suspend fun getPosition(id: VideoId): PlaybackPosition? =
        dao.getByVideoId(id.value)?.toDomain()

    override suspend fun savePosition(id: VideoId, position: PlaybackPosition) {
        dao.upsert(position.toEntity(id))
    }
}
