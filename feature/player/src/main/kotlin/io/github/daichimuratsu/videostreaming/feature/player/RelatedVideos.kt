package io.github.daichimuratsu.videostreaming.feature.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.ui.VideoCard
import io.github.daichimuratsu.videostreaming.core.ui.theme.VideoStreamingTheme

/**
 * 関連動画の見出しと、動画のカードの一覧を表示する。
 *
 * 一覧画面と見た目をそろえるため、同じ`VideoCard`を使う。関連動画がなければ、見出しも出さない。
 * `LazyColumn`ではなく`Column`で並べる。説明の箱と一緒にスクロールする親の中に置き、件数も少ないため
 */
@Composable
internal fun RelatedVideos(
    videos: List<Video>,
    onVideoClick: (VideoId) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (videos.isEmpty()) return
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.player_related_videos),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                // 読み上げで、見出しから見出しへ移れるようにするため
                .semantics { heading() },
        )
        videos.forEach { video ->
            // 視聴状態は渡さない。渡すには再生位置と組み合わせる処理が要り、状態の作り方が複雑になるため
            VideoCard(video = video, onClick = { onVideoClick(video.id) })
        }
    }
}

@PreviewLightDark
@Composable
private fun RelatedVideosPreview() {
    VideoStreamingTheme(dynamicColor = false) {
        RelatedVideos(videos = previewRelatedVideos, onVideoClick = {})
    }
}
