package io.github.daichimuratsu.videostreaming

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Hiltのコード生成の起点。アプリ全体で共有する依存関係のコンテナ（SingletonComponent）が、このクラスに対して生成される */
@HiltAndroidApp
class VideoStreamingApplication : Application()
