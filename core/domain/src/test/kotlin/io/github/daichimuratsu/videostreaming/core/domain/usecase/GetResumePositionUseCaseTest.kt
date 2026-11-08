package io.github.daichimuratsu.videostreaming.core.domain.usecase

import io.github.daichimuratsu.videostreaming.core.domain.fake.FakePlaybackPositionRepository
import io.github.daichimuratsu.videostreaming.core.domain.fake.testVideo
import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// 判定の境界値はWatchStatusTestで確かめているので、ここでは視聴状態ごとの代表値だけを確かめる
class GetResumePositionUseCaseTest : FunSpec({
    val video = testVideo("sintel", 600.seconds)

    fun useCase(saved: PlaybackPosition? = null) = GetResumePositionUseCase(
        FakePlaybackPositionRepository(
            if (saved == null) emptyMap() else mapOf(VideoId("sintel") to saved),
        ),
    )

    test("再生位置が保存されていなければ、最初から再生する") {
        useCase()(video) shouldBe PlaybackPosition(Duration.ZERO)
    }

    test("未視聴（10秒未満）なら、最初から再生する") {
        useCase(PlaybackPosition(3.seconds))(video) shouldBe PlaybackPosition(Duration.ZERO)
    }

    test("視聴途中なら、保存した再生位置から再生する") {
        useCase(PlaybackPosition(300.seconds))(video) shouldBe PlaybackPosition(300.seconds)
    }

    test("視聴済みなら、最初から再生する") {
        useCase(PlaybackPosition(580.seconds))(video) shouldBe PlaybackPosition(Duration.ZERO)
    }
})
