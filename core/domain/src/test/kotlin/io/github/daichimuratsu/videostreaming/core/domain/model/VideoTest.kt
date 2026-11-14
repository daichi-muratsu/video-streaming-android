package io.github.daichimuratsu.videostreaming.core.domain.model

import io.github.daichimuratsu.videostreaming.core.domain.fake.testVideo
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class VideoTest : FunSpec({
    fun video(id: String, vararg tags: String) = testVideo(id, 600.seconds, tags.toList())

    // 再生中の動画。共通のタグの数を0・1・2で変えられるように、タグを2つ持たせる
    val current = video("current", "animation", "fantasy")

    context("共通のタグが1個以上の動画だけを、関連動画にする") {
        test("共通のタグが0個の動画は含めない") {
            val other = video("other", "sci-fi")

            current.relatedVideosIn(listOf(current, other)).shouldBeEmpty()
        }

        test("共通のタグが1個の動画は含める") {
            val other = video("other", "animation", "comedy")

            current.relatedVideosIn(listOf(current, other)) shouldBe listOf(other)
        }

        test("共通のタグが2個の動画は含める") {
            val other = video("other", "animation", "fantasy")

            current.relatedVideosIn(listOf(current, other)) shouldBe listOf(other)
        }
    }

    test("再生中の動画は、すべてのタグが共通でも含めない") {
        current.relatedVideosIn(listOf(current)).shouldBeEmpty()
    }

    context("共通のタグが多い順に並べる") {
        test("共通のタグが2個の動画を、1個の動画より先に並べる") {
            val one = video("one", "animation")
            val two = video("two", "animation", "fantasy")

            current.relatedVideosIn(listOf(one, two)) shouldBe listOf(two, one)
        }

        test("共通のタグの数が同じなら、もとの順に並べる") {
            val first = video("first", "animation")
            val second = video("second", "fantasy")

            current.relatedVideosIn(listOf(first, second)) shouldBe listOf(first, second)
        }
    }

    test("タグが共通する動画がなければ、空の一覧を返す") {
        val others = listOf(video("first", "comedy"), video("second", "sci-fi"))

        current.relatedVideosIn(listOf(current) + others).shouldBeEmpty()
    }
})
