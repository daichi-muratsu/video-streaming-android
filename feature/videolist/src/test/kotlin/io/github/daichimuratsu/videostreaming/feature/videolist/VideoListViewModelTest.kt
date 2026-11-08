package io.github.daichimuratsu.videostreaming.feature.videolist

import app.cash.turbine.test
import io.github.daichimuratsu.videostreaming.core.domain.fake.FakePlaybackPositionRepository
import io.github.daichimuratsu.videostreaming.core.domain.fake.FakeVideoRepository
import io.github.daichimuratsu.videostreaming.core.domain.fake.testVideo
import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoWithWatchStatus
import io.github.daichimuratsu.videostreaming.core.domain.model.WatchStatus
import io.github.daichimuratsu.videostreaming.core.domain.usecase.GetVideoListWithWatchStatusUseCase
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

// `runTest`で包むのは、テスト用のディスパッチャーに渡した処理を、`runTest`が仮想の時間を進めて実行するため。
// 包まないと誰も時間を進めず、次の値を待ち続ける。Mainを差し替えてあるので、`runTest`はMainと同じスケジューラーを使う
class VideoListViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    val bigBuckBunny = testVideo("big-buck-bunny", 600.seconds)
    val sintel = testVideo("sintel", 600.seconds)
    val tearsOfSteel = testVideo("tears-of-steel", 600.seconds)

    // 取得に1秒（仮想の時間）かける。すぐに返すと、読み込み中の状態が結果に上書きされて受け取れず、
    // 読み込み中を挟んだかどうかを確かめられないため
    fun fakeVideos(videos: List<Video>, error: Throwable? = null) =
        FakeVideoRepository(videos, error).apply { fetchDelay = 1.seconds }

    fun viewModel(
        videos: FakeVideoRepository,
        positions: FakePlaybackPositionRepository = FakePlaybackPositionRepository(),
    ) = VideoListViewModel(GetVideoListWithWatchStatusUseCase(videos, positions))

    fun notStarted(video: Video) = VideoWithWatchStatus(video, WatchStatus.NotStarted)

    // 画面が受け取りを始めたことにする。`stateIn`は、受け取る側がいないと読み込みを始めないため
    fun VideoListViewModel.startCollecting() =
        CoroutineScope(mainDispatcher.testDispatcher).launch { uiState.collect {} }

    test("受け取る前は、読み込み中") {
        val viewModel = viewModel(fakeVideos(listOf(bigBuckBunny)))

        viewModel.uiState.value shouldBe VideoListUiState.Loading
    }

    context("取得した動画の本数で、空か成功かに分ける") {
        test("0本なら、空") {
            runTest {
                viewModel(fakeVideos(emptyList())).uiState.test {
                    awaitItem() shouldBe VideoListUiState.Loading
                    awaitItem() shouldBe VideoListUiState.Empty
                }
            }
        }

        test("1本なら、成功") {
            runTest {
                viewModel(fakeVideos(listOf(bigBuckBunny))).uiState.test {
                    awaitItem() shouldBe VideoListUiState.Loading
                    awaitItem() shouldBe VideoListUiState.Success(listOf(notStarted(bigBuckBunny)))
                }
            }
        }

        test("3本なら、成功。順番と視聴状態はUseCaseの結果のまま") {
            runTest {
                val positions = FakePlaybackPositionRepository(
                    mapOf(
                        VideoId("sintel") to PlaybackPosition(300.seconds),
                        VideoId("tears-of-steel") to PlaybackPosition(580.seconds),
                    ),
                )
                val viewModel =
                    viewModel(fakeVideos(listOf(bigBuckBunny, sintel, tearsOfSteel)), positions)
                viewModel.uiState.test {
                    awaitItem() shouldBe VideoListUiState.Loading
                    awaitItem() shouldBe VideoListUiState.Success(
                        listOf(
                            notStarted(bigBuckBunny),
                            VideoWithWatchStatus(sintel, WatchStatus.InProgress(0.5)),
                            VideoWithWatchStatus(tearsOfSteel, WatchStatus.Watched),
                        ),
                    )
                }
            }
        }
    }

    context("取得の失敗と再読み込み") {
        test("取得に失敗したら、エラー") {
            runTest {
                viewModel(fakeVideos(listOf(bigBuckBunny), error = IOException())).uiState.test {
                    awaitItem() shouldBe VideoListUiState.Loading
                    awaitItem() shouldBe VideoListUiState.Error
                }
            }
        }

        test("失敗のあと、取得できる状態で再読み込みすると、読み込み中を経て成功になる") {
            runTest {
                val videos = fakeVideos(listOf(bigBuckBunny), error = IOException())
                val viewModel = viewModel(videos)
                viewModel.uiState.test {
                    awaitItem() shouldBe VideoListUiState.Loading
                    awaitItem() shouldBe VideoListUiState.Error

                    videos.error = null
                    viewModel.reload()

                    awaitItem() shouldBe VideoListUiState.Loading
                    awaitItem() shouldBe VideoListUiState.Success(listOf(notStarted(bigBuckBunny)))
                }
            }
        }

        test("失敗したまま再読み込みすると、読み込み中を経て、またエラーになる") {
            runTest {
                val viewModel = viewModel(fakeVideos(listOf(bigBuckBunny), error = IOException()))
                viewModel.uiState.test {
                    awaitItem() shouldBe VideoListUiState.Loading
                    awaitItem() shouldBe VideoListUiState.Error

                    viewModel.reload()

                    awaitItem() shouldBe VideoListUiState.Loading
                    awaitItem() shouldBe VideoListUiState.Error
                }
            }
        }
    }

    test("一覧を表示中に再生位置を保存すると、読み込み中を挟まずに、新しい視聴状態の一覧になる") {
        runTest {
            val positions = FakePlaybackPositionRepository()
            val viewModel = viewModel(fakeVideos(listOf(bigBuckBunny)), positions)
            viewModel.uiState.test {
                awaitItem() shouldBe VideoListUiState.Loading
                awaitItem() shouldBe VideoListUiState.Success(listOf(notStarted(bigBuckBunny)))

                positions.savePosition(VideoId("big-buck-bunny"), PlaybackPosition(300.seconds))

                awaitItem() shouldBe VideoListUiState.Success(
                    listOf(VideoWithWatchStatus(bigBuckBunny, WatchStatus.InProgress(0.5))),
                )
                testScheduler.advanceUntilIdle()
                expectNoEvents()
            }
        }
    }

    context("画面が受け取りをやめると、5秒後に再生位置の受け取りを止める") {
        test("4.999秒では、受け取りを続ける") {
            val positions = FakePlaybackPositionRepository()
            val viewModel = viewModel(fakeVideos(listOf(bigBuckBunny)), positions)
            viewModel.startCollecting().cancel()

            mainDispatcher.testDispatcher.scheduler.apply {
                advanceTimeBy(4_999)
                runCurrent()
            }

            positions.subscriptionCount shouldBe 1
        }

        test("5秒で、受け取りを止める") {
            val positions = FakePlaybackPositionRepository()
            val viewModel = viewModel(fakeVideos(listOf(bigBuckBunny)), positions)
            viewModel.startCollecting().cancel()

            mainDispatcher.testDispatcher.scheduler.apply {
                advanceTimeBy(5_000)
                runCurrent()
            }

            positions.subscriptionCount shouldBe 0
        }

        test("止めたあとに受け取りを再開すると、読み込み中を挟まずに一覧を表示する") {
            val viewModel = viewModel(fakeVideos(listOf(bigBuckBunny)))
            viewModel.startCollecting().cancel()
            mainDispatcher.testDispatcher.scheduler.apply {
                advanceTimeBy(5_000)
                runCurrent()
            }

            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe VideoListUiState.Success(listOf(notStarted(bigBuckBunny)))
                    // 再開すると取得し直す（1秒）。そのあいだも、取得し終えたあとも、表示中の一覧のまま変わらない
                    testScheduler.advanceUntilIdle()
                    expectNoEvents()
                }
            }
        }
    }
})
