package io.github.daichimuratsu.videostreaming.feature.videolist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoWithWatchStatus
import io.github.daichimuratsu.videostreaming.core.domain.model.WatchStatus
import io.github.daichimuratsu.videostreaming.core.ui.durationDescription
import io.github.daichimuratsu.videostreaming.core.ui.formatDuration
import kotlin.time.Duration

/**
 * 一覧の1件（サムネイル・タイトル・作者・公開年・再生時間・視聴状態）を表示する。
 *
 * 1件全体を押せるようにする。範囲が広いほうが押しやすく、読み上げでも1件が1つのまとまりになるため
 */
@Composable
internal fun VideoListItem(
    item: VideoWithWatchStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val video = item.video
    Column(
        modifier = modifier
            .fillMaxWidth()
            // 読み上げで、ダブルタップの操作を「再生」と伝えるため
            .clickable(
                onClickLabel = stringResource(R.string.video_list_play_label),
                onClick = onClick,
            ),
    ) {
        Thumbnail(
            url = video.thumbnailUrl,
            duration = video.duration.value,
            status = item.status,
        )
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
        )
        Text(
            text = stringResource(
                R.string.video_list_creator_and_year,
                video.creator,
                video.publishedAt.year,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun Thumbnail(
    url: String,
    duration: Duration,
    status: WatchStatus,
    modifier: Modifier = Modifier,
) {
    // 画像が届く前から16:9の枠を取り、届いたときに下の項目がずれないようにする
    Box(modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
        // 読み込み中と失敗時は同じ色の箱にする。失敗してもタイトルで選べるので、失敗の印は出さない
        val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
        AsyncImage(
            model = url,
            // すぐ下のタイトルと同じ内容を2回読み上げないため
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = placeholder,
            error = placeholder,
            modifier = Modifier.matchParentSize(),
        )
        if (status == WatchStatus.Watched) {
            Badge(
                text = stringResource(R.string.video_list_watched),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 8.dp),
            )
        }
        val durationDescription = durationDescription(duration)
        Badge(
            text = formatDuration(duration),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp)
                // 「9:56」が時刻として読み上げられないように、「9分56秒」と読ませる
                .semantics { contentDescription = durationDescription },
        )
        if (status is WatchStatus.InProgress) {
            val inProgressDescription = stringResource(R.string.video_list_in_progress_description)
            LinearProgressIndicator(
                progress = { status.progress.toFloat() },
                // サムネイルの下の端に重ねる細い帯にするため、端の丸み・すき間・終わりの点をなくす
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .semantics { contentDescription = inProgressDescription },
            )
        }
    }
}

// サムネイルの上に重ねる文字。画像の色にかかわらず読めるように、テーマの色ではなく半透明の黒の上に白で書く
@Composable
private fun Badge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}
