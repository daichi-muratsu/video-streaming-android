package io.github.daichimuratsu.videostreaming.core.data.network

import io.github.daichimuratsu.videostreaming.core.data.network.model.VideoListDto
import io.github.daichimuratsu.videostreaming.core.data.network.model.toDomain
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import kotlinx.serialization.SerializationException

class VideoJsonTest : FunSpec({
    context("リポジトリの`data/videos.json`") {
        // テストのリソースに`data/`を追加してあるので、本物のファイルをクラスパスから読める
        val json = checkNotNull(
            VideoJsonTest::class.java.classLoader?.getResource("videos.json"),
        ).readText()
        val dto = networkJson.decodeFromString<VideoListDto>(json)

        // `Video`・`License`の文字列の項目には`require`がないので、空でないことをここで確かめる
        test("3本あり、文字列の項目・タグがすべて空でない") {
            dto.videos shouldHaveSize 3
            dto.videos.forEach { video ->
                assertSoftly(video) {
                    id.shouldNotBeBlank()
                    title.shouldNotBeBlank()
                    description.shouldNotBeBlank()
                    creator.shouldNotBeBlank()
                    publishedAt.shouldNotBeBlank()
                    thumbnailUrl.shouldNotBeBlank()
                    streamUrl.shouldNotBeBlank()
                    tags.shouldNotBeEmpty()
                    tags.forEach { it.shouldNotBeBlank() }
                    license.name.shouldNotBeBlank()
                    license.url.shouldNotBeBlank()
                    license.attribution.shouldNotBeBlank()
                    license.changes.shouldNotBeBlank()
                }
            }
        }

        // `getVideo(id)`は一覧の最初の1本を返すので、重複があると2本目を開けない
        test("IDが重複していない") {
            val ids = dto.videos.map { it.id }
            ids.distinct() shouldBe ids
        }

        test("すべての動画を`Video`に変換できる") {
            dto.videos.map { it.toDomain() } shouldHaveSize 3
        }
    }

    context("JSONの形") {
        test("知らないキーがあっても、読み飛ばして変換できる") {
            val dto = networkJson.decodeFromString<VideoListDto>(
                videoListJson(extra = """"chapters": [],"""),
            )

            dto.videos.single().title shouldBe "タイトル"
        }

        test("必要な項目がないと、変換が失敗する") {
            shouldThrow<SerializationException> {
                networkJson.decodeFromString<VideoListDto>(videoListJson(titleKey = null))
            }
        }

        // 知らないキー（`tittle`）は読み飛ばされ、必要な`title`がないことになる
        test("キーを書き間違えると、変換が失敗する") {
            shouldThrow<SerializationException> {
                networkJson.decodeFromString<VideoListDto>(videoListJson(titleKey = "tittle"))
            }
        }
    }
})

/** 動画が1本のJSONを作る。`titleKey`を`null`にすると、`title`の項目を書かない。 */
private fun videoListJson(titleKey: String? = "title", extra: String = ""): String {
    val title = titleKey?.let { """"$it": "タイトル",""" } ?: ""
    return """
        {
          "videos": [
            {
              "id": "big-buck-bunny",
              $title
              $extra
              "description": "説明",
              "creator": "作者",
              "durationSeconds": 596,
              "publishedAt": "2008-05-30",
              "thumbnailUrl": "https://example.com/thumbnail.jpg",
              "streamUrl": "https://example.com/master.m3u8",
              "tags": ["animation"],
              "license": {
                "name": "CC BY 3.0",
                "url": "https://creativecommons.org/licenses/by/3.0/",
                "attribution": "表記",
                "changes": "改変"
              }
            }
          ]
        }
    """.trimIndent()
}
