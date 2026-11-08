package io.github.daichimuratsu.videostreaming.core.data.repository

import io.github.daichimuratsu.videostreaming.core.data.fake.FakeVideoApi
import io.github.daichimuratsu.videostreaming.core.data.fake.testVideoDto
import io.github.daichimuratsu.videostreaming.core.data.network.model.toDomain
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class VideoRepositoryImplTest : FunSpec({
    val dtos = listOf(testVideoDto("big-buck-bunny"), testVideoDto("sintel"))
    val videos = dtos.map { it.toDomain() }

    test("取得に成功すると、変換した一覧を返す") {
        val repository = VideoRepositoryImpl(FakeVideoApi(dtos))

        repository.getVideos() shouldBe videos
    }

    context("キャッシュ") {
        test("2回呼んでも、取得は1回だけ") {
            val api = FakeVideoApi(dtos)
            val repository = VideoRepositoryImpl(api)

            repository.getVideos()
            repository.getVideos() shouldBe videos

            api.callCount shouldBe 1
        }

        test("取得に失敗したら例外を伝え、次に呼ばれたときに取得し直す") {
            val api = FakeVideoApi(dtos).apply { error = IOException("取得の失敗") }
            val repository = VideoRepositoryImpl(api)

            shouldThrow<IOException> { repository.getVideos() }

            api.error = null
            repository.getVideos() shouldBe videos
            api.callCount shouldBe 2
        }

        test("同時に2回呼んでも、取得は1回だけ") {
            runTest {
                val gate = CompletableDeferred<Unit>()
                val api = FakeVideoApi(dtos).apply { this.gate = gate }
                val repository = VideoRepositoryImpl(api)

                val first = async { repository.getVideos() }
                val second = async { repository.getVideos() }
                // 1つ目が取得の途中で止まっている間に、2つ目も呼ばれた状態にする
                runCurrent()
                api.callCount shouldBe 1

                gate.complete(Unit)
                first.await() shouldBe videos
                second.await() shouldBe videos
                api.callCount shouldBe 1
            }
        }
    }

    context("getVideo") {
        test("一覧にあるIDなら、その動画を返す") {
            val repository = VideoRepositoryImpl(FakeVideoApi(dtos))

            repository.getVideo(VideoId("sintel")) shouldBe videos[1]
        }

        test("一覧にないIDなら、nullを返す") {
            val repository = VideoRepositoryImpl(FakeVideoApi(dtos))

            repository.getVideo(VideoId("unknown")).shouldBeNull()
        }
    }
})
