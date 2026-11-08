package io.github.daichimuratsu.videostreaming.feature.videolist

import io.github.daichimuratsu.videostreaming.core.domain.model.VideoWithWatchStatus

/**
 * 一覧画面に表示する内容を表す状態。
 *
 * 読み込み中・成功・空・エラーは同時に起きないので、型を分けて、「読み込み中かつエラー」のような組み合わせを作れないようにする
 */
sealed interface VideoListUiState {
    /** 一覧を読み込んでいる。 */
    data object Loading : VideoListUiState

    /** 一覧の取得に成功し、1本以上の動画がある。 */
    data class Success(
        val videos: List<VideoWithWatchStatus>,
    ) : VideoListUiState {
        init {
            // 空の一覧は`Empty`で表す。画面が`Success`の中で空かどうかを確かめずに済むように、作った時点で止める
            require(videos.isNotEmpty()) { "Successの一覧が空になっている" }
        }
    }

    /** 一覧の取得に成功したが、動画が1本もない。 */
    data object Empty : VideoListUiState

    /**
     * 一覧の取得に失敗した。
     *
     * 例外の種類を問わず、利用者にできるのは再読み込みだけなので、種類を持たない。表示する文言は画面で決める
     */
    data object Error : VideoListUiState
}
