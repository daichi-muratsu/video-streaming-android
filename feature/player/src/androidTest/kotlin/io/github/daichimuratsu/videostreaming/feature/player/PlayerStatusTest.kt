package io.github.daichimuratsu.videostreaming.feature.player

import androidx.compose.runtime.Composable
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

// 再生のエラー・該当なし・動画の情報の取得の失敗は、通信を切る・壊れた動画を用意するなどしないと表示されないため、部品に値を渡して確かめる
@RunWith(AndroidJUnit4::class)
class PlayerStatusTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val networkMessage = context.getString(R.string.player_error_message)
    private val otherMessage = context.getString(R.string.player_playback_error_other_message)

    @Test
    fun `通信のエラーで再生できないとき通信状況を確認する案内を表示する`() {
        setContent { PlayerPlaybackError(kind = PlaybackErrorKind.Network, onRetry = {}) }

        composeRule.onNodeWithText(context.getString(R.string.player_playback_error_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(networkMessage).assertIsDisplayed()
        composeRule.onNodeWithText(otherMessage).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.player_retry)).assertIsDisplayed()
    }

    @Test
    fun `通信以外のエラーで再生できないとき時間をおく案内を表示する`() {
        setContent { PlayerPlaybackError(kind = PlaybackErrorKind.Other, onRetry = {}) }

        composeRule.onNodeWithText(context.getString(R.string.player_playback_error_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(otherMessage).assertIsDisplayed()
        composeRule.onNodeWithText(networkMessage).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.player_retry)).assertIsDisplayed()
    }

    @Test
    fun `再生できないとき再試行のボタンを押すとonRetryを1回呼ぶ`() {
        var retryCount = 0
        setContent {
            PlayerPlaybackError(kind = PlaybackErrorKind.Network, onRetry = { retryCount++ })
        }

        composeRule.onNodeWithText(context.getString(R.string.player_retry)).performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun `該当する動画がないとき動画が見つからないことを表示する`() {
        setContent { PlayerNotFound() }

        composeRule.onNodeWithText(context.getString(R.string.player_not_found))
            .assertIsDisplayed()
    }

    @Test
    fun `動画の情報の取得に失敗したとき見出しと説明と再読み込みのボタンを表示する`() {
        setContent { PlayerError(onReload = {}) }

        composeRule.onNodeWithText(context.getString(R.string.player_error_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(networkMessage).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.player_reload)).assertIsDisplayed()
    }

    @Test
    fun `動画の情報の取得に失敗したとき再読み込みのボタンを押すとonReloadを1回呼ぶ`() {
        var reloadCount = 0
        setContent { PlayerError(onReload = { reloadCount++ }) }

        composeRule.onNodeWithText(context.getString(R.string.player_reload)).performClick()

        assertEquals(1, reloadCount)
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            VideoStreamingTheme(dynamicColor = false) { content() }
        }
    }
}
