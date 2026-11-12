package io.github.daichimuratsu.videostreaming.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.repository.PlaybackPositionRepository
import io.github.daichimuratsu.videostreaming.core.domain.repository.VideoRepository
import io.github.daichimuratsu.videostreaming.feature.player.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 再生画面の状態を作り、`Player`を持つViewModel。
 *
 * `Player`はHiltのModuleで作ったものを受け取り、ExoPlayerの型と`Context`に依存しない（後で`MediaController`に差し替えるため）。
 * 画面の回転のあとも同じ`Player`で再生を続けるため、`Player`は画面ではなくViewModelが持ち、破棄されるときに解放する
 */
@HiltViewModel(assistedFactory = PlayerViewModel.Factory::class)
class PlayerViewModel @AssistedInject constructor(
    // `VideoId`ではなく`String`で受け取る。`value class`を引数に持つ関数は、Kotlinが名前を書き換える（`create-MEJnCn0`など）ため、
    // Daggerが`Factory`の実装を作れない
    @Assisted videoId: String,
    private val videoRepository: VideoRepository,
    playbackPositionRepository: PlaybackPositionRepository,
    private val mediaItemFactory: MediaItemFactory,
    // 画面を離れたあとも、再生位置の最後の保存を終えるためのスコープ。`viewModelScope`は`onCleared`で止まる
    @ApplicationScope applicationScope: CoroutineScope,
    /** 画面が映像を表示し、操作ボタンから操作する`Player`。 */
    val player: Player,
) : ViewModel() {
    private val videoId = VideoId(videoId)

    private val positionTracker = PlaybackPositionTracker(
        videoId = this.videoId,
        progress = PlayerPlaybackProgress(player),
        repository = playbackPositionRepository,
        saveScope = applicationScope,
    )

    // 再生位置の監視。`MediaItem`を設定する前は`null`で、`onCleared`で保存するかの判定に使う
    private var trackingJob: Job? = null

    // 再読み込みの合図。中身は要らないので`Unit`にする
    private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * 画面に表示する状態。
     *
     * 動画の情報は1回取得すれば変わらないので、画面が受け取りをやめても止めない（`Lazily`）。
     * `WhileSubscribed`で止めると、画面に戻ったときに取得し直し、`MediaItem`を設定し直して再生が最初に戻るため
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PlayerUiState> = reloadRequests
        .map { true }
        // 受け取り始めたときに1回読み込む。ViewModelの`init`で読み込み始めると、作りかけのViewModelを読み書きするおそれがあるため
        .onStart { emit(false) }
        .flatMapLatest { isReload -> playerStates(isReload) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = PlayerUiState.Loading,
        )

    /** 動画の情報を読み込み直す。エラーのときに、画面の「再読み込み」ボタンから呼ぶ。 */
    fun reload() {
        reloadRequests.tryEmit(Unit)
    }

    private fun playerStates(isReload: Boolean): Flow<PlayerUiState> =
        flow { emit(videoRepository.getVideo(videoId)) }
            .map { video ->
                if (video == null) PlayerUiState.NotFound else PlayerUiState.Success(video)
            }
            .onEach { state -> if (state is PlayerUiState.Success) startPlayback(state.video) }
            .onStart { if (isReload) emit(PlayerUiState.Loading) }
            // `flatMapLatest`の内側で受け止める。外側で受け止めると、合図を受け取る`Flow`ごと終わり、再読み込みが効かなくなるため。
            // `catch`は`Flow`を止めるための例外（`CancellationException`）を受け止めない
            .catch { emit(PlayerUiState.Error) }

    // 一覧で動画を選ぶ操作を、再生を始める指示として扱う。再生画面で再生ボタンを押す操作を増やさないため
    private fun startPlayback(video: Video) {
        player.setMediaItem(mediaItemFactory.create(video))
        player.prepare()
        player.playWhenReady = true
        startTracking()
    }

    private fun startTracking() {
        if (trackingJob != null) return
        trackingJob = viewModelScope.launch { positionTracker.track() }
    }

    override fun onCleared() {
        // `MediaItem`を設定する前に離れたときは保存しない。保存すると、前に保存した位置を0で上書きするため。
        // 位置は`Player`から読むので、解放より先に保存する
        if (trackingJob != null) positionTracker.save()
        player.release()
    }

    @AssistedFactory
    interface Factory {
        fun create(videoId: String): PlayerViewModel
    }
}
