package io.github.daichimuratsu.videostreaming.core.ui

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.daichimuratsu.videostreaming.core.domain.model.License
import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoDuration
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.github.daichimuratsu.videostreaming.core.domain.model.WatchStatus
import io.github.daichimuratsu.videostreaming.core.ui.theme.VideoStreamingTheme
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// 視聴済みの印と視聴途中の帯は、手で確かめるには動画を10秒以上または90%以上まで再生する必要があるため、状態ごとに表示を確かめる
@RunWith(AndroidJUnit4::class)
class VideoCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val watchedLabel = context.getString(R.string.core_ui_watched)
    private val inProgressDescription = context.getString(R.string.core_ui_in_progress_description)

    @Test
    fun `視聴済みのとき視聴済みの印だけを表示する`() {
        setCard(status = WatchStatus.Watched)

        composeRule.onNodeWithText(watchedLabel).assertExists()
        composeRule.onNodeWithContentDescription(inProgressDescription).assertDoesNotExist()
    }

    @Test
    fun `視聴途中のとき再生した割合の帯だけを表示する`() {
        setCard(status = WatchStatus.InProgress(progress = 0.5))

        composeRule.onNodeWithContentDescription(inProgressDescription)
            .assertRangeInfoEquals(ProgressBarRangeInfo(current = 0.5f, range = 0f..1f))
        composeRule.onNodeWithText(watchedLabel).assertDoesNotExist()
    }

    @Test
    fun `未視聴のとき印と帯を表示しない`() {
        setCard(status = WatchStatus.NotStarted)

        composeRule.onNodeWithText(watchedLabel).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(inProgressDescription).assertDoesNotExist()
    }

    // 関連動画のカードは`status`を渡さないため、既定値の`null`でも表示しないことを確かめる
    @Test
    fun `視聴状態を渡さないとき印と帯を表示しない`() {
        setCard(status = null)

        composeRule.onNodeWithText(watchedLabel).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(inProgressDescription).assertDoesNotExist()
    }

    private fun setCard(status: WatchStatus?) {
        composeRule.setContent {
            VideoStreamingTheme(dynamicColor = false) {
                VideoCard(video = testVideo, onClick = {}, status = status)
            }
        }
    }

    private val testVideo = Video(
        id = VideoId("big-buck-bunny"),
        title = "Big Buck Bunny",
        description = "",
        creator = "Blender Foundation",
        duration = VideoDuration(596.seconds),
        publishedAt = LocalDate.of(2008, 5, 30),
        thumbnailUrl = "",
        streamUrl = "",
        tags = emptyList(),
        license = License(name = "CC BY 3.0", url = "", attribution = "", changes = ""),
    )
}
