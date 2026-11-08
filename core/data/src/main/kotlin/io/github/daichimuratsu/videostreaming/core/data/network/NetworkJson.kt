package io.github.daichimuratsu.videostreaming.core.data.network

import kotlinx.serialization.json.Json

/**
 * 取得したJSONを変換するときの設定。
 *
 * 知らないキーを読み飛ばす。R2の`videos.json`はどのバージョンのアプリも読むので、
 * JSONに項目を追加したときに、前のバージョンのアプリが一覧の取得ごと失敗しないようにするため。
 * 必要な項目がない場合は、DTOの項目に既定値がないので変換が失敗する。
 * Retrofitとテストで同じ設定を使うため、ここに1つだけ置く
 */
internal val networkJson: Json = Json { ignoreUnknownKeys = true }
