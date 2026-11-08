package io.github.daichimuratsu.videostreaming.core.data.repository

import app.cash.turbine.test
import io.github.daichimuratsu.videostreaming.core.data.fake.FakePlaybackPositionDao
import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class PlaybackPositionRepositoryImplTest : FunSpec({
    val bunny = VideoId("big-buck-bunny")
    val sintel = VideoId("sintel")

    test("保存していない動画は、nullを返す") {
        val repository = PlaybackPositionRepositoryImpl(FakePlaybackPositionDao())

        repository.getPosition(bunny).shouldBeNull()
    }

    test("保存した再生位置を取得できる") {
        val repository = PlaybackPositionRepositoryImpl(FakePlaybackPositionDao())

        repository.savePosition(bunny, PlaybackPosition(30.seconds))

        repository.getPosition(bunny) shouldBe PlaybackPosition(30.seconds)
    }

    test("同じ動画を2回保存すると、後の値になる") {
        val repository = PlaybackPositionRepositoryImpl(FakePlaybackPositionDao())

        repository.savePosition(bunny, PlaybackPosition(30.seconds))
        repository.savePosition(bunny, PlaybackPosition(45.seconds))

        repository.getPosition(bunny) shouldBe PlaybackPosition(45.seconds)
    }

    test("保存するたびに、動画のIDごとの再生位置の新しいMapを出力する") {
        val repository = PlaybackPositionRepositoryImpl(FakePlaybackPositionDao())

        repository.observePositions().test {
            awaitItem() shouldBe emptyMap()

            repository.savePosition(bunny, PlaybackPosition(30.seconds))
            awaitItem() shouldBe mapOf(bunny to PlaybackPosition(30.seconds))

            repository.savePosition(sintel, PlaybackPosition(60.seconds))
            awaitItem() shouldBe mapOf(
                bunny to PlaybackPosition(30.seconds),
                sintel to PlaybackPosition(60.seconds),
            )
        }
    }
})
