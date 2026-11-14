package io.github.daichimuratsu.videostreaming.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.daichimuratsu.videostreaming.core.ui.theme.VideoStreamingTheme

// プレイヤーの枠は明暗どちらのテーマでも黒なので、枠の中の文字とボタンは白で描く
private val OnPlayerColor = Color.White

/**
 * プレイヤーの枠の中に、読み込み中を表示する。
 *
 * 枠の位置を変えずに中身だけを替え、読み込みが終わったときに画面が跳ねないようにするため、枠の中に出す
 */
@Composable
internal fun PlayerLoading(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.player_loading_description)
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            color = OnPlayerColor,
            modifier = Modifier.semantics { contentDescription = description },
        )
    }
}

/** プレイヤーの枠の中に、動画の情報を取得できなかったことと、再読み込みのボタンを表示する。 */
@Composable
internal fun PlayerError(onReload: () -> Unit, modifier: Modifier = Modifier) {
    PlayerErrorContent(
        title = stringResource(R.string.player_error_title),
        message = stringResource(R.string.player_error_message),
        actionLabel = stringResource(R.string.player_reload),
        onAction = onReload,
        modifier = modifier,
    )
}

/**
 * 映像の上に、再生できなかったことと、再試行のボタンを表示する。
 *
 * 通信のエラーのときだけ、通信状況を確かめる案内を出す。それ以外のエラーは、通信状況を確かめても直らないため
 */
@Composable
internal fun PlayerPlaybackError(
    kind: PlaybackErrorKind,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 止まった映像の上に出すので、白い場面でも文字が読めるように半透明の黒を重ねる。
    // 黒は枠全体に重ね、`modifier`の余白（ディスプレイカットアウトの分）は文字とボタンだけに付ける
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))) {
        PlayerErrorContent(
            title = stringResource(R.string.player_playback_error_title),
            message = stringResource(
                when (kind) {
                    PlaybackErrorKind.Network -> R.string.player_error_message
                    PlaybackErrorKind.Other -> R.string.player_playback_error_other_message
                },
            ),
            actionLabel = stringResource(R.string.player_retry),
            onAction = onRetry,
            modifier = modifier,
        )
    }
}

@Composable
private fun PlayerErrorContent(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = OnPlayerColor,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = OnPlayerColor,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onAction) {
            Text(text = actionLabel, color = OnPlayerColor)
        }
    }
}

/** プレイヤーの枠の中に、該当する動画がないことを表示する。 */
@Composable
internal fun PlayerNotFound(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.player_not_found),
            style = MaterialTheme.typography.titleMedium,
            color = OnPlayerColor,
            textAlign = TextAlign.Center,
        )
    }
}

// 枠は縦向きで高さ約200dpしかないので、文字を大きくする設定でもエラーの表示が収まるかを確かめる
@PreviewLightDark
@PreviewFontScale
@Composable
private fun PlayerErrorPreview() {
    VideoStreamingTheme(dynamicColor = false) {
        PlayerFrame { PlayerError(onReload = {}) }
    }
}

// 通信以外の文言のほうが長いので、文字を大きくする設定でも枠に収まるかを確かめる
@PreviewLightDark
@PreviewFontScale
@Composable
private fun PlayerPlaybackErrorPreview() {
    VideoStreamingTheme(dynamicColor = false) {
        PlayerFrame { PlayerPlaybackError(kind = PlaybackErrorKind.Other, onRetry = {}) }
    }
}

@PreviewLightDark
@Composable
private fun PlayerNotFoundPreview() {
    VideoStreamingTheme(dynamicColor = false) {
        PlayerFrame { PlayerNotFound() }
    }
}

@PreviewLightDark
@Composable
private fun PlayerLoadingPreview() {
    VideoStreamingTheme(dynamicColor = false) {
        PlayerFrame { PlayerLoading() }
    }
}
