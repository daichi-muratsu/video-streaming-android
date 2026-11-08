package io.github.daichimuratsu.videostreaming.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.daichimuratsu.videostreaming.core.data.network.VideoApi
import io.github.daichimuratsu.videostreaming.core.data.network.networkJson
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** 動画の情報を取得する通信の部品を提供するHiltのModule。 */
@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    // R2のパブリック開発URL。サムネイルと動画のURLはJSONに完全なURLで書いてあるので、ここで使うのは`videos.json`の取得だけ
    private const val BASE_URL = "https://pub-ccc81773052c4e64b5db10913b0f0739.r2.dev/"

    // 画像の読み込み（Coil）とコネクションプールを共有するため、アプリで1つにする
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Singleton
    fun provideVideoApi(okHttpClient: OkHttpClient): VideoApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(
            networkJson.asConverterFactory("application/json; charset=utf-8".toMediaType()),
        )
        .build()
        .create()
}
