package io.github.daichimuratsu.videostreaming.feature.player

import androidx.media3.common.PlaybackException

/**
 * 再生のエラーの種類。エラーの表示で、どの案内を出すかを決める。
 *
 * 通信のエラーだけを分ける。それ以外（サーバーのエラー・形式の誤り・デコードの失敗など）は、
 * ユーザーが通信状況を確かめても直らないので、通信の案内を出さない
 */
internal enum class PlaybackErrorKind {
    Network,
    Other,
    ;

    companion object {
        /**
         * `PlaybackException.errorCode`から種類を返す。
         *
         * `PlaybackException`ではなく`errorCode`を受け取る。`PlaybackException`を作るとAndroidの`SystemClock`・`Bundle`を使うので、
         * JVMのユニットテストで判定を確かめられないため
         */
        fun of(errorCode: Int): PlaybackErrorKind = when (errorCode) {
            // 接続の失敗（圏外・ホストに届かない・サーバーが接続を切った）と、時間切れ
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            -> Network

            else -> Other
        }
    }
}
