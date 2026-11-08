package io.github.daichimuratsu.videostreaming.core.data.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import io.github.daichimuratsu.videostreaming.core.data.database.model.PlaybackPositionEntity

/** アプリの端末内のデータベース。 */
@Database(entities = [PlaybackPositionEntity::class], version = 1)
internal abstract class VideoStreamingDatabase : RoomDatabase() {
    abstract fun playbackPositionDao(): PlaybackPositionDao
}
