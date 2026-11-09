package io.github.daichimuratsu.videostreaming.core.testing

import io.kotest.core.listeners.AfterTestListener
import io.kotest.core.listeners.BeforeTestListener
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * テストのあいだ、`Dispatchers.Main`をテスト用のディスパッチャーに差し替えるKotestの拡張。
 *
 * `viewModelScope`はMainを使うが、ユニットテストにはAndroidのメインスレッドがないため。
 * 公式の`MainDispatcherRule`（JUnit 4のRule）と同じく、テストのたびに差し替えて、終わったら戻す
 */
class MainDispatcherExtension :
    BeforeTestListener,
    AfterTestListener {
    /** 差し替えたディスパッチャー。仮想の時間を進めるテストで、`scheduler`を使う。 */
    lateinit var testDispatcher: TestDispatcher
        private set

    override suspend fun beforeTest(testCase: TestCase) {
        // テストごとに作り直し、前のテストで進めた仮想の時間を持ち越さない
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        // 戻さないと、あとに実行するテストが前のテストのディスパッチャーを使い、実行順で結果が変わる
        Dispatchers.resetMain()
    }
}
