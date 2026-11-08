package io.github.daichimuratsu.videostreaming.feature.videolist

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// 1画面に入る本数。本物の本数は読み込むまでわからないので、画面を埋める数だけ並べる
private const val SKELETON_ITEM_COUNT = 3

/**
 * 読み込み中に、一覧と同じ形の箱（スケルトン）を並べて表示する。
 *
 * 読み込みは短く一瞬で消えることが多いので、中央の`CircularProgressIndicator`ではなく一覧と同じ形にし、一覧に置き換わったときにちらつかないようにする。
 * ゆっくり明滅させ、長くかかったときも止まって見えないようにする
 */
@Composable
internal fun VideoListSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_000), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    val description = stringResource(R.string.video_list_loading_description)
    Column(
        modifier = modifier
            .semantics { contentDescription = description }
            // 明るさは描画のときに読み、毎フレームの再コンポーズを避ける
            .graphicsLayer { this.alpha = alpha },
    ) {
        repeat(SKELETON_ITEM_COUNT) { SkeletonItem() }
    }
}

@Composable
private fun SkeletonItem(modifier: Modifier = Modifier) {
    // サムネイルの読み込み中と同じ色にし、スケルトンから一覧に置き換わったときの見た目をつなげる
    val color = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(color))
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 10.dp)
                .fillMaxWidth(0.6f)
                .height(16.dp)
                .background(color, RoundedCornerShape(4.dp)),
        )
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 6.dp, bottom = 18.dp)
                .fillMaxWidth(0.4f)
                .height(12.dp)
                .background(color, RoundedCornerShape(4.dp)),
        )
    }
}
