package io.github.daichimuratsu.videostreaming.feature.player.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.daichimuratsu.videostreaming.feature.player.HlsMediaItemFactory
import io.github.daichimuratsu.videostreaming.feature.player.MediaItemFactory

/**
 * 再生画面のViewModelに渡す`Player`と`MediaItemFactory`を提供するHiltのModule。
 *
 * ViewModelは`Player`インターフェースだけを受け取り、ExoPlayerの型と`Context`を知らずに済むよう、ここで作る
 */
@Module
@InstallIn(ViewModelComponent::class)
internal abstract class PlayerModule {
    @Binds
    abstract fun bindMediaItemFactory(factory: HlsMediaItemFactory): MediaItemFactory

    companion object {
        // スコープを付けない。ViewModelを作るたびに新しいExoPlayerを作り、そのViewModelが`onCleared`で解放する。
        // Activityの`Context`を渡すと、回転で破棄されたActivityをExoPlayerが持ち続けるので、`ApplicationContext`にする
        // `EventLogger`はMedia3の`@UnstableApi`。デバッグのビルドで再生の出来事を確かめるためだけに使う
        @OptIn(UnstableApi::class)
        @Provides
        fun providePlayer(@ApplicationContext context: Context): Player = ExoPlayer.Builder(context)
            // オーディオフォーカスはExoPlayerに任せる。ほかのアプリが音を出し始めたら一時停止し、通知音のあいだは音量を下げる
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */
                true,
            )
            // イヤホンを抜いたり、Bluetoothのイヤホンを切ったりしたら一時停止する。スピーカーから急に音が出ないようにするため
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                // デバッグのビルドだけ、再生の出来事（選ばれた画質など）をログに書き出す。画質が回線の速さに合わせて
                // 切り替わるかを確かめるため。`BuildConfig`はライブラリのモジュールでは既定で作られないので、
                // アプリの`debuggable`（`FLAG_DEBUGGABLE`）で見分ける
                if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    addAnalyticsListener(EventLogger())
                }
            }
    }
}
