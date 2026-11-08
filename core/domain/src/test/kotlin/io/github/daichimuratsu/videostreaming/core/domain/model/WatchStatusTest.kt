package io.github.daichimuratsu.videostreaming.core.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

// 長い動画と短い動画で、テストを分ける。分かれ目は、90%の位置がちょうど10秒になる長さ（10秒÷0.9。約11.1秒）。
// これより長い動画では、90%の位置が10秒より後になり、2つのルール（90%以上・10秒未満）がぶつからない
class WatchStatusTest : FunSpec({
    test("再生位置が保存されていなければ、未視聴") {
        WatchStatus.of(null, VideoDuration(600.seconds)) shouldBe WatchStatus.NotStarted
    }

    context("長い動画（90%の位置が10秒より後になる）") {
        // 90%は540秒
        val duration = VideoDuration(600.seconds)

        test("0秒なら、未視聴") {
            WatchStatus.of(PlaybackPosition(0.seconds), duration) shouldBe WatchStatus.NotStarted
        }

        test("10秒未満なら、未視聴") {
            WatchStatus.of(PlaybackPosition(3.seconds), duration) shouldBe WatchStatus.NotStarted
        }

        test("10秒の1ナノ秒前なら、未視聴") {
            val position = PlaybackPosition(10.seconds - 1.nanoseconds)
            WatchStatus.of(position, duration) shouldBe WatchStatus.NotStarted
        }

        test("ちょうど10秒なら、視聴途中") {
            val status = WatchStatus.of(PlaybackPosition(10.seconds), duration)
            status.shouldBeInstanceOf<WatchStatus.InProgress>()
        }

        test("視聴途中の割合は、位置÷長さ") {
            WatchStatus.of(PlaybackPosition(300.seconds), duration) shouldBe
                WatchStatus.InProgress(0.5)
        }

        test("90%の1ナノ秒前なら、視聴途中") {
            val status = WatchStatus.of(PlaybackPosition(540.seconds - 1.nanoseconds), duration)
            status.shouldBeInstanceOf<WatchStatus.InProgress>()
        }

        test("ちょうど90%なら、視聴済み") {
            WatchStatus.of(PlaybackPosition(540.seconds), duration) shouldBe WatchStatus.Watched
        }

        test("90%を超えたら、視聴済み") {
            WatchStatus.of(PlaybackPosition(580.seconds), duration) shouldBe WatchStatus.Watched
        }

        test("長さを超えても、視聴済み") {
            WatchStatus.of(PlaybackPosition(601.seconds), duration) shouldBe WatchStatus.Watched
        }
    }

    context("短い動画（90%の位置が10秒より前になる）") {
        // 90%は4.5秒
        val duration = VideoDuration(5.seconds)

        test("90%未満なら、未視聴") {
            WatchStatus.of(PlaybackPosition(3.seconds), duration) shouldBe WatchStatus.NotStarted
        }

        test("90%の1ナノ秒前なら、未視聴") {
            val position = PlaybackPosition(4.5.seconds - 1.nanoseconds)
            WatchStatus.of(position, duration) shouldBe WatchStatus.NotStarted
        }

        test("10秒未満でも、ちょうど90%なら視聴済み") {
            WatchStatus.of(PlaybackPosition(4.5.seconds), duration) shouldBe WatchStatus.Watched
        }

        test("最後まで再生したら、視聴済み") {
            WatchStatus.of(PlaybackPosition(5.seconds), duration) shouldBe WatchStatus.Watched
        }
    }
})
