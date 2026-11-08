package io.github.daichimuratsu.videostreaming.core.domain.usecase

import app.cash.turbine.test
import io.github.daichimuratsu.videostreaming.core.domain.fake.FakePlaybackPositionRepository
import io.github.daichimuratsu.videostreaming.core.domain.fake.FakeVideoRepository
import io.github.daichimuratsu.videostreaming.core.domain.fake.testVideo
import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.model.WatchStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.time.Duration.Companion.seconds

class GetVideoListWithWatchStatusUseCaseTest : FunSpec({
    val videos = listOf(
        testVideo("big-buck-bunny", 600.seconds),
        testVideo("sintel", 600.seconds),
        testVideo("tears-of-steel", 600.seconds),
    )

    test("動画の一覧に、動画ごとの視聴状態を組み合わせる。動画の順番は変えない") {
        val positions = FakePlaybackPositionRepository(
            mapOf(
                VideoId("sintel") to PlaybackPosition(300.seconds),
                VideoId("tears-of-steel") to PlaybackPosition(580.seconds),
            ),
        )
        val useCase = GetVideoListWithWatchStatusUseCase(FakeVideoRepository(videos), positions)

        useCase().test {
            val list = awaitItem()
            list.map { it.video } shouldBe videos
            list.map { it.status } shouldBe listOf(
                WatchStatus.NotStarted,
                WatchStatus.InProgress(0.5),
                WatchStatus.Watched,
            )
        }
    }

    test("再生位置を保存すると、新しい一覧を出力する") {
        val positions = FakePlaybackPositionRepository()
        val useCase = GetVideoListWithWatchStatusUseCase(FakeVideoRepository(videos), positions)

        useCase().test {
            awaitItem().map { it.status } shouldBe List(3) { WatchStatus.NotStarted }

            positions.savePosition(VideoId("big-buck-bunny"), PlaybackPosition(300.seconds))

            awaitItem().map { it.status } shouldBe listOf(
                WatchStatus.InProgress(0.5),
                WatchStatus.NotStarted,
                WatchStatus.NotStarted,
            )
        }
    }

    test("動画の一覧の取得に失敗したら、その例外をそのまま伝える") {
        val error = IllegalStateException("取得の失敗")
        val useCase = GetVideoListWithWatchStatusUseCase(
            FakeVideoRepository(videos, error = error),
            FakePlaybackPositionRepository(),
        )

        useCase().test {
            awaitError() shouldBeSameInstanceAs error
        }
    }
})
