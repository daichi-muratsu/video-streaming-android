package io.github.daichimuratsu.videostreaming.core.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VideoIdTest : FunSpec({
    test("1文字でも作れる") {
        VideoId("a").value shouldBe "a"
    }

    test("空白以外の文字があれば作れる") {
        VideoId("big-buck-bunny").value shouldBe "big-buck-bunny"
    }

    test("空の文字列では作れない") {
        shouldThrow<IllegalArgumentException> { VideoId("") }
    }

    test("空白だけの文字列では作れない") {
        shouldThrow<IllegalArgumentException> { VideoId("  ") }
    }
})
