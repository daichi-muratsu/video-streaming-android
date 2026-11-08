package io.github.daichimuratsu.videostreaming.core.data.network.model

import io.github.daichimuratsu.videostreaming.core.data.fake.testVideoDto
import io.github.daichimuratsu.videostreaming.core.domain.model.License
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoDuration
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.time.Duration.Companion.seconds

// 値オブジェクトのルールの境界値は`:core:domain`のテストで確かめてあるので、ここでは変換と例外の伝わり方を代表値で確かめる
class VideoDtoTest : FunSpec({
    test("秒の数値・日付の文字列・IDを、ドメインモデルの型に変換する") {
        val video = testVideoDto(
            id = "big-buck-bunny",
            durationSeconds = 596,
            publishedAt = "2008-05-30",
        ).toDomain()

        video.id shouldBe VideoId("big-buck-bunny")
        video.duration shouldBe VideoDuration(596.seconds)
        video.publishedAt shouldBe LocalDate.of(2008, 5, 30)
    }

    test("文字列の項目・タグ・ライセンスは、そのまま渡す") {
        val dto = testVideoDto()

        val video = dto.toDomain()

        video.title shouldBe dto.title
        video.description shouldBe dto.description
        video.creator shouldBe dto.creator
        video.thumbnailUrl shouldBe dto.thumbnailUrl
        video.streamUrl shouldBe dto.streamUrl
        video.tags shouldBe dto.tags
        video.license shouldBe License(
            name = dto.license.name,
            url = dto.license.url,
            attribution = dto.license.attribution,
            changes = dto.license.changes,
        )
    }

    context("正しくない値は、例外をそのまま投げる") {
        test("長さが0秒") {
            shouldThrow<IllegalArgumentException> { testVideoDto(durationSeconds = 0).toDomain() }
        }

        test("日付の形式の誤り（ありえない月）") {
            shouldThrow<DateTimeParseException> {
                testVideoDto(publishedAt = "2008-13-01").toDomain()
            }
        }

        test("IDが空") {
            shouldThrow<IllegalArgumentException> { testVideoDto(id = "").toDomain() }
        }
    }
})
