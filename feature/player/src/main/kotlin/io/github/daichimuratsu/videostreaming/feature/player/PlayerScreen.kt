package io.github.daichimuratsu.videostreaming.feature.player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
 * 読み込み中・エラー・該当なしは、プレイヤーの枠の中に出す。枠の位置を変えずに中身だけを替え、読み込みが終わったときに画面が跳ねないようにするため。
 * 全画面では、説明の箱を隠し、プレイヤーを画面いっぱいに広げる
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
    // 画面の見た目だけの状態なので、ViewModelに持たず画面で持つ。回転のあとも全画面のままにする
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    FullscreenEffect(isFullscreen = isFullscreen)
    // 全画面中の「戻る」は、一覧に戻らず全画面を終える。全画面を終えるつもりの操作で一覧まで戻ると、操作と結果が食い違うため
    BackHandler(enabled = isFullscreen) { isFullscreen = false }

    Column(modifier = modifier.fillMaxSize()) {
        PlayerFrame(isFullscreen = isFullscreen) {
            when (uiState) {
                PlayerUiState.Loading -> PlayerLoading()

                is PlayerUiState.Success -> {
                    // 映像の縦横比を保って枠に収める。最初の映像が出るまでは、黒い覆いをかぶせる
                    ContentFrame(player = player, modifier = Modifier.fillMaxSize())
                    PlayerControlsOverlay(
                        player = player,
                        isFullscreen = isFullscreen,
                        onToggleFullscreen = { isFullscreen = !isFullscreen },
                        // 横向きの全画面では、ディスプレイカットアウト（カメラのための切り欠き）に操作ボタンが重ならないようにする
                        modifier = Modifier.windowInsetsPadding(WindowInsets.displayCutout),
                    )
                }

                PlayerUiState.NotFound -> PlayerNotFound()

                PlayerUiState.Error -> PlayerError(onReload = onReload)
            }
        }
        // 説明の箱は、動画の情報を取得できてから出す。全画面では隠す
        if (uiState is PlayerUiState.Success && !isFullscreen) {
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
 * プレイヤーの黒い枠。映像・操作ボタン・読み込み中などを、この中に重ねる。
 *
 * 縦向きでは16:9の枠にし、edge-to-edgeなので、ステータスバーの分の余白を枠の上に取って余白も黒くする。
 * 全画面では画面いっぱいに広げる。端末の画面の縦横比が16:9でなくても、`ContentFrame`が映像の縦横比を保つ
 */
@Composable
internal fun PlayerFrame(
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .background(Color.Black)
            .then(
                if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .aspectRatio(16f / 9f)
                },
            ),
        content = content,
    )
}

/**
 * 全画面のときに、横向きを求め、システムバーを隠す。全画面を終えたら、縦向きに固定してシステムバーを戻す。
 *
 * 短い辺が600dp以上の端末では、`targetSdk`が36以上だと向きの固定が無視される（Android 16の動作の変更）。
 * その場合も、`PlayerFrame`が今の向きのまま画面いっぱいに広がる
 */
// 全画面を終えたあとの縦向きの固定を、Lintの`SourceLockedOrientationActivity`（向きを固定すべきでない）から外す。
// 全画面でない横向きの画面を作るまでの一時的な固定で、再生画面を離れると外す。
// 短い辺が600dp以上の端末では、固定はもともと無視される。回転で全画面にする動きを作るときに、固定をやめられるかを見直す
@SuppressLint("SourceLockedOrientationActivity")
@Composable
private fun FullscreenEffect(isFullscreen: Boolean) {
    val activity = LocalActivity.current ?: return
    // 再生画面を離れたら、向きの固定を外す。一覧画面は横向きでも表示できるため
    DisposableEffect(activity) {
        onDispose {
            if (!activity.isChangingConfigurations) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
    DisposableEffect(activity, isFullscreen) {
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            // 左右どちらの横向きでもよい。端末をどちら向きに倒しても見られるようにするため
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            // 隠したシステムバーは、画面の端からスワイプしたときだけ一時的に出す
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            // 回転で`Activity`が作り直されるときは戻さない。戻すと、縦に戻してから横に固定し直すことを繰り返すおそれがあるため
            if (isFullscreen && !activity.isChangingConfigurations) {
                // 固定を外すだけだと、端末を横に持ったまま（または平らに置いたまま）終えると横向きのままになる。
                // 全画面でない横向きの画面は作っていないので、縦向きに固定して縦の画面に戻す
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
