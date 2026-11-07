package io.github.daichimuratsu.videostreaming.core.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class PlaybackPositionTest : FunSpec({
    test("0は作れる") {
        PlaybackPosition(Duration.ZERO).value shouldBe Duration.ZERO
    }

    test("正の値は作れる") {
        PlaybackPosition(10.seconds).value shouldBe 10.seconds
    }

    test("負の値では作れない") {
        shouldThrow<IllegalArgumentException> { PlaybackPosition((-1).nanoseconds) }
    }
})
