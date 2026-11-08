package io.github.daichimuratsu.videostreaming

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.daichimuratsu.videostreaming.feature.videolist.VideoListRoute
import kotlinx.serialization.Serializable

/** 一覧画面を表すキー。 */
@Serializable
data object VideoListKey : NavKey

/**
 * アプリの画面遷移。バックスタックの最後のキーに対応する画面を表示する。
 *
 * featureモジュール同士は依存させないので、画面同士は`:app`のここでつなぐ
 */
@Composable
fun VideoStreamingNavDisplay(modifier: Modifier = Modifier) {
    // 画面の回転やプロセスの終了のあとも、表示していた画面に戻せるように保存する
    val backStack = rememberNavBackStack(VideoListKey)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        // ViewModelを画面（`NavEntry`）ごとに作り、画面をバックスタックから外したときに破棄するため
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<VideoListKey> {
                // 再生画面を作るときに、再生画面への遷移をつなぐ
                VideoListRoute(onVideoClick = {})
            }
        },
        modifier = modifier,
    )
}
