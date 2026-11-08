package io.github.daichimuratsu.videostreaming.core.domain.repository

import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId

/** 動画の情報を取得するRepository。 */
interface VideoRepository {
    /**
     * 動画の一覧を取得する。
     *
     * 取得に失敗した場合（ネットワークのエラー、JSONの形式の誤りなど）は、例外を投げる
     */
    suspend fun getVideos(): List<Video>

    /**
     * 指定したIDの動画を取得する。該当する動画がない場合は`null`を返す。
     *
     * 取得に失敗した場合は、例外を投げる
     */
    suspend fun getVideo(id: VideoId): Video?
}
