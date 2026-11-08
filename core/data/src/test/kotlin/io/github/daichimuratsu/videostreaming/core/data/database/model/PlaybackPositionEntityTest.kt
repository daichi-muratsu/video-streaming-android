package io.github.daichimuratsu.videostreaming.core.data.database.model

import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

class PlaybackPositionEntityTest : FunSpec({
    val id = VideoId("big-buck-bunny")

    test("0は、0ミリ秒として保存し、0に戻す") {
        PlaybackPosition(Duration.ZERO).toEntity(id) shouldBe
            PlaybackPositionEntity("big-buck-bunny", 0)
        PlaybackPositionEntity("big-buck-bunny", 0).toDomain() shouldBe
            PlaybackPosition(Duration.ZERO)
    }

    test("ミリ秒の倍数の位置は、同じ値に戻る") {
        PlaybackPosition(125_300.milliseconds).toEntity(id) shouldBe
            PlaybackPositionEntity("big-buck-bunny", 125_300)
        PlaybackPositionEntity("big-buck-bunny", 125_300).toDomain() shouldBe
            PlaybackPosition(125_300.milliseconds)
    }

    test("1ミリ秒未満の端数は切り捨てる") {
        PlaybackPosition(1_999_999.nanoseconds).toEntity(id).positionMillis shouldBe 1
    }
})
