package io.github.daichimuratsu.videostreaming.feature.videolist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoWithWatchStatus
import io.github.daichimuratsu.videostreaming.core.ui.theme.VideoStreamingTheme

/**
 * 一覧画面をViewModelとつなぐ。
 *
 * 状態の受け取りとViewModelの用意はここで行い、表示は`VideoListScreen`に任せる。表示だけをプレビューやテストで確かめられるようにするため
 */
@Composable
fun VideoListRoute(
    onVideoClick: (VideoId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VideoListViewModel = hiltViewModel(),
) {
    // 画面が見えないあいだは受け取りを止める。ViewModelの`WhileSubscribed`と合わせて、再生位置の受け取りを止めるため
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    VideoListScreen(
        uiState = uiState,
        onVideoClick = onVideoClick,
        onReload = viewModel::reload,
        modifier = modifier,
    )
}

/**
 * 一覧画面の状態を受け取って表示する。
 *
 * タイトルのバーは置かない。最初の画面で唯一の一覧なので、「動画」と書いても見ればわかることを書くだけになるため
 */
@Composable
internal fun VideoListScreen(
    uiState: VideoListUiState,
    onVideoClick: (VideoId) -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 一覧をステータスバーの下から始める。裏まで流すと、時計や電池のアイコンが画像に重なって読みにくくなるため。
    // 下の端はナビゲーションバーの裏まで流し、最後の1件が隠れないように`LazyColumn`の余白で避ける
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
    ) {
        when (uiState) {
            VideoListUiState.Loading -> VideoListSkeleton()

            is VideoListUiState.Success -> VideoList(
                videos = uiState.videos,
                onVideoClick = onVideoClick,
            )

            VideoListUiState.Empty -> EmptyContent(modifier = Modifier.align(Alignment.Center))

            VideoListUiState.Error -> ErrorContent(
                onReload = onReload,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun VideoList(
    videos: List<VideoWithWatchStatus>,
    onVideoClick: (VideoId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
    ) {
        // 再生位置が変わって一覧が作り直されても、同じ動画の項目として引き継ぐため、動画のIDで結ぶ
        items(items = videos, key = { it.video.id.value }) { item ->
            VideoListItem(item = item, onClick = { onVideoClick(item.video.id) })
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.video_list_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = 24.dp),
    )
}

// 例外の種類を問わず、利用者にできるのは再読み込みだけなので、文言は1つにして、通信状況の確認を促す
@Composable
private fun ErrorContent(onReload: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.video_list_error_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.video_list_error_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        // この画面でできる操作は再読み込みだけなので、一番目立つ塗りのボタンにする
        Button(onClick = onReload) {
            Text(text = stringResource(R.string.video_list_reload))
        }
    }
}

// プレビューを本番と同じテーマで表示する。Dynamic Colorは壁紙で色が変わるので使わず、色を固定して見比べられるようにする
@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    VideoStreamingTheme(dynamicColor = false) {
        Surface(content = content)
    }
}

@PreviewLightDark
@Composable
private fun VideoListScreenSuccessPreview() {
    PreviewSurface {
        VideoListScreen(
            uiState = VideoListUiState.Success(previewVideos),
            onVideoClick = {},
            onReload = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun VideoListScreenLoadingPreview() {
    PreviewSurface {
        VideoListScreen(uiState = VideoListUiState.Loading, onVideoClick = {}, onReload = {})
    }
}

@PreviewLightDark
@Composable
private fun VideoListScreenErrorPreview() {
    PreviewSurface {
        VideoListScreen(uiState = VideoListUiState.Error, onVideoClick = {}, onReload = {})
    }
}

@PreviewLightDark
@Composable
private fun VideoListScreenEmptyPreview() {
    PreviewSurface {
        VideoListScreen(uiState = VideoListUiState.Empty, onVideoClick = {}, onReload = {})
    }
}
