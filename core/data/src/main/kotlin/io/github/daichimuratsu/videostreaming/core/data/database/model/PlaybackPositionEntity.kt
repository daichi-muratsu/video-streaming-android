package io.github.daichimuratsu.videostreaming.core.data.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import kotlin.time.Duration.Companion.milliseconds

/**
 * 動画ごとの再生位置を保存するテーブルの1行を表すEntity。
 *
 * 同じ動画の再生位置を上書きするため、動画のIDを主キーにして、1本の動画に1行だけ持つ。
 * 再生位置は、`Player`の値と単位を揃えるためミリ秒の`Long`で保存し、単位がわかるように列名に`millis`を入れる
 */
@Entity(tableName = "playback_positions")
internal data class PlaybackPositionEntity(
    @PrimaryKey
    @ColumnInfo(name = "video_id")
    val videoId: String,
    @ColumnInfo(name = "position_millis")
    val positionMillis: Long,
)

/** 保存した再生位置を、Domain層の`PlaybackPosition`に変換する。 */
internal fun PlaybackPositionEntity.toDomain(): PlaybackPosition =
    PlaybackPosition(positionMillis.milliseconds)

/**
 * Domain層の`PlaybackPosition`を、保存するEntityに変換する。
 *
 * 1ミリ秒未満は切り捨てる。`Player`の再生位置はミリ秒単位なので、失われる値はない
 */
internal fun PlaybackPosition.toEntity(id: VideoId): PlaybackPositionEntity =
    PlaybackPositionEntity(
        videoId = id.value,
        positionMillis = value.inWholeMilliseconds,
    )
