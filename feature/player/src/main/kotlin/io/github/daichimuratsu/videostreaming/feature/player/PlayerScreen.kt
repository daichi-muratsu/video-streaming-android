package io.github.daichimuratsu.videostreaming.feature.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.provider.Settings
import android.view.OrientationEventListener
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.state.rememberErrorState
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
    val activity = LocalActivity.current
    // 回転で`Activity`を作り直すときも`ON_STOP`が届くので、そのときは止めない。止めると、回転のたびに再生が途切れるため。
    // `ON_PAUSE`では止めない。画面分割で隣のアプリを操作しただけで止まるため
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (activity?.isChangingConfigurations != true) viewModel.onBackground()
    }
    // 画面を開いたときにも届くが、バックグラウンドに移っていなければ、ViewModelは何もしない
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onForeground() }
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
    // 横向きなら全画面にする。全画面でない横向きの画面を作らずに済ませるため。
    // 向きから決めるので、端末を倒して横にしたときも、全画面のボタンで横に固定したときも、同じように全画面になる
    val isFullscreen = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val requestFullscreen = rememberFullscreenRequest()
    FullscreenSystemBarsEffect(isFullscreen = isFullscreen)
    // 全画面中の「戻る」は、一覧に戻らず全画面を終える。全画面を終えるつもりの操作で一覧まで戻ると、操作と結果が食い違うため
    BackHandler(enabled = isFullscreen) { requestFullscreen(false) }

    Column(modifier = modifier.fillMaxSize()) {
        PlayerFrame(isFullscreen = isFullscreen) {
            when (uiState) {
                PlayerUiState.Loading -> PlayerLoading()

                is PlayerUiState.Success -> {
                    // 映像の縦横比を保って枠に収める。最初の映像が出るまでは、黒い覆いをかぶせる
                    ContentFrame(player = player, modifier = Modifier.fillMaxSize())
                    KeepScreenOnWhilePlaying(player = player)
                    // 再生のエラーは`UiState`に入れず、`Player`から受け取る。`prepare()`でやり直すと`Player`がエラーを消すので、
                    // ほかに持つと表示が残るおそれがあるため
                    val playbackError = rememberErrorState(player).error
                    // 横向きの全画面では、ディスプレイカットアウト（カメラのための切り欠き）に操作ボタンが重ならないようにする
                    val cutoutPadding = Modifier.windowInsetsPadding(WindowInsets.displayCutout)
                    if (playbackError == null) {
                        PlayerControlsOverlay(
                            player = player,
                            isFullscreen = isFullscreen,
                            onToggleFullscreen = { requestFullscreen(!isFullscreen) },
                            modifier = cutoutPadding,
                        )
                    } else {
                        PlayerPlaybackError(
                            kind = PlaybackErrorKind.of(playbackError.errorCode),
                            // `prepare()`でエラーが起きた位置から読み込み直す。一時停止中にエラーになったときも再生を始めるため、
                            // `play()`も呼ぶ（Media3の再生ボタンの処理と同じ組み合わせ）
                            onRetry = {
                                player.prepare()
                                player.play()
                            },
                            modifier = cutoutPadding,
                        )
                    }
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
 * 全画面にするか終えるかを受け取り、画面の向きを固定する関数を返す。
 *
 * 全画面かどうかは向きから決まるので、向きを変えれば全画面も切り替わる。
 * 固定は、端末の傾きが固定した向きに追いついたら外す。外したあとは、端末を倒せば全画面が切り替わる。
 * すぐに外すと、端末を横に持ったまま全画面を終えたときに、横向きに戻ってまた全画面になるため。
 * 自動回転がオフのときは外さない。外すと、システムの向き（縦）にすぐ戻るため。
 * 短い辺が600dp以上の端末では、`targetSdk`が36以上だと向きの固定が無視される（Android 16の動作の変更）ので、
 * 全画面のボタンを押しても向きは変わらない
 */
// 全画面を終えたときの縦向きの固定を、Lintの`SourceLockedOrientationActivity`（向きを固定すべきでない）から外す。
// 全画面のボタンを押したときだけ固定し、端末の傾きが追いつくか、再生画面を離れると外す
@SuppressLint("SourceLockedOrientationActivity")
@Composable
private fun rememberFullscreenRequest(): (Boolean) -> Unit {
    val activity = LocalActivity.current
    // 回転で`Activity`を作り直しても、固定は新しい`Activity`に引き継がれるので、固定中かどうかも保存する
    var lock by rememberSaveable { mutableStateOf<OrientationLock?>(null) }
    // 再生画面を離れたら、向きの固定を外す。一覧画面は横向きでも表示できるため
    DisposableEffect(activity) {
        onDispose {
            if (activity != null && !activity.isChangingConfigurations) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
    // 固定しているあいだだけ、端末の傾きを受け取る。センサーを使い続けないため
    DisposableEffect(activity, lock) {
        val currentLock = lock
        if (activity == null || currentLock == null) return@DisposableEffect onDispose {}
        val listener = object : OrientationEventListener(activity) {
            override fun onOrientationChanged(orientation: Int) {
                if (currentLock.isReachedBy(orientation) && activity.isAutoRotateEnabled()) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    lock = null
                }
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }
    return remember(activity) {
        { fullscreen ->
            if (activity != null) {
                activity.requestedOrientation = if (fullscreen) {
                    // 左右どちらの横向きでもよい。端末をどちら向きに倒しても見られるようにするため
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                }
                lock = if (fullscreen) OrientationLock.Landscape else OrientationLock.Portrait
            }
        }
    }
}

// 端末の設定で、自動回転がオンかどうか。読み取りに権限は要らない
private fun Context.isAutoRotateEnabled(): Boolean =
    Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1

/** 全画面のあいだ、ステータスバーとナビゲーションバーを隠す。 */
@Composable
private fun FullscreenSystemBarsEffect(isFullscreen: Boolean) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity, isFullscreen) {
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            // 隠したシステムバーは、画面の端からスワイプしたときだけ一時的に出す
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            // 再生画面を離れたときや、縦向きに戻ったときに出し直す。回転で`Activity`を作り直すときは、新しい`Activity`が決め直す
            if (isFullscreen && !activity.isChangingConfigurations) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

/**
 * 再生中だけ、画面が消えないようにする。
 *
 * 一時停止したまま端末を置いたときは、通常どおり画面を消す。再生中かどうかは`UiState`に入れず、`Player`から受け取る
 */
@Composable
private fun KeepScreenOnWhilePlaying(player: Player) {
    var keepsScreenOn by remember(player) { mutableStateOf(player.isPlaying) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                keepsScreenOn = isPlaying
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    // 大きさのない部品に付ける。この部品が表示されているあいだだけ、画面が点いたままになる
    if (keepsScreenOn) Box(modifier = Modifier.keepScreenOn())
}
