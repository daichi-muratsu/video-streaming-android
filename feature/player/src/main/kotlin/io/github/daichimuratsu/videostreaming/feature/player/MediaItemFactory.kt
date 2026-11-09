package io.github.daichimuratsu.videostreaming.feature.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import javax.inject.Inject

/**
 * 動画から、`Player`に渡す`MediaItem`を作る。
 *
 * `MediaItem.Builder.setUri`はAndroidの`Uri.parse`を呼び、JVMのユニットテストでは動かない。
 * 作る部分をここに分け、ViewModelのテストではFakeに差し替える
 */
interface MediaItemFactory {
    fun create(video: Video): MediaItem
}

internal class HlsMediaItemFactory @Inject constructor() : MediaItemFactory {
    override fun create(video: Video): MediaItem = MediaItem.Builder()
        .setMediaId(video.id.value)
        .setUri(video.streamUrl)
        // URLの拡張子から形式を推測させず、HLSと明示する。拡張子のないURLでも、HLSとして再生するため
        .setMimeType(MimeTypes.APPLICATION_M3U8)
        .build()
}
