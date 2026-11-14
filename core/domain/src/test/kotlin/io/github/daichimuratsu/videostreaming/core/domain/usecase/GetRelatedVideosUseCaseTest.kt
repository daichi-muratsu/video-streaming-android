package io.github.daichimuratsu.videostreaming.core.domain.usecase

import io.github.daichimuratsu.videostreaming.core.domain.fake.FakeVideoRepository
import io.github.daichimuratsu.videostreaming.core.domain.fake.testVideo
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

// 選び方の境界値はVideoTestで確かめているので、ここでは一覧の取得との組み合わせだけを確かめる
class GetRelatedVideosUseCaseTest : FunSpec({
    val current = testVideo("current", 600.seconds, listOf("animation"))
    val related = testVideo("related", 600.seconds, listOf("animation"))
    val unrelated = testVideo("unrelated", 600.seconds, listOf("sci-fi"))

    test("取得した一覧から、タグが共通する動画を返す") {
        val useCase =
            GetRelatedVideosUseCase(FakeVideoRepository(listOf(current, related, unrelated)))

        useCase(current) shouldBe listOf(related)
    }

    test("一覧の取得に失敗したら、例外を投げる") {
        val useCase = GetRelatedVideosUseCase(
            FakeVideoRepository(listOf(current, related), error = IOException()),
        )

        shouldThrow<IOException> { useCase(current) }
    }
})
