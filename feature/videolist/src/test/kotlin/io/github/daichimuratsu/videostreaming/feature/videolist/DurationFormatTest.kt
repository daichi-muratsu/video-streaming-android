package io.github.daichimuratsu.videostreaming.feature.videolist

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DurationFormatTest : FunSpec({
    context("0秒以上、1分未満は「0:秒」") {
        test("0秒（下端）") { formatDuration(0.seconds) shouldBe "0:00" }
        test("59秒（上端）") { formatDuration(59.seconds) shouldBe "0:59" }
        test("5秒（秒をゼロ埋めする）") { formatDuration(5.seconds) shouldBe "0:05" }
    }

    context("1分以上、10分未満は「分:秒」で、分は1桁") {
        test("60秒（下端）") { formatDuration(60.seconds) shouldBe "1:00" }
        test("599秒（上端）") { formatDuration(599.seconds) shouldBe "9:59" }
        test("596秒") { formatDuration(596.seconds) shouldBe "9:56" }
    }

    context("10分以上、1時間未満は「分:秒」で、分は2桁") {
        test("600秒（下端）") { formatDuration(600.seconds) shouldBe "10:00" }
        test("3599秒（上端）") { formatDuration(3599.seconds) shouldBe "59:59" }
        test("888秒") { formatDuration(888.seconds) shouldBe "14:48" }
    }

    context("1時間以上は「時:分:秒」") {
        test("3600秒（下端）") { formatDuration(3600.seconds) shouldBe "1:00:00" }
        test("3723秒（分と秒をゼロ埋めする）") { formatDuration(3723.seconds) shouldBe "1:02:03" }
    }

    test("秒未満は切り捨てる。59.999秒は、1:00ではなく0:59") {
        formatDuration(59_999.milliseconds) shouldBe "0:59"
    }
})
