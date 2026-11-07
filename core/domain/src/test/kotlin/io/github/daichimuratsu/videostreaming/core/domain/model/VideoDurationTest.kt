package io.github.daichimuratsu.videostreaming.core.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class VideoDurationTest : FunSpec({
    test("0より少しでも大きければ作れる") {
        VideoDuration(1.nanoseconds).value shouldBe 1.nanoseconds
    }

    test("正の値は作れる") {
        VideoDuration(596.seconds).value shouldBe 596.seconds
    }

    test("0では作れない") {
        shouldThrow<IllegalArgumentException> { VideoDuration(Duration.ZERO) }
    }

    test("負の値では作れない") {
        shouldThrow<IllegalArgumentException> { VideoDuration((-1).nanoseconds) }
    }
})
