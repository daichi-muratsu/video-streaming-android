package io.github.daichimuratsu.videostreaming.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.repository.PlaybackPositionRepository
import io.github.daichimuratsu.videostreaming.core.domain.repository.VideoRepository
import io.github.daichimuratsu.videostreaming.core.domain.usecase.GetRelatedVideosUseCase
import io.github.daichimuratsu.videostreaming.core.domain.usecase.GetResumePositionUseCase
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
import kotlinx.coroutines.flow.merge
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
    private val savedStateHandle: SavedStateHandle,
    private val videoRepository: VideoRepository,
    private val getResumePosition: GetResumePositionUseCase,
    private val getRelatedVideos: GetRelatedVideosUseCase,
    private val playbackPositionRepository: PlaybackPositionRepository,
    private val mediaItemFactory: MediaItemFactory,
    // 画面を離れたあとも、再生位置の最後の保存を終えるためのスコープ。`viewModelScope`は`onCleared`で止まる
    @ApplicationScope private val applicationScope: CoroutineScope,
    /** 画面が映像を表示し、操作ボタンから操作する`Player`。 */
    val player: Player,
) : ViewModel() {
    // 画面に表示している動画のID。関連動画を選ぶと替わる。
    // バックスタックに残るのは最初の`PlayerKey`なので、プロセスが終了されたあとも替えたあとの動画に戻せるように、`SavedStateHandle`に保存する。
    // 保存したIDがなければ（画面を開いたとき）、`PlayerKey`のIDを使う
    private val currentVideoId: StateFlow<String> =
        savedStateHandle.getStateFlow(CURRENT_VIDEO_ID_KEY, videoId)

    private val playbackProgress = PlayerPlaybackProgress(player)

    // 再生中の動画の再生位置を保存するTracker。Trackerは作るときのIDで保存するので、動画を替えるたびに作り直す。
    // `MediaItem`を設定する前は`null`で、`onCleared`で保存するかの判定に使う
    private var positionTracker: PlaybackPositionTracker? = null
    private var trackingJob: Job? = null

    // 画面が見えていないあいだは`true`。読み込みがバックグラウンドで終わったときに、再生を始めないために使う
    private var isInBackground = false

    // バックグラウンドに移る前に再生していた（`playWhenReady`が`true`だった）か。画面に戻ったときに、再生し直すかの判定に使う
    private var resumesOnForeground = false

    // 再読み込みの合図。中身は要らないので`Unit`にする
    private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * 画面に表示する状態。
     *
     * 動画の情報は1回取得すれば変わらないので、画面が受け取りをやめても止めない（`Lazily`）。
     * `WhileSubscribed`で止めると、画面に戻ったときに取得し直し、`MediaItem`を設定し直して再生が最初に戻るため
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PlayerUiState> = merge(
        // 受け取り始めたときと、関連動画を選んだときに読み込む。
        // `StateFlow`は受け取り始めたときに今の値を出力するので、ViewModelの`init`で読み込み始めずに済む（作りかけのViewModelを読み書きしないため）
        currentVideoId.map { id -> LoadRequest(id, isReload = false) },
        reloadRequests.map { LoadRequest(currentVideoId.value, isReload = true) },
    )
        // 続けて選んだときは、前の読み込みを止め、最後に選んだ動画だけを再生する
        .flatMapLatest { request -> playerStates(VideoId(request.videoId), request.isReload) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = PlayerUiState.Loading,
        )

    /** 動画の情報を読み込み直す。エラーのときに、画面の「再読み込み」ボタンから呼ぶ。 */
    fun reload() {
        reloadRequests.tryEmit(Unit)
    }

    /**
     * 再生する動画を替える。画面の関連動画から呼ぶ。
     *
     * 画面遷移にせず、同じ`Player`のまま替える。替える動画の情報を取得し終えるまでは、前の動画の再生と表示を続ける
     */
    fun selectVideo(id: VideoId) {
        savedStateHandle[CURRENT_VIDEO_ID_KEY] = id.value
    }

    /**
     * 画面が見えなくなったときに、再生を一時停止する。画面の`ON_STOP`から呼ぶ。
     *
     * バックグラウンドで音を出し続けないため。止めると再生位置の保存も動くので、プロセスが終了されても、バックグラウンドに移った位置から再開できる
     */
    fun onBackground() {
        isInBackground = true
        // 再生中かどうか（`isPlaying`）ではなく、再生するつもりか（`playWhenReady`）で判定する。
        // バッファリング中は`isPlaying`が`false`になるが、戻ったときには再生を続けるべきため
        resumesOnForeground = player.playWhenReady
        player.pause()
    }

    /** 画面がまた見えるようになったときに、バックグラウンドに移る前に再生していれば、再生し直す。画面の`ON_START`から呼ぶ。 */
    fun onForeground() {
        isInBackground = false
        if (resumesOnForeground) player.play()
        resumesOnForeground = false
    }

    private fun playerStates(videoId: VideoId, isReload: Boolean): Flow<PlayerUiState> =
        flow { emit(videoRepository.getVideo(videoId)) }
            .map { video ->
                if (video == null) {
                    PlayerUiState.NotFound
                } else {
                    val relatedVideos = getRelatedVideos(video)
                    // 始める位置の取得に失敗したときは、`MediaItem`を設定せずにエラーにする。
                    // 0から再生すると、次の保存で、読み出せなかった再生位置を上書きするため
                    startPlayback(video, getResumePosition(video))
                    PlayerUiState.Success(video, relatedVideos)
                }
            }
            .onStart { if (isReload) emit(PlayerUiState.Loading) }
            // `flatMapLatest`の内側で受け止める。外側で受け止めると、合図を受け取る`Flow`ごと終わり、再読み込みが効かなくなるため。
            // `catch`は`Flow`を止めるための例外（`CancellationException`）を受け止めない
            .catch {
                // 関連動画への切り替えに失敗したときに、エラーの表示の裏で前の動画を再生し続けないため
                player.pause()
                emit(PlayerUiState.Error)
            }

    // 一覧で動画を選ぶ操作を、再生を始める指示として扱う。再生画面で再生ボタンを押す操作を増やさないため
    private fun startPlayback(video: Video, startPosition: PlaybackPosition) {
        // 前の動画の最後の位置は、`MediaItem`を替える前に、前の動画のIDで保存する。
        // 替えたあとに保存すると、`Player`の位置が新しい動画の始める位置になっていて、それを前の動画の位置として保存するため
        stopTracking()
        // `Player`との境界なので、ここでミリ秒に変換する
        player.setMediaItem(
            mediaItemFactory.create(video),
            startPosition.value.inWholeMilliseconds,
        )
        player.prepare()
        // 読み込みがバックグラウンドで終わったときは再生を始めず、画面に戻ったときに始める。バックグラウンドで音を出さないため
        if (isInBackground) {
            resumesOnForeground = true
        } else {
            player.playWhenReady = true
        }
        startTracking(video.id)
    }

    private fun startTracking(videoId: VideoId) {
        val tracker = PlaybackPositionTracker(
            videoId = videoId,
            progress = playbackProgress,
            repository = playbackPositionRepository,
            saveScope = applicationScope,
        )
        positionTracker = tracker
        trackingJob = viewModelScope.launch { tracker.track() }
    }

    private fun stopTracking() {
        trackingJob?.cancel()
        positionTracker?.save()
    }

    override fun onCleared() {
        // `MediaItem`を設定する前に離れたときは（Trackerがないので）保存しない。保存すると、前に保存した位置を0で上書きするため。
        // 位置は`Player`から読むので、解放より先に保存する
        positionTracker?.save()
        player.release()
    }

    // 読み込みの合図。関連動画を選んだときは読み込み中を挟まず、再読み込みのときだけ挟む
    private data class LoadRequest(val videoId: String, val isReload: Boolean)

    @AssistedFactory
    interface Factory {
        fun create(videoId: String): PlayerViewModel
    }

    internal companion object {
        // テストで、プロセスが終了されたあとの`SavedStateHandle`を作るときにも使う
        const val CURRENT_VIDEO_ID_KEY = "currentVideoId"
    }
}
