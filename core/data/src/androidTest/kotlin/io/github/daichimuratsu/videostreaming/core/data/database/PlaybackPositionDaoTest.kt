package io.github.daichimuratsu.videostreaming.core.data.database

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.github.daichimuratsu.videostreaming.core.data.database.model.PlaybackPositionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `PlaybackPositionDao`を、端末のメモリ上のデータベースで確かめるテスト。
 *
 * RepositoryのテストではDAOをFakeに差し替えているので、Fakeと本物のRoomの動きが合っているかをここで確かめる
 */
@RunWith(AndroidJUnit4::class)
class PlaybackPositionDaoTest {
    private lateinit var database: VideoStreamingDatabase
    private lateinit var dao: PlaybackPositionDao

    @Before
    fun setUp() {
        // テストごとに空のデータベースにするため、メモリ上に作る。ドライバーはアプリと同じにする
        database =
            Room.inMemoryDatabaseBuilder<VideoStreamingDatabase>(
                ApplicationProvider.getApplicationContext<Context>(),
            )
                .setDriver(BundledSQLiteDriver())
                .build()
        dao = database.playbackPositionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `保存していない動画はnullを返す`() = runTest {
        assertNull(dao.getByVideoId("big-buck-bunny"))
    }

    @Test
    fun `保存した行を取得できる`() = runTest {
        dao.upsert(PlaybackPositionEntity("big-buck-bunny", 30_000))

        assertEquals(
            PlaybackPositionEntity("big-buck-bunny", 30_000),
            dao.getByVideoId("big-buck-bunny"),
        )
    }

    @Test
    fun `同じ動画を2回保存すると1行だけで後の値になる`() = runTest {
        dao.upsert(PlaybackPositionEntity("big-buck-bunny", 30_000))
        dao.upsert(PlaybackPositionEntity("big-buck-bunny", 45_000))

        dao.observeAll().test {
            assertEquals(listOf(PlaybackPositionEntity("big-buck-bunny", 45_000)), awaitItem())
        }
    }

    @Test
    fun `保存するたびに新しい一覧を出力する`() = runTest {
        dao.observeAll().test {
            assertEquals(emptyList<PlaybackPositionEntity>(), awaitItem())

            dao.upsert(PlaybackPositionEntity("big-buck-bunny", 30_000))
            assertEquals(listOf(PlaybackPositionEntity("big-buck-bunny", 30_000)), awaitItem())

            dao.upsert(PlaybackPositionEntity("sintel", 60_000))
            assertEquals(
                setOf(
                    PlaybackPositionEntity("big-buck-bunny", 30_000),
                    PlaybackPositionEntity("sintel", 60_000),
                ),
                // `ORDER BY`を付けていないので、行の順番は確かめない
                awaitItem().toSet(),
            )
        }
    }
}
