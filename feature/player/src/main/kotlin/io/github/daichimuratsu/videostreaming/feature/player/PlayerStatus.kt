package io.github.daichimuratsu.videostreaming.feature.player

import androidx.compose.foundation.layout.Arrangement
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.player_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = OnPlayerColor,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.player_error_message),
            style = MaterialTheme.typography.bodySmall,
            color = OnPlayerColor,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onReload) {
            Text(text = stringResource(R.string.player_reload), color = OnPlayerColor)
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
