package io.github.daichimuratsu.videostreaming.feature.videolist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.daichimuratsu.videostreaming.core.domain.usecase.GetVideoListWithWatchStatusUseCase
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * 一覧画面の状態を作るViewModel。
 *
 * 視聴状態の判定はUseCase（`WatchStatus.of`）で済んでいるので、ここでは判定せず、取得の結果を`VideoListUiState`に変換し、再読み込みを受け付ける
 */
@HiltViewModel
class VideoListViewModel @Inject constructor(
    private val getVideoListWithWatchStatus: GetVideoListWithWatchStatusUseCase,
) : ViewModel() {
    // 再読み込みの合図。中身は要らないので`Unit`にする。
    // 受け取る側がいないときの合図は捨てる（画面がないなら、読み込む必要がない）
    private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * 画面に表示する状態。
     *
     * 画面が受け取っていないあいだ（再生画面を開いているあいだなど）は、5秒後に再生位置の受け取りを止める。
     * 5秒待つのは、画面の回転で受け取りが一瞬途切れたときに、取得し直さないため
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<VideoListUiState> = reloadRequests
        .map { true }
        // 受け取り始めたときに1回読み込む。ViewModelの`init`で読み込み始めると、作りかけのViewModelを読み書きするおそれがあるため
        .onStart { emit(false) }
        // 合図のたびに、前の`Flow`を止めて、UseCaseの`Flow`を作り直す。失敗で終わった`Flow`は、もう値を出力しないため
        .flatMapLatest { isReload -> videoListStates(isReload) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VideoListUiState.Loading,
        )

    /** 一覧を読み込み直す。エラーのときに、画面の「再読み込み」ボタンから呼ぶ。 */
    fun reload() {
        reloadRequests.tryEmit(Unit)
    }

    private fun videoListStates(isReload: Boolean): Flow<VideoListUiState> =
        getVideoListWithWatchStatus()
            .map { videos ->
                if (videos.isEmpty()) VideoListUiState.Empty else VideoListUiState.Success(videos)
            }
            // 再読み込みのときだけ`Loading`にする。再生画面から5秒より後に戻ったときにも作り直すが、
            // そのときは表示中の一覧を残し、読み込み中の表示がちらつかないようにする
            .onStart { if (isReload) emit(VideoListUiState.Loading) }
            // `flatMapLatest`の内側で受け止める。外側で受け止めると、合図を受け取る`Flow`ごと終わり、再読み込みが効かなくなるため。
            // `catch`は`Flow`を止めるための例外（`CancellationException`）を受け止めない
            .catch { emit(VideoListUiState.Error) }
}
