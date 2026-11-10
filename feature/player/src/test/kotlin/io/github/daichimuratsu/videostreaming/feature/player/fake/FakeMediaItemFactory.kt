package io.github.daichimuratsu.videostreaming.feature.player.fake

import androidx.media3.common.MediaItem
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.feature.player.MediaItemFactory

/**
 * テスト用の`MediaItemFactory`。動画のIDだけを入れた`MediaItem`を作る。
 *
 * URLを入れると、`MediaItem.Builder.setUri`がAndroidの`Uri.parse`を呼び、JVMのユニットテストで失敗するため。
 * IDで、どの動画の`MediaItem`かを見分ける
 */
class FakeMediaItemFactory : MediaItemFactory {
    override fun create(video: Video): MediaItem =
        MediaItem.Builder().setMediaId(video.id.value).build()
}
