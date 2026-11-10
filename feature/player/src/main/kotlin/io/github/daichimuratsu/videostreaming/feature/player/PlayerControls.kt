package io.github.daichimuratsu.videostreaming.feature.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import io.github.daichimuratsu.videostreaming.core.ui.durationDescription
import io.github.daichimuratsu.videostreaming.core.ui.formatDuration
import io.github.daichimuratsu.videostreaming.core.ui.theme.VideoStreamingTheme
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

// 最後の操作から自動で隠すまでの時間。Media3の`PlayerView`の既定値（`PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS`）に合わせる
private val ControlsHideDelay = 5.seconds

// 再生位置を更新する間隔。時間の表示は秒までで、10〜15分の動画では縦向きのシークバーの1dpが約2秒に当たるため、
// 1秒より細かくしても表示がほとんど変わらない
private const val PROGRESS_TICK_INTERVAL_MS = 1_000L

private val OnPlayerColor = Color.White

/**
 * 映像に重ねる操作ボタン。`Player`から再生中か・再生位置を受け取り、表示は`PlayerControls`に任せる。
 *
 * `Player`の状態は、Media3の状態ホルダーで受け取る。ViewModelの`UiState`にも持つと、`Player`と食い違うおそれがあるため
 */
// 状態ホルダーはMedia3の`@UnstableApi`。Composeで`Player`の状態を受け取る公式の部品のため、Media3を上げてAPIが変わったときに直す
@OptIn(UnstableApi::class)
@Composable
internal fun PlayerControlsOverlay(
    player: Player,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playPauseState = rememberPlayPauseButtonState(player)
    // 実際に映像が進んでいるか（`Player.isPlaying`）。`playPauseState.showPlay`は、読み込み中でも再生する予定なら
    // 一時停止のアイコンを出す（`false`になる）ので、隠す判断には使わない
    val isPlaying by rememberIsPlaying(player)
    val progressState = rememberProgressStateWithTickInterval(
        player = player,
        tickIntervalMs = PROGRESS_TICK_INTERVAL_MS,
    )
    // 読み込み中は出しておく。何も映らない黒い枠だけになるのを避け、読み込み中だと伝えるため。
    // 回転のあとも、出していた操作ボタンを出したままにする
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    // 最初の再生が始まったか。回転のあとに、操作ボタンを出し直さないように保存する
    var hasStartedPlaying by rememberSaveable { mutableStateOf(false) }
    // 操作のたびに増やし、自動で隠すまでの時間を数え直す
    var interactionCount by remember { mutableIntStateOf(0) }

    // 最初の再生が始まった瞬間に、1回だけ隠す。一覧で動画を選んだ時点で目的は見ることなので、映像にかぶせない。
    // Media3の`PlayerView`の既定（`auto_show`）は再生の開始時に操作ボタンを出すが、映像をタップすると出る操作は
    // 動画アプリで共通のため、出さなくても迷わないと判断した
    LaunchedEffect(isPlaying) {
        if (isPlaying && !hasStartedPlaying) {
            hasStartedPlaying = true
            controlsVisible = false
        }
    }

    // 一時停止中は隠さない。止めているときは、次の操作を探していることが多いため
    LaunchedEffect(controlsVisible, isPlaying, interactionCount) {
        if (controlsVisible && isPlaying) {
            delay(ControlsHideDelay)
            controlsVisible = false
        }
    }

    PlayerControls(
        controlsVisible = controlsVisible,
        // アイコンは、再生する予定か（読み込み中も含む）で決める。読み込み中に再生のアイコンを出すと、押す必要があるように見えるため
        isPlaying = !playPauseState.showPlay,
        positionMs = progressState.currentPositionMs,
        durationMs = progressState.durationMs,
        onToggleControls = {
            controlsVisible = !controlsVisible
            interactionCount++
        },
        onPlayPause = {
            playPauseState.onClick()
            interactionCount++
        },
        onSeek = { positionMs -> player.seekTo(positionMs) },
        onInteraction = { interactionCount++ },
        isFullscreen = isFullscreen,
        onToggleFullscreen = {
            onToggleFullscreen()
            interactionCount++
        },
        modifier = modifier,
    )
}

/**
 * `Player`が実際に再生しているか（`Player.isPlaying`）を、Composeの状態として受け取る。
 *
 * 使っているMedia3（1.11.1）の`media3-ui-compose`には、これを受け取る状態ホルダーがないため、`Player.Listener`で受け取る
 */
@Composable
private fun rememberIsPlaying(player: Player): State<Boolean> {
    val isPlaying = remember(player) { mutableStateOf(player.isPlaying) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying.value = playing
            }
        }
        player.addListener(listener)
        // 画面を離れたあとに、`Player`が古い状態を書き換え続けないように外す
        onDispose { player.removeListener(listener) }
    }
    return isPlaying
}

/**
 * 操作ボタンを表示する。映像をタップするたびに、操作ボタンを出す・隠すを切り替える。
 *
 * `Player`を受け取らず、表示に必要な値だけを受け取る。プレビューやUIテストで、再生中・一時停止中の見た目を確かめられるようにするため
 */
