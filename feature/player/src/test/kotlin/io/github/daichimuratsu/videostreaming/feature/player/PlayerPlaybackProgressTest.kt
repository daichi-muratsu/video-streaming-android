package io.github.daichimuratsu.videostreaming.feature.player

import app.cash.turbine.test
import io.github.daichimuratsu.videostreaming.feature.player.fake.FakePlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.runTest

class PlayerPlaybackProgressTest : FunSpec({
    test("Playerの再生位置が1,500ミリ秒なら、1.5秒を返す") {
        val player = FakePlayer().apply { currentPositionMs = 1_500 }

        PlayerPlaybackProgress(player).currentPosition shouldBe 1_500.milliseconds
    }

    test("受け取り始めると現在の値を出力し、そのあとはPlayerが知らせるたびに出力する") {
        val player = FakePlayer()
        runTest {
            PlayerPlaybackProgress(player).isPlaying.test {
                awaitItem() shouldBe false

                player.changeIsPlaying(true)
                awaitItem() shouldBe true

                player.changeIsPlaying(false)
                awaitItem() shouldBe false
            }
        }
    }

    test("受け取りをやめると、Playerからリスナーを外す") {
        val player = FakePlayer()
        runTest {
            PlayerPlaybackProgress(player).isPlaying.test {
                awaitItem()
                player.listeners shouldHaveSize 1
            }
        }
        player.listeners.shouldBeEmpty()
    }
})
