package io.github.daichimuratsu.videostreaming.feature.player

import androidx.media3.common.Player
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * `Player`の値を、`PlaybackProgress`の形に変換するアダプター。
 *
 * ミリ秒から`Duration`への変換と、`Player.Listener`から`Flow`への変換を、`Player`との境界のここだけで行う
 */
internal class PlayerPlaybackProgress(private val player: Player) : PlaybackProgress {
    override val currentPosition: Duration
        get() = player.currentPosition.milliseconds

    override val isPlaying: Flow<Boolean> = callbackFlow {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                trySend(isPlaying)
            }
        }
        // 受け取り始める前に再生が始まっていても、取りこぼさないように現在の値を先に出力する
        trySend(player.isPlaying)
        player.addListener(listener)
        // 受け取りをやめたら外す。外さないと、`Player`がリスナーを持ち続けるため
        awaitClose { player.removeListener(listener) }
    }
}
