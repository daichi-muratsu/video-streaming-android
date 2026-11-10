package io.github.daichimuratsu.videostreaming

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.feature.player.PlayerRoute
import io.github.daichimuratsu.videostreaming.feature.videolist.VideoListRoute
import kotlinx.serialization.Serializable

/** 一覧画面を表すキー。 */
@Serializable
data object VideoListKey : NavKey

/**
 * 再生画面を表すキー。
 *
 * 動画のIDは`VideoId`ではなく`String`で持つ。キーは保存するため`@Serializable`にする必要があり、
 * `VideoId`を入れると、Domain層に`kotlinx.serialization`の注釈を持ち込むことになるため
 */
@Serializable
data class PlayerKey(val videoId: String) : NavKey

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
                VideoListRoute(onVideoClick = { videoId ->
                    // 一番上が一覧のときだけ積む。素早く2回押すと再生画面が2つ積まれ、「戻る」を2回押さないと一覧に戻れないため。
                    // 画面の状態ではなくバックスタックを見るので、2回のタップがほぼ同時に届いても防げる
                    if (backStack.lastOrNull() == VideoListKey) {
                        backStack.add(PlayerKey(videoId.value))
                    }
                })
            }
            entry<PlayerKey> { key ->
                PlayerRoute(videoId = VideoId(key.videoId))
            }
        },
        modifier = modifier,
    )
}
