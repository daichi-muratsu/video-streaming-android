package io.github.daichimuratsu.videostreaming.feature.videolist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daichimuratsu.videostreaming.core.ui.theme.VideoStreamingTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// エラーと空の表示は、通信を切るか動画のデータを空にしないと手で確かめられないため、状態を渡して確かめる
@RunWith(AndroidJUnit4::class)
class VideoListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `取得に失敗したとき見出しと説明と再読み込みのボタンを表示する`() {
        setScreen(uiState = VideoListUiState.Error)

        composeRule.onNodeWithText(context.getString(R.string.video_list_error_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.video_list_error_message))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.video_list_reload))
            .assertIsDisplayed()
    }

    @Test
    fun `取得に失敗したとき再読み込みのボタンを押すとonReloadを1回呼ぶ`() {
        var reloadCount = 0
        setScreen(uiState = VideoListUiState.Error, onReload = { reloadCount++ })

        composeRule.onNodeWithText(context.getString(R.string.video_list_reload)).performClick()

        assertEquals(1, reloadCount)
    }

    @Test
    fun `動画が1本もないとき動画がないことを表す文言を表示する`() {
        setScreen(uiState = VideoListUiState.Empty)

        composeRule.onNodeWithText(context.getString(R.string.video_list_empty))
            .assertIsDisplayed()
    }

    private fun setScreen(uiState: VideoListUiState, onReload: () -> Unit = {}) {
        composeRule.setContent {
            VideoStreamingTheme(dynamicColor = false) {
                VideoListScreen(uiState = uiState, onVideoClick = {}, onReload = onReload)
            }
        }
    }
}
