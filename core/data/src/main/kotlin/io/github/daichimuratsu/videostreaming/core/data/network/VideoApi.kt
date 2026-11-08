package io.github.daichimuratsu.videostreaming.core.data.network

import io.github.daichimuratsu.videostreaming.core.data.network.model.VideoListDto
import retrofit2.http.GET

/** R2に置いた動画の情報（JSON）を取得するRetrofitのinterface。 */
internal interface VideoApi {
    /**
     * 動画の一覧を取得する。
     *
     * 戻り値を`Response`で包まない。HTTPのエラー（2xx以外）はRetrofitが`HttpException`として投げるので、
     * `VideoRepository`の「取得に失敗した場合は例外を投げる」を、成功か失敗かを判定する処理なしで満たせるため
     */
    @GET("videos.json")
    suspend fun getVideos(): VideoListDto
}
