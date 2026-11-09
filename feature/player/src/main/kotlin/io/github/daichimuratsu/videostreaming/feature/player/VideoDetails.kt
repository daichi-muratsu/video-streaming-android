package io.github.daichimuratsu.videostreaming.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.daichimuratsu.videostreaming.core.domain.model.License
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.ui.theme.VideoStreamingTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * プレイヤーの下に、タイトルと説明の箱を表示する。
 *
 * 説明の箱は最初は閉じておく。クレジットは場所をとり、その下に置く予定の関連動画が押し出されるため
 */
@Composable
internal fun VideoDetails(video: Video, modifier: Modifier = Modifier) {
    // 回転のあとも、開いた状態を保つ
    var expanded by rememberSaveable { mutableStateOf(false) }
    VideoDetailsContent(
        video = video,
        expanded = expanded,
        onToggleExpanded = { expanded = !expanded },
        modifier = modifier,
    )
}

/**
 * タイトルと説明の箱を表示する。閉じているときは作者・公開日・説明の1行目、開いているときはクレジットまで出す。
 *
 * 作者・公開日・説明・クレジットを1つの箱にまとめ、箱のどこを押しても開閉できるようにする。
 * クレジットの見出しの行だけを開閉する形は、その行だけが浮いて見えるため
 */
@Composable
internal fun VideoDetailsContent(
    video: Video,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleLabel = stringResource(
        if (expanded) R.string.player_show_less else R.string.player_show_more,
    )
    val stateLabel = stringResource(
        if (expanded) R.string.player_details_expanded else R.string.player_details_collapsed,
    )
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = video.title, style = MaterialTheme.typography.titleLarge)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClickLabel = toggleLabel, onClick = onToggleExpanded)
                // 読み上げで、開いているか閉じているかを伝える
                .semantics { stateDescription = stateLabel }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.player_creator_and_date,
                    video.creator,
                    formatPublishedDate(video.publishedAt),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodyMedium,
                // 閉じているときは1行にし、関連動画を上に出す
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (expanded) {
                Credit(license = video.license, modifier = Modifier.padding(top = 8.dp))
            }
            Text(
                text = toggleLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

// CC BYの決まり（原作者の表記・ライセンスとそのページへのリンク・改変した内容）を満たすクレジット
@Composable
private fun Credit(license: License, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.player_credit_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(text = license.attribution, style = MaterialTheme.typography.bodySmall)
        LicenseText(license = license)
        Text(
            text = stringResource(R.string.player_changes, license.changes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ライセンスの名前の部分だけをリンクにし、押すとブラウザーでライセンスのページを開く
@Composable
private fun LicenseText(license: License) {
    val text = stringResource(R.string.player_license, license.name)
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    val annotatedText = remember(text, license, linkStyles) {
        buildAnnotatedString {
            append(text)
            val start = text.indexOf(license.name)
            if (start >= 0) {
                addLink(
                    LinkAnnotation.Url(license.url, linkStyles),
                    start,
                    start + license.name.length,
                )
            }
        }
    }
    Text(text = annotatedText, style = MaterialTheme.typography.bodySmall)
}

// 端末の言語に合わせた書き方にする（日本語なら「2008年5月30日」）
private fun formatPublishedDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))

@PreviewLightDark
@Composable
private fun VideoDetailsCollapsedPreview() {
    VideoStreamingTheme(dynamicColor = false) {
        VideoDetailsContent(video = previewVideo, expanded = false, onToggleExpanded = {})
    }
}

@PreviewLightDark
@Composable
private fun VideoDetailsExpandedPreview() {
    VideoStreamingTheme(dynamicColor = false) {
        VideoDetailsContent(video = previewVideo, expanded = true, onToggleExpanded = {})
    }
}
