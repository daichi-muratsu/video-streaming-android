package io.github.daichimuratsu.videostreaming

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.github.daichimuratsu.videostreaming.core.ui.theme.VideoStreamingTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoStreamingTheme {
                // 画面ごとに背景を塗らずに済むように、ここでテーマの背景の色を敷く。
                // ステータスバー・ナビゲーションバーの裏も、この色になる
                Surface(modifier = Modifier.fillMaxSize()) {
                    VideoStreamingNavDisplay()
                }
            }
        }
    }
}
