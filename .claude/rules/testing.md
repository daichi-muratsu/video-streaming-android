---
paths:
  - "**/src/test/**"
  - "**/src/testFixtures/**"
  - "**/src/androidTest/**"
  - "core/testing/**"
---

# テスト

## ViewModel・Flow

- ViewModelのテストは、kotlinx-coroutines-testとTurbineを使う
- ViewModelのテストでは、`MainDispatcherExtension`（`:core:testing`）を`extension(…)`で適用してMainを差し替え、Turbineで受け取るテストの本体を`runTest`で包む（包まないと、テスト用のディスパッチャーに渡した処理が実行されず、次の値を待ち続けるため。例：`VideoListViewModelTest`）
- 読み込み中を挟むかを確かめるときは、Fakeの取得に時間をかける（`FakeVideoRepository.fetchDelay`）。すぐに返すと、`StateFlow`が途中の値を飛ばし、テストが見逃すため
- 一定の間隔の処理は、仮想の時間で確かめる。`advanceTimeBy`のあとに`runCurrent`を呼ぶ（`advanceTimeBy`は、ちょうどその時刻に予定された処理を実行しないため。例：`PlaybackPositionTrackerTest`）
- ViewModelの`onCleared`は、`ViewModelStore`に`ViewModelProvider.create(…)`で入れ、`ViewModelStore.clear()`で起こす（例：`PlayerViewModelTest`）。`lifecycle-viewmodel-testing`の`viewModelScenario`は、中でAndroidの`Bundle`を使い、Androidのモジュールのユニットテストで失敗するため

## Fake・テスト用の道具の置き場所

- Fakeは、使うモジュールの`src/test`の`fake`パッケージに置く（例：`:core:data`の`FakeVideoApi`）
- ほかのモジュールのテストでも使うFakeは、`src/testFixtures`の`fake`パッケージに置く（例：`:core:domain`の`FakePlaybackPositionRepository`）。`src/test`の中身は、ほかのモジュールから参照できないため。使う側は`testImplementation(testFixtures(project(":core:domain")))`で依存関係を追加する
- テスト用の道具（Fakeではないもの）は`:core:testing`に置く（例：`MainDispatcherExtension`）。featureモジュールにはConvention Pluginで`testImplementation`に追加してある

## PlayerのFake

- `Player`のFakeは、`media3-test-utils`の`StubPlayer`を継承し、使う関数だけを上書きする（例：`FakePlayer`）
- プロパティの名前は`Player`の関数と重ねない（`var mediaItem`は`setMediaItem`と衝突する）
- `isPlaying`は`BasePlayer`の`final`の関数なので、元になる`getPlaybackState`・`getPlayWhenReady`・`getPlaybackSuppressionReason`を上書きする
- `setMediaItem(MediaItem, Long)`は`BasePlayer`の中で`setMediaItems(List, Int, Long)`を呼ぶので、こちらを上書きする

## 端末のテスト・JUnit 4

- RoomのDAOのテストは`src/androidTest`に置き、メモリ上のデータベースとアプリと同じ`BundledSQLiteDriver`で確かめる（例：`PlaybackPositionDaoTest`）。公式がRobolectricでのテストを推奨していないため
- JUnit 4のテストの関数名は、日本語をバッククォートで囲む（ktlintの`function-naming`が、囲まない日本語の関数名を指摘するため）。空白は入れない（空白を含む関数名は、AndroidではAPI level 30以上でしか使えず、`minSdk`は26のため）

## 書き方の例

- `WatchStatusTest`（範囲ごとに`context`で分ける）
- `GetVideoListWithWatchStatusUseCaseTest`（`Flow`をTurbineで確かめる）
- `VideoRepositoryImplTest`（Fakeの呼ばれた回数でキャッシュを確かめる）
