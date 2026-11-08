package io.github.daichimuratsu.videostreaming.core.data.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.daichimuratsu.videostreaming.core.data.database.PlaybackPositionDao
import io.github.daichimuratsu.videostreaming.core.data.database.VideoStreamingDatabase
import javax.inject.Singleton

/** 端末のデータベース（Room）を提供するHiltのModule。 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    // ファイル名を変えると、端末に保存した再生位置が引き継がれないので、変えない
    private const val DATABASE_NAME = "video-streaming.db"

    // `RoomDatabase`のインスタンスは作るコストが大きいので、公式のガイドの推奨に従い、アプリで1つにする
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VideoStreamingDatabase =
        Room.databaseBuilder<VideoStreamingDatabase>(context, DATABASE_NAME)
            // Androidのバージョンによらず同じSQLiteを使い、DAOのテストと同じドライバーにするため
            .setDriver(BundledSQLiteDriver())
            .build()

    @Provides
    fun providePlaybackPositionDao(database: VideoStreamingDatabase): PlaybackPositionDao =
        database.playbackPositionDao()
}
