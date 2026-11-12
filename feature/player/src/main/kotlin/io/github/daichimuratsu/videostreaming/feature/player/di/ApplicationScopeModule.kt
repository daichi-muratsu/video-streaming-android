package io.github.daichimuratsu.videostreaming.feature.player.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * アプリと同じ長さのスコープを表すQualifier。
 *
 * `viewModelScope`は`onCleared`で止まるので、画面を離れたあとも続ける処理（再生位置の最後の保存）に使う
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ApplicationScope

/**
 * アプリと同じ長さのスコープを提供するHiltのModule。
 *
 * 公式のアーキテクチャガイド（Data layerの「Make an operation live longer than the screen」）のとおり、
 * スコープは使う側で作らず、コンストラクタで受け取る。テストでテスト用のスコープに替えるため。
 * 使うのは再生画面だけなので、`:feature:player`に置く
 */
@Module
@InstallIn(SingletonComponent::class)
internal object ApplicationScopeModule {
    // `SupervisorJob`にする。`Job`だと、1つの保存が失敗したときにスコープごと止まり、そのあとの保存が動かないため
    // 受け止めなかった例外は、ログに書いて終える。書かないと、Androidではアプリがクラッシュする。
    // 再生位置が数秒ずれても視聴への影響は小さいので、保存の失敗で再生を止めない
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.Default +
            CoroutineExceptionHandler { _, throwable ->
                Log.w(TAG, "アプリのスコープの処理が失敗した", throwable)
            },
    )

    private const val TAG = "ApplicationScope"
}
