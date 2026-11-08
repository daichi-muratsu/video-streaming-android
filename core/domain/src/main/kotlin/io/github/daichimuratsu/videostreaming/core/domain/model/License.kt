package io.github.daichimuratsu.videostreaming.core.domain.model

/**
 * 動画のライセンスを表す値オブジェクト。
 *
 * ライセンスの名前・URL・原作者の表記・改変した内容は、クレジットでいつも一緒に使うため、1つの型にまとめる
 */
data class License(
    /** ライセンスの名前（例：CC BY 3.0） */
    val name: String,
    /** ライセンスのページのURL */
    val url: String,
    /** 原作者の表記 */
    val attribution: String,
    /** 改変した内容。CC BYのクレジットで示すため、原作者の表記とは分けて持つ */
    val changes: String,
)
