package io.github.daichimuratsu.videostreaming

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

/**
 * Hiltのコード生成の起点。アプリ全体で共有する依存関係のコンテナ（SingletonComponent）が、このクラスに対して生成される
 *
 * Coilの`ImageLoader`もここで作る。Coilの公式が、Androidアプリでは`Application`に`SingletonImageLoader.Factory`を実装する方法を勧めているため
 */
@HiltAndroidApp
class VideoStreamingApplication :
    Application(),
    SingletonImageLoader.Factory {
    // `:core:data`が提供する`OkHttpClient`。動画の情報と同じR2のホストからサムネイルを取得するので、接続を共有する。
    // `Lazy`にして、最初に画像を読み込むときまで作らず、起動を遅くしない
    @Inject
    lateinit var okHttpClient: Lazy<OkHttpClient>

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() }))
        }
        // 画像が届いた瞬間に置き換わると、サムネイルがばらばらに切り替わってちらつくため、読み込み中の箱からなめらかに切り替える
        .crossfade(true)
        .build()
}
