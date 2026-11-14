package io.github.daichimuratsu.videostreaming.feature.player

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

// 範囲ごとに、両端の値（境界値）と中の値（代表値）を1件ずつのテストにする。失敗したときに、どの角度かがテストの名前でわかるため
class OrientationLockTest : FunSpec({
    context("横に固定しているとき") {
        test("向きがわからなければ（-1）、追いついていない") {
            OrientationLock.Landscape.isReachedBy(-1) shouldBe false
        }
        listOf(0, 30, 59, 121, 180, 239, 301, 330, 359).forEach { degrees ->
            test("${degrees}度なら、追いついていない") {
                OrientationLock.Landscape.isReachedBy(degrees) shouldBe false
            }
        }
        listOf(60, 90, 120, 240, 270, 300).forEach { degrees ->
            test("${degrees}度なら、追いついた") {
                OrientationLock.Landscape.isReachedBy(degrees) shouldBe true
            }
        }
    }

    context("縦に固定しているとき") {
        test("向きがわからなければ（-1）、追いついていない") {
            OrientationLock.Portrait.isReachedBy(-1) shouldBe false
        }
        listOf(31, 180, 329).forEach { degrees ->
            test("${degrees}度なら、追いついていない") {
                OrientationLock.Portrait.isReachedBy(degrees) shouldBe false
            }
        }
        listOf(0, 15, 30, 330, 345, 359).forEach { degrees ->
            test("${degrees}度なら、追いついた") {
                OrientationLock.Portrait.isReachedBy(degrees) shouldBe true
            }
        }
    }
})
