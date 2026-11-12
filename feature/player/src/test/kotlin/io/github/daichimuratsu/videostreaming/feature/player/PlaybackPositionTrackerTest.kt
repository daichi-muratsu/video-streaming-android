package io.github.daichimuratsu.videostreaming.feature.player

import io.github.daichimuratsu.videostreaming.core.domain.fake.FakePlaybackPositionRepository
import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.feature.player.fake.FakePlaybackProgress
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class PlaybackPositionTrackerTest : FunSpec({
    val videoId = VideoId("big-buck-bunny")

    // 仮想の時間を進め、その時刻に予定された処理まで実行する。`advanceTimeBy`だけでは、ちょうどその時刻の処理を実行しないため
    fun TestScope.advance(duration: Duration) {
        testScheduler.advanceTimeBy(duration)
        testScheduler.runCurrent()
    }

    // テストごとに、Fake・Trackerを用意する。監視は`backgroundScope`で動かし、テストの終わりに止める
    class Fixture(scope: TestScope) {
        val repository = FakePlaybackPositionRepository()
        val progress = FakePlaybackProgress { scope.testScheduler.currentTime.milliseconds }
        val tracker = PlaybackPositionTracker(videoId, progress, repository, scope.backgroundScope)

        suspend fun savedPosition(): PlaybackPosition? = repository.getPosition(videoId)
    }

    fun TestScope.startTracking(): Fixture = Fixture(this).also { fixture ->
        backgroundScope.launch { fixture.tracker.track() }
        testScheduler.runCurrent()
    }

    fun TestScope.play(fixture: Fixture, isPlaying: Boolean) {
        fixture.progress.isPlaying.value = isPlaying
        testScheduler.runCurrent()
    }

    context("再生を続けた時間で、一定の間隔の保存をするかを分ける") {
        test("9.999秒では、保存しない") {
            runTest {
                val fixture = startTracking()
                play(fixture, isPlaying = true)

                advance(9_999.milliseconds)

                fixture.savedPosition().shouldBeNull()
            }
        }

        test("10秒で、その時点の位置を保存する") {
            runTest {
                val fixture = startTracking()
                play(fixture, isPlaying = true)

                advance(10.seconds)

                fixture.savedPosition() shouldBe PlaybackPosition(10.seconds)
            }
        }

        test("25秒では、最後に保存した位置は20秒") {
            runTest {
                val fixture = startTracking()
                play(fixture, isPlaying = true)

                advance(25.seconds)

                fixture.savedPosition() shouldBe PlaybackPosition(20.seconds)
            }
        }
    }

    context("再生が止まったとき") {
        test("再生中に7秒で止まると、7秒を保存する") {
            runTest {
                val fixture = startTracking()
                play(fixture, isPlaying = true)
                advance(7.seconds)

                play(fixture, isPlaying = false)

                fixture.savedPosition() shouldBe PlaybackPosition(7.seconds)
            }
        }

        test("止まったまま10秒以上たっても、新しく保存しない") {
            runTest {
                val fixture = startTracking()
                play(fixture, isPlaying = true)
                advance(7.seconds)
                play(fixture, isPlaying = false)

                advance(20.seconds)

                fixture.savedPosition() shouldBe PlaybackPosition(7.seconds)
            }
        }

        test("再開してから9.999秒では、一定の間隔の保存をしない") {
            runTest {
                val fixture = startTracking()
                play(fixture, isPlaying = true)
                advance(7.seconds)
                play(fixture, isPlaying = false)
                advance(3.seconds)
                play(fixture, isPlaying = true)

                advance(9_999.milliseconds)

                fixture.savedPosition() shouldBe PlaybackPosition(7.seconds)
            }
        }

        test("再開してから10秒で、その時点の位置を保存する") {
            runTest {
                val fixture = startTracking()
                play(fixture, isPlaying = true)
                advance(7.seconds)
                play(fixture, isPlaying = false)
                advance(3.seconds)
                play(fixture, isPlaying = true)

                advance(10.seconds)

                // 7秒の再生＋3秒の停止＋10秒の再生
                fixture.savedPosition() shouldBe PlaybackPosition(20.seconds)
            }
        }

        test("監視を始めたときに再生中でなければ、保存しない") {
            runTest {
                val fixture = startTracking()

                advance(20.seconds)

                fixture.savedPosition().shouldBeNull()
            }
        }
    }

    context("画面を離れるときの保存") {
        test("呼んだ時点の位置を保存する") {
            runTest {
                val fixture = startTracking()
                advance(3.seconds)

                fixture.tracker.save()
                testScheduler.runCurrent()

                fixture.savedPosition() shouldBe PlaybackPosition(3.seconds)
            }
        }

        test("監視のスコープを止めたあとに呼んでも、保存が最後まで終わる") {
            runTest {
                val fixture = Fixture(this)
                // 画面のスコープ（`viewModelScope`）の代わり
                val screenScope = CoroutineScope(coroutineContext + Job())
                screenScope.launch { fixture.tracker.track() }
                testScheduler.runCurrent()
                play(fixture, isPlaying = true)
                advance(3.seconds)

                screenScope.cancel()
                fixture.tracker.save()
                testScheduler.runCurrent()

                fixture.savedPosition() shouldBe PlaybackPosition(3.seconds)
            }
        }
    }
})
