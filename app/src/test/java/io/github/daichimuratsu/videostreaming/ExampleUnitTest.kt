package io.github.daichimuratsu.videostreaming

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** ひな形のサンプルのテストを、Kotest（FunSpec）で書き直したもの。Kotestが動くことの確認に使う */
class ExampleUnitTest : FunSpec({
    test("2 + 2 は 4 になる") {
        (2 + 2) shouldBe 4
    }
})
