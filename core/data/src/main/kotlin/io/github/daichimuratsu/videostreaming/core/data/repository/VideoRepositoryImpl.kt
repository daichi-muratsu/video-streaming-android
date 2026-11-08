package io.github.daichimuratsu.videostreaming.core.data.repository

import io.github.daichimuratsu.videostreaming.core.data.network.VideoApi
import io.github.daichimuratsu.videostreaming.core.data.network.model.toDomain
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.repository.VideoRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * R2のJSONから動画の情報を取得する`VideoRepository`の実装。
 *
 * 取得と変換に成功した一覧をメモリに持ち、2回目からは取得し直さない。
 * 一覧画面で表示した動画を再生画面で開くときに、読み込み中やエラーをもう一度出さないため。
 * JSONはアプリを使っている間に変わらない前提なので、取得し直さない。
 * 一覧画面と再生画面で同じ一覧を使うため、`@Singleton`にする
 */
@Singleton
internal class VideoRepositoryImpl @Inject constructor(
    private val api: VideoApi,
) : VideoRepository {
    // 同時に呼ばれたときに取得を1回にまとめ、ほかのスレッドが書き込んだ`cache`の値を読めるようにする
    private val mutex = Mutex()
    private var cache: List<Video>? = null

    // 失敗したときは`cache`に入れずに例外を投げるので、次に呼ばれたときに取得し直す
    override suspend fun getVideos(): List<Video> = mutex.withLock {
        cache ?: api.getVideos().videos
            .map { it.toDomain() }
            .also { cache = it }
    }

    // 1本だけを取得するAPIはないので、一覧から探す
    override suspend fun getVideo(id: VideoId): Video? = getVideos().firstOrNull { it.id == id }
}
