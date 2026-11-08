package io.github.daichimuratsu.videostreaming.core.data.fake

import io.github.daichimuratsu.videostreaming.core.data.network.VideoApi
import io.github.daichimuratsu.videostreaming.core.data.network.model.VideoDto
import io.github.daichimuratsu.videostreaming.core.data.network.model.VideoListDto
import kotlinx.coroutines.CompletableDeferred

/**
 * テスト用の`VideoApi`。
 *
 * キャッシュを確かめるため、呼ばれた回数を数える。
 * `error`を設定すると、取得の失敗としてその例外を投げる
 */
internal class FakeVideoApi(
    private val videos: List<VideoDto>,
) : VideoApi {
    var callCount = 0
        private set

    var error: Throwable? = null

    /** 設定すると、完了するまで`getVideos`の中で止まる。同時に呼ばれた場面を作るため */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun getVideos(): VideoListDto {
        callCount++
        gate?.await()
        error?.let { throw it }
        return VideoListDto(videos)
    }
}
