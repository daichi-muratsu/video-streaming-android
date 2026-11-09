package io.github.daichimuratsu.videostreaming.feature.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId

/**
 * 再生画面をViewModelとつなぐ。
 *
 * 状態の受け取りとViewModelの用意はここで行い、表示は`PlayerScreen`に任せる。表示だけをプレビューやテストで確かめられるようにするため
 */
@Composable
fun PlayerRoute(
    videoId: VideoId,
    modifier: Modifier = Modifier,
    // 動画のIDはassisted injectionで渡す。ViewModelは画面（`NavEntry`）ごとに作られ、画面をバックスタックから外したときに破棄される
    viewModel: PlayerViewModel = hiltViewModel<PlayerViewModel, PlayerViewModel.Factory>(
        creationCallback = { factory -> factory.create(videoId.value) },
    ),
) {
    // 受け取り始めると、ViewModelが動画の情報を取得して再生を始める（`Lazily`は、受け取る側が現れるまで始めない）。
    // 画面が見えないあいだは受け取りを止める
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlayerScreen(
        uiState = uiState,
        player = viewModel.player,
        modifier = modifier,
    )
}

/**
 * 再生画面の状態と`Player`を受け取って表示する。
 *
 * 今は映像だけを16:9の枠に表示する
 */
// TODO: 操作ボタン・説明欄・読み込み中とエラーの表示を作る
// `PlayerSurface`はMedia3の`@UnstableApi`（今後のバージョンで変わるかもしれないAPI）。
// Media3でComposeから映像を表示する部品（`PlayerSurface`・`ContentFrame`）は、どちらも`@UnstableApi`のため、
// Media3を上げてAPIが変わったときに直す
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(uiState: PlayerUiState, player: Player, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .statusBarsPadding()
                .aspectRatio(16f / 9f),
        ) {
            PlayerSurface(player = player, modifier = Modifier.fillMaxSize())
        }
    }
}
