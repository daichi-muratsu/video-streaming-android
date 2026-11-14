---
paths:
  - "feature/player/**"
---

# 再生

## Playerの扱い

- ExoPlayerは`PlayerModule`（HiltのModule）で`ApplicationContext`から作り、`PlayerViewModel`は`Player`として受け取って、`onCleared`で解放する（ViewModelが`Context`とExoPlayerの型を知らずに済み、テストでFakeに替えられるため）
- 画面とViewModelは`Player`インターフェースに対して書き、ExoPlayerの型に依存しない（後で`MediaController`に差し替えるため）
- 再生中か・再生位置などの再生の状態は`UiState`に入れず、画面がMedia3の状態ホルダー（`rememberPlayPauseButtonState`など）で`Player`から受け取る（`UiState`にも持つと、`Player`と食い違うおそれがあるため）
- `MediaItem`は`MediaItemFactory`で作る（本物は`HlsMediaItemFactory`）。`MediaItem.Builder.setUri`はAndroidの`Uri.parse`を呼び、JVMのユニットテストで動かないため
- Media3の`@UnstableApi`（`PlayerSurface`・`ContentFrame`・状態ホルダー・`EventLogger`など）は、`androidx.annotation.OptIn`で`@OptIn(UnstableApi::class)`を付け、理由のコメントを付ける。同じファイルでMaterial 3の`@ExperimentalMaterial3Api`も使うときは、Kotlinのほうを`@kotlin.OptIn(…)`と書く

## 再生位置

- 再生位置の保存は`PlaybackPositionTracker`に分ける。Trackerは`Player`を直接扱わず、再生位置と再生中かどうかだけの`PlaybackProgress`を受け取る（Fakeでテストするため）
- `Player`との変換（ミリ秒から`Duration`、`Player.Listener`から`Flow`）は、アダプターの`PlayerPlaybackProgress`だけで行う
- 再生位置は、再生中の10秒ごと・再生が止まったとき・画面を離れるとき（`onCleared`）に保存する。`MediaItem`を設定する前に画面を離れたときは保存しない（前に保存した位置を0で上書きするため）
- 再生を始める位置は`GetResumePositionUseCase`で取得し、`setMediaItem(item, startPositionMs)`に渡す。位置を取得してから`MediaItem`を設定し、取得に失敗したら設定せずにエラーにする（0から再生すると、次の保存で、読み出せなかった再生位置を上書きするため）
- 画面を離れたあとも続ける処理は、`@ApplicationScope`の`CoroutineScope`で行う（`viewModelScope`は`onCleared`で止まるため）。`Player`はメインスレッドで読み、書き込みだけをこのスコープで行う

## 関連動画

- 関連動画は、画面遷移にせず、同じ`Player`に`setMediaItem`して切り替える（`PlayerViewModel.selectVideo`）
- Trackerは作るときの動画のIDで保存するので、動画ごとに作り直す
- 前の動画の位置は`MediaItem`を替える前に保存する（替えたあとは、`Player`の位置が新しい動画の始める位置になるため）
- 取得し終えるまでは前の動画の表示を続け、読み込み中を挟まない

## OSとの連携

- 画面が見えなくなったら（`ON_STOP`）一時停止し、見えるようになったら（`ON_START`）、バックグラウンドに移る前に再生していたときだけ再生し直す
- Routeが`LifecycleEventEffect`で受け取ってViewModelの関数（`onBackground`・`onForeground`）を呼び、判定と`Player`の操作はViewModelで行う（Fakeでテストするため）。回転で作り直すとき（`isChangingConfigurations`）は呼ばない
- `ON_PAUSE`では止めない（画面分割で隣のアプリを操作しただけで止まるため）
- 再生していたかは`isPlaying`ではなく`playWhenReady`で判定する（バッファリング中は`isPlaying`が`false`になるため）。バックグラウンドで読み込みが終わったときは、再生を始めずに、戻ったときに始める
- オーディオフォーカスとイヤホンを抜いたときの一時停止は、ExoPlayerの設定（`setAudioAttributes`の`handleAudioFocus`・`setHandleAudioBecomingNoisy`）に任せ、自分で書かない
- 全画面かどうかは画面の向きで決める（横向きなら全画面）
- 全画面のボタンは向きを固定し、端末の傾きが固定した向きに追いついたら外す（すぐに外すと、横に持ったまま全画面を終えたときに、また全画面になるため）。追いついたかの判定は`OrientationLock`に分けてテストする。自動回転がオフのときは外さない
- 再生中だけ`Modifier.keepScreenOn()`で画面を点けたままにする（例：`KeepScreenOnWhilePlaying`）。一時停止中は、通常どおり画面を消す

## エラー・バッファリング

- 再生のエラーは、`rememberErrorState`で`Player`から受け取り、`UiState`に入れない（`UiState.Error`は、動画の情報の取得の失敗だけを表す）
- 再試行のボタンは、画面から`prepare()`と`play()`を呼ぶ（一時停止中にエラーになったときも、`prepare()`だけでは再生が始まらないため）
- エラーの案内は`PlaybackErrorKind.of`で`errorCode`から決める。通信状況を確かめる案内は、接続の失敗と時間切れ（`ERROR_CODE_IO_NETWORK_CONNECTION_FAILED`・`ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT`）だけに出す（サーバーのエラーやデコードの失敗は、通信状況を確かめても直らないため）
- 読み込みの失敗の自動の再試行と時間切れは、ExoPlayerの既定（`DefaultLoadErrorHandlingPolicy`・`DefaultHttpDataSource`）に任せる。自動の再試行のあとにエラーになったら、再試行のボタンで戻す
- バッファリングのスピナーは、`STATE_BUFFERING`かつ`playWhenReady`が`true`のときだけ出す（`PlayerView`の`SHOW_BUFFERING_WHEN_PLAYING`と同じ条件。一時停止中も読み込みを続けてバッファリングになるが、待っても再生は始まらないため）
