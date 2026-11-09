package io.github.daichimuratsu.videostreaming.feature.player

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.test
import io.github.daichimuratsu.videostreaming.core.domain.fake.FakeVideoRepository
import io.github.daichimuratsu.videostreaming.core.domain.fake.testVideo
import io.github.daichimuratsu.videostreaming.core.testing.MainDispatcherExtension
import io.github.daichimuratsu.videostreaming.feature.player.fake.FakeMediaItemFactory
import io.github.daichimuratsu.videostreaming.feature.player.fake.FakePlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.IOException
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest

// `runTest`で包むのは、テスト用のディスパッチャーに渡した処理を、`runTest`が仮想の時間を進めて実行するため。
// Mainを差し替えてあるので、`runTest`はMainと同じスケジューラーを使う
class PlayerViewModelTest : FunSpec({
    extension(MainDispatcherExtension())

    val bigBuckBunny = testVideo("big-buck-bunny", 600.seconds)

    // 取得に1秒（仮想の時間）かける。すぐに返すと、読み込み中の状態が結果に上書きされて受け取れないため
    fun fakeVideos(error: Throwable? = null) =
        FakeVideoRepository(listOf(bigBuckBunny), error).apply { fetchDelay = 1.seconds }

    fun viewModel(
        videos: FakeVideoRepository,
        player: FakePlayer = FakePlayer(),
        videoId: String = bigBuckBunny.id.value,
    ) = PlayerViewModel(videoId, videos, FakeMediaItemFactory(), player)

    test("受け取る前は、読み込み中") {
        viewModel(fakeVideos()).uiState.value shouldBe PlayerUiState.Loading
    }

    context("取得の結果で、成功・該当なし・エラーに分ける") {
        test("該当する動画があれば、成功") {
            runTest {
                viewModel(fakeVideos()).uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny)
                }
            }
        }

        test("該当する動画がなければ、該当なし") {
            runTest {
                viewModel(fakeVideos(), videoId = "unknown").uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.NotFound
                }
            }
        }

        test("取得に失敗したら、エラー") {
            runTest {
                viewModel(fakeVideos(IOException())).uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Error
                }
            }
        }
    }

    context("取得の失敗と再読み込み") {
        test("失敗のあと、取得できる状態で再読み込みすると、読み込み中を経て成功になる") {
            val videos = fakeVideos(IOException())
            val viewModel = viewModel(videos)
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Error

                    videos.error = null
                    viewModel.reload()

                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny)
                }
            }
        }

        test("失敗したまま再読み込みすると、読み込み中を経て、またエラーになる") {
            val viewModel = viewModel(fakeVideos(IOException()))
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Error

                    viewModel.reload()

                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Error
                }
            }
        }
    }

    test("受け取りをやめて1時間後に再開しても、取得し直さない") {
        val videos = fakeVideos()
        val player = FakePlayer()
        val viewModel = viewModel(videos, player)
        runTest {
            viewModel.uiState.test {
                awaitItem() shouldBe PlayerUiState.Loading
                awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny)
            }
            testScheduler.advanceTimeBy(1.hours)
            testScheduler.runCurrent()
            // 取得し直すと、エラーになるようにしておく。成功のままなら、取得し直していない
            videos.error = IOException()

            viewModel.uiState.test {
                awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny)
                testScheduler.advanceUntilIdle()
                expectNoEvents()
            }
        }
        player.setMediaItemsList.size shouldBe 1
    }

    context("取得に成功したときだけ、Playerで再生を始める") {
        test("成功したら、その動画のMediaItemを設定し、prepareを呼び、playWhenReadyをtrueにする") {
            val player = FakePlayer()
            runTest {
                viewModel(fakeVideos(), player).uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny)
                }
            }
            player.setMediaItemsList.map { it.mediaId } shouldBe listOf("big-buck-bunny")
            player.prepareCount shouldBe 1
            player.playWhenReadyValue shouldBe true
        }

        test("該当する動画がなければ、MediaItemを設定せず、prepareも呼ばない") {
            val player = FakePlayer()
            runTest {
                viewModel(fakeVideos(), player, videoId = "unknown").uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.NotFound
                }
            }
            player.setMediaItemsList.shouldBeEmpty()
            player.prepareCount shouldBe 0
        }

        test("取得に失敗したら、MediaItemを設定せず、prepareも呼ばない") {
            val player = FakePlayer()
            runTest {
                viewModel(fakeVideos(IOException()), player).uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Error
                }
            }
            player.setMediaItemsList.shouldBeEmpty()
            player.prepareCount shouldBe 0
        }

        test("失敗のあと再読み込みで成功したら、MediaItemを設定するのは1回だけ") {
            val videos = fakeVideos(IOException())
            val player = FakePlayer()
            val viewModel = viewModel(videos, player)
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Error

                    videos.error = null
                    viewModel.reload()

                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny)
                }
            }
            player.setMediaItemsList.size shouldBe 1
        }
    }

    test("ViewModelを破棄すると、Playerを解放する") {
        val player = FakePlayer()
        // 画面と同じく`ViewModelStore`に入れ、`clear`で破棄する。`onCleared`はテストから直接呼べないため。
        // `lifecycle-viewmodel-testing`の`viewModelScenario`は、中でAndroidの`Bundle`を使い、JVMのユニットテストで失敗する
        val store = ViewModelStore()
        ViewModelProvider.create(
            store,
            viewModelFactory { initializer { viewModel(fakeVideos(), player) } },
        )[PlayerViewModel::class]
        player.releaseCount shouldBe 0

        store.clear()

        player.releaseCount shouldBe 1
    }
})
