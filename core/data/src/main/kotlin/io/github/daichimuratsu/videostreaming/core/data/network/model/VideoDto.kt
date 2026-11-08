package io.github.daichimuratsu.videostreaming.core.data.network.model

import io.github.daichimuratsu.videostreaming.core.domain.model.Video
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoDuration
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.Serializable

/**
 * `videos.json`の動画1本分を表すDTO。
 *
 * `:core:domain`にkotlinx.serializationの注釈を持ち込まないため、`Video`とは別のクラスにする。
 * JSONの形（秒の数値・日付の文字列）のまま持ち、`Video`への変換は`toDomain`で行う。
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

/**
 * 動画1本分のDTOを、Domain層の`Video`に変換する。
 *
 * 秒の数値と日付の文字列は、取得元との境界であるここで`Duration`と`LocalDate`に変換する。
 * 値が正しくない場合（IDが空、長さが0以下、日付の形式の誤り）は、値オブジェクトの`require`や`LocalDate.parse`の例外をそのまま投げる。
 * `VideoRepository`の「取得に失敗した場合は例外を投げる」に含めるため
 */
internal fun VideoDto.toDomain(): Video = Video(
    id = VideoId(id),
    title = title,
    description = description,
    creator = creator,
    duration = VideoDuration(durationSeconds.seconds),
    publishedAt = LocalDate.parse(publishedAt),
    thumbnailUrl = thumbnailUrl,
    streamUrl = streamUrl,
    tags = tags,
    license = license.toDomain(),
)
