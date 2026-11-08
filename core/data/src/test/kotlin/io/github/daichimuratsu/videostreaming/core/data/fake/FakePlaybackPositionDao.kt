package io.github.daichimuratsu.videostreaming.core.data.fake

import io.github.daichimuratsu.videostreaming.core.data.database.PlaybackPositionDao
import io.github.daichimuratsu.videostreaming.core.data.database.model.PlaybackPositionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * テスト用の`PlaybackPositionDao`。
 *
 * Roomと同じく、保存するたびに`observeAll`へ新しい一覧を出力するため、行を`MutableStateFlow`で持つ。
 * `MutableStateFlow`は同じ値を続けて出力しないので、同じ値を保存し直したときの出力はRoomと異なる。
 * Roomの動きそのものは、端末で動かすDAOのテストで確かめる
 */
internal class FakePlaybackPositionDao : PlaybackPositionDao {
    // 主キー（動画のID）ごとに1行だけ持つ
    private val rows = MutableStateFlow<Map<String, PlaybackPositionEntity>>(emptyMap())

    override suspend fun upsert(entity: PlaybackPositionEntity) {
        rows.update { it + (entity.videoId to entity) }
    }

    override suspend fun getByVideoId(videoId: String): PlaybackPositionEntity? =
        rows.value[videoId]

    override fun observeAll(): Flow<List<PlaybackPositionEntity>> = rows.map { it.values.toList() }
}
