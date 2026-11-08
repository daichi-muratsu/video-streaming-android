package io.github.daichimuratsu.videostreaming.core.data.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import io.github.daichimuratsu.videostreaming.core.data.database.model.PlaybackPositionEntity
import kotlinx.coroutines.flow.Flow

/** 再生位置のテーブルを操作するDAO。 */
@Dao
internal interface PlaybackPositionDao {
    /**
     * 再生位置を保存する。同じ動画の行があれば、その行を更新する。
     *
     * `@Insert(onConflict = REPLACE)`は既存の行を削除してから挿入するので、行を削除しない`@Upsert`を使う
     */
    @Upsert
    suspend fun upsert(entity: PlaybackPositionEntity)

    /** 指定した動画の再生位置を取得する。保存していない場合は`null`を返す。 */
    @Query("SELECT * FROM playback_positions WHERE video_id = :videoId")
    suspend fun getByVideoId(videoId: String): PlaybackPositionEntity?

    /** すべての再生位置を取得し、テーブルが変わるたびに新しい一覧を出力する。 */
    @Query("SELECT * FROM playback_positions")
    fun observeAll(): Flow<List<PlaybackPositionEntity>>
}
