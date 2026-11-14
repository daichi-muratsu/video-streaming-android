package io.github.daichimuratsu.videostreaming.feature.player

/**
 * 全画面のボタンで固定した画面の向きを表す。
 *
 * 端末の傾きが固定した向きに追いついたら、固定を外すために使う。外したあとは、端末を倒せば全画面が切り替わる
 */
internal enum class OrientationLock {
    Landscape,
    Portrait,
    ;

    /**
     * 端末の傾き（`OrientationEventListener`の角度）が、この向きに追いついたかを返す。
     *
     * 角度は、自然な向き（縦）が0度、左側が上で90度、右側が上で270度。向きがわからないときは-1。
     * 中心から±30度だけを追いついたとみなす。中間の角度で、固定を外すかの判定が入れ替わり続けないようにするため。
     * 縦は自然な向きだけにする。縦に固定するときは`USER_PORTRAIT`で、上下が逆の縦にはならないため
     */
    fun isReachedBy(degrees: Int): Boolean = when (this) {
        Landscape -> degrees in 60..120 || degrees in 240..300
        Portrait -> degrees in 0..30 || degrees in 330..359
    }
}
