package io.github.daichimuratsu.videostreaming.core.domain.repository

import io.github.daichimuratsu.videostreaming.core.domain.model.PlaybackPosition
import io.github.daichimuratsu.videostreaming.core.domain.model.VideoId
import kotlinx.coroutines.flow.Flow

/** 再生位置を保存・取得するRepository。 */
interface PlaybackPositionRepository {
    /**
     * 保存したすべての再生位置を、動画のIDごとに取得する。再生位置を保存していない動画は含まない。
     *
     * 保存するたびに、新しい値を出力する。再生画面から一覧画面に戻ったとき、
     * 保存の完了より先に読み込んでも、保存した再生位置を一覧画面に反映するため
     */
    fun observePositions(): Flow<Map<VideoId, PlaybackPosition>>

    /** 指定したIDの動画の再生位置を取得する。保存していない場合は`null`を返す。 */
    suspend fun getPosition(id: VideoId): PlaybackPosition?

    /** 再生位置を保存する。同じ動画の再生位置がすでにある場合は、上書きする。 */
    suspend fun savePosition(id: VideoId, position: PlaybackPosition)
}
