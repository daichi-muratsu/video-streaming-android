package io.github.daichimuratsu.videostreaming.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.daichimuratsu.videostreaming.core.data.repository.PlaybackPositionRepositoryImpl
import io.github.daichimuratsu.videostreaming.core.data.repository.VideoRepositoryImpl
import io.github.daichimuratsu.videostreaming.core.domain.repository.PlaybackPositionRepository
import io.github.daichimuratsu.videostreaming.core.domain.repository.VideoRepository

/**
 * Domain層のRepositoryのinterfaceに、Data層の実装を結び付けるHiltのModule。
 *
 * UI層はinterfaceだけを受け取り、実装のクラスを知らずに済むようにするため
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository

    @Binds
    abstract fun bindPlaybackPositionRepository(
        impl: PlaybackPositionRepositoryImpl,
    ): PlaybackPositionRepository
}
