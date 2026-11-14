package io.github.daichimuratsu.videostreaming.feature.player

import androidx.media3.common.PlaybackException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

// 通信のエラーは2001・2002だけなので、両方と、すぐ外側の2000・2003を境界値にする。
// それ以外は、エラーの種類ごとに代表値を1つずつ確かめる
class PlaybackErrorKindTest : FunSpec({
    context("通信のエラー") {
        listOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        ).forEach { errorCode ->
            test("${PlaybackException.getErrorCodeName(errorCode)}なら、通信のエラー") {
                PlaybackErrorKind.of(errorCode) shouldBe PlaybackErrorKind.Network
            }
        }
    }

    context("通信以外のエラー") {
        listOf(
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_TIMEOUT,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
        ).forEach { errorCode ->
            test("${PlaybackException.getErrorCodeName(errorCode)}なら、通信以外のエラー") {
                PlaybackErrorKind.of(errorCode) shouldBe PlaybackErrorKind.Other
            }
        }
    }
})
