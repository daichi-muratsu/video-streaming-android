package io.github.daichimuratsu.videostreaming.feature.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
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
        onReload = viewModel::reload,
        modifier = modifier,
    )
}

/**
 * 再生画面の状態と`Player`を受け取って表示する。上にプレイヤー、下にタイトルと説明の箱を置く。
 *
 * 読み込み中・エラー・該当なしは、プレイヤーの枠の中に出す。枠の位置を変えずに中身だけを替え、読み込みが終わったときに画面が跳ねないようにするため
 */
// `ContentFrame`はMedia3の`@UnstableApi`（今後のバージョンで変わるかもしれないAPI）。
// Media3でComposeから映像を表示する部品（`PlayerSurface`・`ContentFrame`）は、どちらも`@UnstableApi`のため、
// Media3を上げてAPIが変わったときに直す
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    player: Player,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PlayerFrame {
            when (uiState) {
                PlayerUiState.Loading -> PlayerLoading()

                is PlayerUiState.Success -> {
                    // 映像の縦横比を保って枠に収める。最初の映像が出るまでは、黒い覆いをかぶせる
                    ContentFrame(player = player, modifier = Modifier.fillMaxSize())
                    PlayerControlsOverlay(player = player)
                }

                PlayerUiState.NotFound -> PlayerNotFound()

                PlayerUiState.Error -> PlayerError(onReload = onReload)
            }
        }
        // 説明の箱は、動画の情報を取得できてから出す
        if (uiState is PlayerUiState.Success) {
            VideoDetails(
                video = uiState.video,
                modifier = Modifier
                    .fillMaxWidth()
                    // プレイヤーは上に固定し、説明だけをスクロールする。説明を読んでいるあいだも映像が見えるようにするため
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
            )
        }
    }
}

/**
 * プレイヤーの黒い16:9の枠。映像・操作ボタン・読み込み中などを、この中に重ねる。
 *
 * edge-to-edgeなので、ステータスバーの分の余白を枠の上に取り、余白も黒くする
 */
@Composable
internal fun PlayerFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .statusBarsPadding()
            .aspectRatio(16f / 9f),
        content = content,
    )
}
