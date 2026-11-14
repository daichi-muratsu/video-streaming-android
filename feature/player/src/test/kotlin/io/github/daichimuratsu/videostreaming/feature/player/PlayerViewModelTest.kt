package io.github.daichimuratsu.videostreaming.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.test
import io.github.daichimuratsu.videostreaming.core.domain.fake.FakePlaybackPositionRepository
import io.github.daichimuratsu.videostreaming.core.domain.fake.FakeVideoRepository
import io.github.daichimuratsu.videostreaming.core.domain.fake.testVideo
import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.usecase.GetRelatedVideosUseCase
import io.github.daichimuratsu.videostreaming.core.domain.usecase.GetResumePositionUseCase
import io.github.daichimuratsu.videostreaming.core.testing.MainDispatcherExtension
import io.github.daichimuratsu.videostreaming.feature.player.fake.FakeMediaItemFactory
import io.github.daichimuratsu.videostreaming.feature.player.fake.FakePlayer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.IOException
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        positions: FakePlaybackPositionRepository = FakePlaybackPositionRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = PlayerViewModel(
        videoId = videoId,
        savedStateHandle = savedStateHandle,
        videoRepository = videos,
        getResumePosition = GetResumePositionUseCase(positions),
        getRelatedVideos = GetRelatedVideosUseCase(videos),
        playbackPositionRepository = positions,
        mediaItemFactory = FakeMediaItemFactory(),
        // FakeのRepositoryはすぐに保存を終えるので、呼び出したスレッドでそのまま実行する
        applicationScope = CoroutineScope(Dispatchers.Unconfined),
        player = player,
    )

    // 画面と同じく`ViewModelStore`に入れる。`onCleared`はテストから直接呼べず、`ViewModelStore.clear`で起こすため。
    // `lifecycle-viewmodel-testing`の`viewModelScenario`は、中でAndroidの`Bundle`を使い、JVMのユニットテストで失敗する
    fun storedViewModel(store: ViewModelStore, create: () -> PlayerViewModel): PlayerViewModel =
        ViewModelProvider.create(
            store,
            viewModelFactory {
                initializer { create() }
            },
        )[PlayerViewModel::class]

    test("受け取る前は、読み込み中") {
        viewModel(fakeVideos()).uiState.value shouldBe PlayerUiState.Loading
    }

    context("取得の結果で、成功・該当なし・エラーに分ける") {
        test("該当する動画があれば、成功") {
            runTest {
                viewModel(fakeVideos()).uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, emptyList())
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
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, emptyList())
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
                awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, emptyList())
            }
            testScheduler.advanceTimeBy(1.hours)
            testScheduler.runCurrent()
            // 取得し直すと、エラーになるようにしておく。成功のままなら、取得し直していない
            videos.error = IOException()

            viewModel.uiState.test {
                awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, emptyList())
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
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, emptyList())
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
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, emptyList())
                }
            }
            player.setMediaItemsList.size shouldBe 1
        }
    }

    // 境界値（10秒・90%）は`WatchStatusTest`と`GetResumePositionUseCaseTest`で確かめてある。
    // ここでは、UseCaseの結果が`Player`に渡ることを、結果が変わる範囲ごとの代表値で確かめる
    context("保存した再生位置から、再生を始める位置を決める") {
        suspend fun startPositionMs(positions: FakePlaybackPositionRepository): Long? {
            val player = FakePlayer()
            runTest {
                viewModel(fakeVideos(), player, positions = positions).uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, emptyList())
                }
            }
            return player.startPositionMs
        }

        test("再生位置が保存されていなければ、最初から再生する") {
            startPositionMs(FakePlaybackPositionRepository()) shouldBe 0L
        }

        test("視聴途中なら、保存した再生位置から再生する") {
            val positions = FakePlaybackPositionRepository(
                mapOf(bigBuckBunny.id to PlaybackPosition(120.seconds)),
            )
            startPositionMs(positions) shouldBe 120_000L
        }

        test("視聴済みなら、最初から再生する") {
            val positions = FakePlaybackPositionRepository(
                mapOf(bigBuckBunny.id to PlaybackPosition(570.seconds)),
            )
            startPositionMs(positions) shouldBe 0L
        }

        test("再生位置の取得に失敗したら、エラーになり、MediaItemを設定せず、prepareも呼ばない") {
            val player = FakePlayer()
            val positions = FakePlaybackPositionRepository().apply {
                getPositionError = IOException()
            }
            runTest {
                viewModel(fakeVideos(), player, positions = positions).uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Error
                }
            }
            player.setMediaItemsList.shouldBeEmpty()
            player.prepareCount shouldBe 0
        }
    }

    test("ViewModelを破棄すると、Playerを解放する") {
        val player = FakePlayer()
        val store = ViewModelStore()
        storedViewModel(store) { viewModel(fakeVideos(), player) }
        player.releaseCount shouldBe 0

        store.clear()

        player.releaseCount shouldBe 1
    }

    context("再生位置の保存") {
        test("取得に成功したあとに破棄すると、その時点の位置を、解放より先に保存する") {
            val player = FakePlayer()
            val positions = FakePlaybackPositionRepository()
            val store = ViewModelStore()
            val viewModel =
                storedViewModel(store) { viewModel(fakeVideos(), player, positions = positions) }
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, emptyList())
                }
                player.currentPositionMs = 42_000

                store.clear()

                positions.getPosition(bigBuckBunny.id) shouldBe PlaybackPosition(42.seconds)
            }
            player.releaseCount shouldBe 1
        }

        test("読み込み中に破棄すると、保存しない") {
            val positions = FakePlaybackPositionRepository()
            val store = ViewModelStore()
            val viewModel =
                storedViewModel(store) { viewModel(fakeVideos(), positions = positions) }
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                }

                store.clear()

                positions.getPosition(bigBuckBunny.id).shouldBeNull()
            }
        }

        test("該当する動画がないときに破棄すると、保存しない") {
            val positions = FakePlaybackPositionRepository()
            val store = ViewModelStore()
            val viewModel = storedViewModel(store) {
                viewModel(fakeVideos(), videoId = "unknown", positions = positions)
            }
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.NotFound
                }

                store.clear()

                positions.getPosition(VideoId("unknown")).shouldBeNull()
            }
        }

        test("取得に失敗したときに破棄すると、保存しない") {
            val positions = FakePlaybackPositionRepository()
            val store = ViewModelStore()
            val viewModel = storedViewModel(store) {
                viewModel(fakeVideos(IOException()), positions = positions)
            }
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Error
                }

                store.clear()

                positions.getPosition(bigBuckBunny.id).shouldBeNull()
            }
        }

        test("取得に成功したあとに再生が止まると、その時点の位置を保存する") {
            val player = FakePlayer()
            val positions = FakePlaybackPositionRepository()
            val viewModel = viewModel(fakeVideos(), player, positions = positions)
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, emptyList())
                }
                player.changeIsPlaying(true)
                testScheduler.runCurrent()
                player.currentPositionMs = 7_000

                player.changeIsPlaying(false)
                testScheduler.runCurrent()

                positions.getPosition(bigBuckBunny.id) shouldBe PlaybackPosition(7.seconds)
            }
        }
    }

    // 「再生中」は、再生するつもりか（`playWhenReady`）で表す。バッファリング中も、戻ったときに再生を続けるため
    context("画面が見えなくなったら一時停止し、戻ったら、バックグラウンドに移る前に再生していたときだけ再生し直す") {
        // 取得に成功し、`startPlayback`で`playWhenReady`が`true`になるまで進める
        suspend fun loaded(viewModel: PlayerViewModel) {
            viewModel.uiState.test {
                awaitItem() shouldBe PlayerUiState.Loading
                awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, emptyList())
            }
        }

        test("再生中にバックグラウンドに移ると、一時停止する") {
            val player = FakePlayer()
            val viewModel = viewModel(fakeVideos(), player)
            runTest {
                loaded(viewModel)

                viewModel.onBackground()
            }
            player.playWhenReadyValue shouldBe false
        }

        test("再生中にバックグラウンドに移ってから戻ると、再生し直す") {
            val player = FakePlayer()
            val viewModel = viewModel(fakeVideos(), player)
            runTest {
                loaded(viewModel)

                viewModel.onBackground()
                viewModel.onForeground()
            }
            player.playWhenReadyValue shouldBe true
        }

        test("一時停止中にバックグラウンドに移ってから戻ると、一時停止のまま") {
            val player = FakePlayer()
            val viewModel = viewModel(fakeVideos(), player)
            runTest {
                loaded(viewModel)
                player.pause()

                viewModel.onBackground()
                viewModel.onForeground()
            }
            player.playWhenReadyValue shouldBe false
        }

        test("読み込み中にバックグラウンドに移ると、バックグラウンドで取得に成功しても、MediaItemを設定してprepareを呼ぶが、再生は始めない") {
            val player = FakePlayer()
            val viewModel = viewModel(fakeVideos(), player)
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    viewModel.onBackground()
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, emptyList())
                }
            }
            player.setMediaItemsList.size shouldBe 1
            player.prepareCount shouldBe 1
            player.playWhenReadyValue shouldBe false
        }

        test("読み込み中にバックグラウンドに移り、バックグラウンドで取得に成功したあとに戻ると、再生を始める") {
            val player = FakePlayer()
            val viewModel = viewModel(fakeVideos(), player)
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    viewModel.onBackground()
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, emptyList())
                }

                viewModel.onForeground()
            }
            player.playWhenReadyValue shouldBe true
        }

        test("戻って再生し直したあとに一時停止し、もう一度バックグラウンドに移ってから戻ると、一時停止のまま") {
            val player = FakePlayer()
            val viewModel = viewModel(fakeVideos(), player)
            runTest {
                loaded(viewModel)
                viewModel.onBackground()
                viewModel.onForeground()
                player.pause()

                viewModel.onBackground()
                viewModel.onForeground()
            }
            player.playWhenReadyValue shouldBe false
        }

        test("バックグラウンドに移っていないときに戻る知らせだけが届いても、一時停止中なら、一時停止のまま") {
            val player = FakePlayer()
            val viewModel = viewModel(fakeVideos(), player)
            runTest {
                loaded(viewModel)
                player.pause()

                viewModel.onForeground()
            }
            player.playWhenReadyValue shouldBe false
        }
    }

    context("関連動画への切り替え") {
        // 3本とも同じタグ（`animation`）を持つので、互いに関連動画になる
        val sintel = testVideo("sintel", 600.seconds)
        val spring = testVideo("spring", 600.seconds)

        fun fakeVideosWithRelated() = FakeVideoRepository(listOf(bigBuckBunny, sintel, spring))
            .apply { fetchDelay = 1.seconds }

        test("取得に成功したら、関連動画も成功の状態に入る") {
            runTest {
                viewModel(fakeVideosWithRelated()).uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, listOf(sintel, spring))
                }
            }
        }

        test("関連動画を選ぶと、同じPlayerに、その動画のMediaItemを保存した位置から設定し、prepareを呼ぶ") {
            val player = FakePlayer()
            val positions =
                FakePlaybackPositionRepository(mapOf(sintel.id to PlaybackPosition(300.seconds)))
            val viewModel = viewModel(fakeVideosWithRelated(), player, positions = positions)
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, listOf(sintel, spring))

                    viewModel.selectVideo(sintel.id)

                    awaitItem() shouldBe PlayerUiState.Success(sintel, listOf(bigBuckBunny, spring))
                }
            }
            player.setMediaItemsList.map { it.mediaId } shouldBe listOf("big-buck-bunny", "sintel")
            player.startPositionMs shouldBe 300_000
            player.prepareCount shouldBe 2
        }

        // 取得に1秒かかるあいだに読み込み中を出していれば、選んだ直後に受け取る値が読み込み中になる
        test("関連動画を選ぶと、読み込み中を挟まずに、選んだ動画の成功の状態になる") {
            val viewModel = viewModel(fakeVideosWithRelated())
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, listOf(sintel, spring))

                    viewModel.selectVideo(sintel.id)

                    awaitItem() shouldBe PlayerUiState.Success(sintel, listOf(bigBuckBunny, spring))
                }
            }
        }

        test("続けて2本選ぶと、あとに選んだ動画だけを設定する") {
            val player = FakePlayer()
            val viewModel = viewModel(fakeVideosWithRelated(), player)
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, listOf(sintel, spring))

                    viewModel.selectVideo(sintel.id)
                    viewModel.selectVideo(spring.id)

                    awaitItem() shouldBe PlayerUiState.Success(spring, listOf(bigBuckBunny, sintel))
                }
            }
            player.setMediaItemsList.map { it.mediaId } shouldBe listOf("big-buck-bunny", "spring")
        }

        // `FakePlayer`は、本物と同じく`MediaItem`を替えると再生位置を始める位置（ここでは0）に替える
        test("選ぶと、MediaItemを替える前に、前の動画の位置を前の動画のIDで保存する") {
            val player = FakePlayer()
            val positions = FakePlaybackPositionRepository()
            val viewModel = viewModel(fakeVideosWithRelated(), player, positions = positions)
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, listOf(sintel, spring))
                    player.currentPositionMs = 42_000

                    viewModel.selectVideo(sintel.id)

                    awaitItem() shouldBe PlayerUiState.Success(sintel, listOf(bigBuckBunny, spring))
                }
            }
            positions.getPosition(bigBuckBunny.id) shouldBe PlaybackPosition(42.seconds)
        }

        test("切り替えたあとに再生が止まると、新しい動画のIDで保存し、前の動画の位置は変えない") {
            val player = FakePlayer()
            val positions = FakePlaybackPositionRepository()
            val viewModel = viewModel(fakeVideosWithRelated(), player, positions = positions)
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, listOf(sintel, spring))
                    player.currentPositionMs = 42_000
                    viewModel.selectVideo(sintel.id)
                    awaitItem() shouldBe PlayerUiState.Success(sintel, listOf(bigBuckBunny, spring))
                }
                player.changeIsPlaying(true)
                testScheduler.runCurrent()
                player.currentPositionMs = 7_000

                player.changeIsPlaying(false)
                testScheduler.runCurrent()

                positions.getPosition(sintel.id) shouldBe PlaybackPosition(7.seconds)
                positions.getPosition(bigBuckBunny.id) shouldBe PlaybackPosition(42.seconds)
            }
        }

        test("切り替えの取得に失敗したら、MediaItemを替えずに一時停止し、エラーになる") {
            val player = FakePlayer()
            val videos = fakeVideosWithRelated()
            val viewModel = viewModel(videos, player)
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, listOf(sintel, spring))
                    videos.error = IOException()

                    viewModel.selectVideo(sintel.id)

                    awaitItem() shouldBe PlayerUiState.Error
                }
            }
            player.setMediaItemsList.map { it.mediaId } shouldBe listOf("big-buck-bunny")
            player.playWhenReadyValue shouldBe false
        }

        test("切り替えに失敗したあとに再読み込みすると、選んだ動画を読み込む") {
            val videos = fakeVideosWithRelated()
            val viewModel = viewModel(videos)
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, listOf(sintel, spring))
                    videos.error = IOException()
                    viewModel.selectVideo(sintel.id)
                    awaitItem() shouldBe PlayerUiState.Error
                    videos.error = null

                    viewModel.reload()

                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(sintel, listOf(bigBuckBunny, spring))
                }
            }
        }

        test("選んだ動画のIDを、SavedStateHandleに保存する") {
            val savedStateHandle = SavedStateHandle()
            val viewModel = viewModel(fakeVideosWithRelated(), savedStateHandle = savedStateHandle)
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(bigBuckBunny, listOf(sintel, spring))

                    viewModel.selectVideo(sintel.id)

                    awaitItem() shouldBe PlayerUiState.Success(sintel, listOf(bigBuckBunny, spring))
                }
            }
            savedStateHandle.get<String>(PlayerViewModel.CURRENT_VIDEO_ID_KEY) shouldBe "sintel"
        }

        // プロセスが終了されたあと、`PlayerKey`は最初の動画のIDのまま、`SavedStateHandle`には選んだ動画のIDが戻る
        test("SavedStateHandleに動画のIDがあれば、PlayerKeyのIDではなく、そのIDの動画で始める") {
            val player = FakePlayer()
            val savedStateHandle =
                SavedStateHandle(mapOf(PlayerViewModel.CURRENT_VIDEO_ID_KEY to "sintel"))
            val viewModel = viewModel(
                fakeVideosWithRelated(),
                player,
                videoId = bigBuckBunny.id.value,
                savedStateHandle = savedStateHandle,
            )
            runTest {
                viewModel.uiState.test {
                    awaitItem() shouldBe PlayerUiState.Loading
                    awaitItem() shouldBe PlayerUiState.Success(sintel, listOf(bigBuckBunny, spring))
                }
            }
            player.setMediaItemsList.map { it.mediaId } shouldBe listOf("sintel")
        }
    }
})
