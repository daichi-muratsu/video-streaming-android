package io.github.daichimuratsu.videostreaming.feature.player

import io.github.daichimuratsu.videostreaming.core.domain.model.Video

/**
 * 再生画面の状態。動画の情報（タイトル・説明・クレジット・HLSのURL）の取得の結果を表す。
 *
 * 再生中か・再生位置などの再生の状態は持たない。`Player`が持っていて、画面がMedia3の状態ホルダーで受け取るため、
 * ここにも持つと`Player`と食い違うおそれがある
 */
sealed interface PlayerUiState {
    data object Loading : PlayerUiState

    data class Success(val video: Video) : PlayerUiState

    /** 指定したIDの動画がない。再読み込みしても変わらないので、`Error`と分ける */
    data object NotFound : PlayerUiState

    data object Error : PlayerUiState
}
