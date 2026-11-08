package io.github.daichimuratsu.videostreaming.core.data.network.model

import kotlinx.serialization.Serializable

/**
 * `videos.json`の動画1本分を表すDTO。
 *
 * `:core:domain`にkotlinx.serializationの注釈を持ち込まないため、`Video`とは別のクラスにする。
 * JSONの形（秒の数値・日付の文字列）のまま持ち、`Video`への変換はRepositoryで行う。
 * 項目には既定値を付けない。JSONに項目がないときに、読み込みの時点で失敗させるため
 */
@Serializable
internal data class VideoDto(
    val id: String,
    val title: String,
    val description: String,
    val creator: String,
    val durationSeconds: Int,
    /** ISO 8601の日付（例：`2008-05-30`） */
    val publishedAt: String,
    val thumbnailUrl: String,
    val streamUrl: String,
    val tags: List<String>,
    val license: LicenseDto,
)