@Composable
internal fun PlayerControls(
    controlsVisible: Boolean,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onToggleControls: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onInteraction: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // シークバーを動かしているあいだの位置。動かしているあいだは表示だけを変え、指を離したときに1回だけシークする。
    // シークのたびにHLSの断片を取得し直すので、動かしているあいだにシークし続けると通信が増えるため
    var draggingPositionMs by remember { mutableStateOf<Long?>(null) }
    // 長さがまだわからないとき（`C.TIME_UNSET`は負の値）は0として扱う
    val knownDurationMs = durationMs.coerceAtLeast(0)
    val shownPositionMs = (draggingPositionMs ?: positionMs).coerceIn(0, knownDurationMs)

    val toggleLabel = stringResource(
        if (controlsVisible) R.string.player_hide_controls else R.string.player_show_controls,
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            // 映像全体を押せるようにする。押したときの波紋は、映像の上では邪魔になるので出さない
            .clickable(
                interactionSource = null,
                indication = null,
                onClickLabel = toggleLabel,
                onClick = onToggleControls,
            ),
    ) {
        AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
            // 白い場面でもボタンが見えるように、映像の上に半透明の黒を重ねる
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))) {
                PlayPauseButton(
                    isPlaying = isPlaying,
                    onClick = onPlayPause,
                    modifier = Modifier.align(Alignment.Center),
                )
                Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    // 時間は左、全画面のボタンは右に置く（Media3の`PlayerView`のレイアウトも、全画面のボタンは下の右側）
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PositionText(positionMs = shownPositionMs, durationMs = knownDurationMs)
                        Spacer(modifier = Modifier.weight(1f))
                        FullscreenButton(isFullscreen = isFullscreen, onClick = onToggleFullscreen)
                    }
                    SeekBar(
                        positionMs = shownPositionMs,
                        durationMs = knownDurationMs,
                        onValueChange = { positionMs ->
                            draggingPositionMs = positionMs
                            onInteraction()
                        },
                        onValueChangeFinished = {
                            draggingPositionMs?.let(onSeek)
                            draggingPositionMs = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenButton(
    isFullscreen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 全画面中は「終了」の絵に変え、今の状態がわかるようにする
    val (iconRes, labelRes) = if (isFullscreen) {
        R.drawable.player_ic_fullscreen_exit to R.string.player_exit_fullscreen
    } else {
        R.drawable.player_ic_fullscreen to R.string.player_enter_fullscreen
    }
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(labelRes),
            tint = OnPlayerColor,
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.size(64.dp)) {
        Icon(
            painter = painterResource(
                if (isPlaying) R.drawable.player_ic_pause else R.drawable.player_ic_play_arrow,
            ),
            contentDescription = stringResource(
                if (isPlaying) R.string.player_pause else R.string.player_play,
            ),
            tint = OnPlayerColor,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
private fun PositionText(positionMs: Long, durationMs: Long, modifier: Modifier = Modifier) {
    val position = positionMs.milliseconds
    val duration = durationMs.milliseconds
    // 「1:23」が時刻として読み上げられないように、「1分23秒」の形で読ませる
    val description = stringResource(
        R.string.player_position_and_duration_description,
        durationDescription(position),
        durationDescription(duration),
    )
    Text(
        text = stringResource(
            R.string.player_position_and_duration,
            formatDuration(position),
            formatDuration(duration),
        ),
        style = MaterialTheme.typography.labelMedium,
        color = OnPlayerColor,
        modifier = modifier.semantics { contentDescription = description },
    )
}

// `Slider`の`thumb`・`track`を渡す書き方は、Material 3で`@ExperimentalMaterial3Api`のため。
// このファイルの`OptIn`はMedia3用の`androidx.annotation.OptIn`なので、Kotlinの`OptIn`は完全な名前で書く
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onValueChange: (positionMs: Long) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        R.string.player_position_and_duration_description,
        durationDescription(positionMs.milliseconds),
        durationDescription(durationMs.milliseconds),
    )
    val colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = OnPlayerColor.copy(alpha = 0.3f),
    )
    Slider(
        value = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
        onValueChange = { fraction -> onValueChange((fraction * durationMs).toLong()) },
        onValueChangeFinished = onValueChangeFinished,
        // 長さがわかるまではシークできない
        enabled = durationMs > 0,
        colors = colors,
        // 映像の上で目立ちすぎないように、細い線と小さな丸にする（Material 3の既定は太い線と縦長のつまみ）
        thumb = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = colors,
                drawStopIndicator = null,
                thumbTrackGapSize = 0.dp,
                modifier = Modifier.height(4.dp),
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            // 割合（50%など）ではなく、再生位置として読み上げる
            .semantics { stateDescription = description },
    )
}

@PreviewLightDark
@Composable
private fun PlayerControlsPlayingPreview() {
    VideoStreamingTheme(dynamicColor = false) {
        PlayerFrame {
            PlayerControls(
                controlsVisible = true,
                isPlaying = true,
                positionMs = 83_000,
                durationMs = 596_000,
                onToggleControls = {},
                onPlayPause = {},
                onSeek = {},
                onInteraction = {},
                isFullscreen = false,
                onToggleFullscreen = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PlayerControlsPausedPreview() {
    VideoStreamingTheme(dynamicColor = false) {
        PlayerFrame {
            PlayerControls(
                controlsVisible = true,
                isPlaying = false,
                positionMs = 0,
                durationMs = 596_000,
                onToggleControls = {},
                onPlayPause = {},
                onSeek = {},
                onInteraction = {},
                isFullscreen = false,
                onToggleFullscreen = {},
            )
        }
    }
}
