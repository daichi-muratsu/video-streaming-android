# CLAUDE.md

<!-- 書き方の基準：コードから読み取れない重要なことだけ書く。200行未満にする -->

## コマンド

- 全体の確認：`./gradlew build`（ビルド・Lint・ユニットテスト・ktlint。CIも同じ内容を実行する）
- ktlintの指摘を直す：`./gradlew spotlessApply`
- 1つのテストクラスだけ実行する
  - `:core:domain`：`./gradlew :core:domain:test --tests "<クラスの完全修飾名>"`
  - Androidモジュール：`./gradlew :<モジュール>:testDebugUnitTest --tests "<クラスの完全修飾名>"`（例：`:core:data`）
- 端末のテスト（`src/androidTest`）：`./gradlew :<モジュール>:connectedDebugAndroidTest`（実機かエミュレーターが必要。CIでは実行しないので、push前に手元で実行する）

## 設計のルール

- 依存の向きは「UI層 → Domain層 ← Data層」。Repositoryのinterfaceは`:core:domain`、実装は`:core:data`に置く
- `:core:domain`は純粋なKotlinにする。Androidのクラスと依存関係を使わない
- featureモジュール同士は依存させない。画面遷移は`:app`でつなぐ
- 値オブジェクトは、1つの値なら`value class`、複数の値なら`data class`で作る（`VideoId`、`PlaybackPosition`、`License`など）
- ドメインルール（例：90%以上再生したら、次は最初から再生する）は`:core:domain`に書く。ViewModelやRepositoryの実装に書かない
- UseCaseは、ドメインルールを使うときか、複数のRepositoryを組み合わせるときだけ作る。Repositoryを呼ぶだけのUseCaseは作らない
- ネットにつなぐモジュールは、自分のマニフェストに`INTERNET`の権限を書く（例：`:core:data`）。権限はアプリ全体に与えられるので、書き忘れても動いてしまい、気づけないため
- モジュールの依存関係は`implementation`にする。公開する関数の引数・戻り値にそのモジュールの型が出てくるときだけ`api`にする（例：featureモジュールの`:core:domain`。`VideoListRoute`の引数に`VideoId`が出てくるため）

### Domain層

- パッケージは`model`（値オブジェクト・エンティティ・ドメインルール）・`repository`（interface）・`usecase`に分ける
- 値オブジェクトは、正しく作れば起きない値（負の再生位置など）を`init`の`require`で止める
- 再生位置・長さは`Duration`で持つ。秒・ミリ秒との変換は、取得元との境界（Data層でJSONを変換するところ、`Player`の値を変換するところ）だけで行う
- 視聴状態の判定は`WatchStatus.of`の1か所で行う。一覧画面の表示と再生を始める位置は、どちらもこの結果を使う
- Repositoryは、1回だけの操作を`suspend`、変わり続けるデータを`Flow`にする。`Flow`を返す関数は`observe`で始める。取得の失敗は例外で伝え、該当するデータがない場合は`null`を返す
- UseCaseは`@Inject constructor`と`operator fun invoke`で作る（例：`GetResumePositionUseCase`）

### Data層

- パッケージは取得先ごとに`network`（Retrofitのinterface・DTO）・`database`（RoomのDatabase・DAO・Entity）・`repository`（Repositoryの実装）・`di`（HiltのModule）に分ける
- クラスは`internal`にする。ほかのモジュールが使うのは、`:core:domain`のRepositoryのinterfaceだけにする
- DTO・Entityはドメインモデルと別のクラスにし、変換は変換元と同じファイルの拡張関数`toDomain()`で書く（例：`VideoDto.toDomain()`、`PlaybackPositionEntity.toDomain()`）。変換の失敗は例外のまま投げる
- DTOの項目に既定値を付けない。JSONに項目がないときに、変換の時点で失敗させるため。JSONの変換には`networkJson`（`ignoreUnknownKeys = true`）だけを使う
- Retrofitのinterface・DAOとRepositoryの間に、DataSourceのinterfaceを作らない。取得先が1つずつで、呼び出しを中継するだけのクラスになるため。テストでは、Retrofitのinterface・DAOを直接Fakeにする
- DAOの関数は、DBの操作で名前を付ける（`upsert`・`getByVideoId`・`observeAll`）。上書きには`@Upsert`を使う（`@Insert(onConflict = REPLACE)`は、既存の行を削除してから挿入するため）
- Roomの列名には単位を入れる（例：`position_millis`）。テーブルを変えるときは`version`を上げ、`core/data/schemas/`に書き出されるスキーマのJSONもコミットする

### UI層

- ViewModelは`StateFlow<UiState>`を公開し、画面からの操作は関数で受け取る
- `UiState`で、読み込み中・成功・エラー・空の状態を表す
- 画面は`XxxRoute`と`XxxScreen`に分ける。`XxxRoute`がViewModelとつなぎ、`XxxScreen`は状態を受け取って表示するだけにする
- `UiState`は`sealed interface`で作る（例：`VideoListUiState`）。「読み込み中かつエラー」のような組み合わせを作れないようにするため。空の一覧は`Empty`で表し、`Success`に入れない
- ViewModelは、UseCaseの`Flow`を`stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), …)`で公開し、画面は`collectAsStateWithLifecycle`で受け取る。画面が見えないあいだは受け取りを止めるため。`init`で読み込みを始めない（例：`VideoListViewModel`）
- 1回だけ取得するデータ（`suspend`の取得）は、`flow { emit(…) }`にして`SharingStarted.Lazily`で公開する。止めても減らせる処理がなく、`WhileSubscribed`だと画面に戻ったときに取得し直すため（例：`PlayerViewModel`）。`Lazily`は受け取る側が現れるまで始めないので、表示に使わなくても画面で受け取る
- 失敗は`Flow`の`catch`で`UiState`にする。`try`〜`catch (e: Exception)`は`CancellationException`まで受け止めるので使わない。再読み込みは、合図の`Flow`から`flatMapLatest`で作り直し、`catch`は`flatMapLatest`の内側に置く。外側に置くと、失敗したときに合図を受け取る`Flow`ごと終わり、再読み込みが効かなくなるため
- ドメインモデルを画面用のクラスに写さない。組み合わせ・判定はDomain層で済ませ、表示の形式（再生時間の`9:56`など）は画面側の関数で整える（例：`formatDuration`）。featureモジュール同士で使う表示の関数は`:core:ui`に置く
- `Player`などから状態を読む部品と、値だけを受け取って表示する部品に分ける（例：`PlayerControlsOverlay`と`PlayerControls`）。値だけを受け取る部品を、プレビューとUIテストで確かめるため
- 文言はfeatureモジュールの`strings.xml`に置き、名前の先頭にモジュールの接頭辞を付ける（例：`video_list_`・`player_`・`core_ui_`）。リソースはアプリ全体で1つにまとめられ、名前が重なると上書きされるため
- アイコンは、Material SymbolsのXMLを`res/drawable`に置く（例：`player_ic_play_arrow`）。`material-icons`のライブラリは、公式がすすめていないので使わない。XMLの`android:tint="?attr/colorControlNormal"`はAppCompatのテーマの属性でリソースのリンクが失敗するので外し、色は`Icon`の`tint`で付ける
- テーマは`:core:ui`の`VideoStreamingTheme`を使う。プレビューは`@PreviewLightDark`で明暗の両方を作り、`VideoStreamingTheme(dynamicColor = false)`で囲む（壁紙で色が変わらないようにするため）。プレビューのデータは1か所にまとめる（例：`previewVideos`）
- 画面遷移は`:app`の`VideoStreamingNavDisplay`でつなぐ。キーは`@Serializable`の`NavKey`にし、`rememberViewModelStoreNavEntryDecorator()`でViewModelを画面ごとに作る
- キーの引数（動画のIDなど）は、Hiltのassisted injectionでViewModelに渡す（`hiltViewModel<VM, VM.Factory>(creationCallback = …)`。例：`PlayerKey`・`PlayerRoute`）。キーとassisted injectionの引数は`String`にする。キーに`value class`を入れるとDomain層に`kotlinx.serialization`の注釈を持ち込み、assisted injectionの引数に`value class`を使うとKotlinが関数の名前を書き換えて、Daggerが`Factory`を作れないため

### 再生

- ExoPlayerは`:feature:player`のHiltのModule（`PlayerModule`）で`ApplicationContext`から作り、`PlayerViewModel`は`Player`として受け取って、`onCleared`で解放する。ViewModelが`Context`とExoPlayerの型を知らずに済み、テストでFakeに替えられるため
- 画面とViewModelは`Player`インターフェースに対して書き、ExoPlayerの型に依存しない（後で`MediaController`に差し替えるため）
- 再生中か・再生位置などの再生の状態は`UiState`に入れず、画面がMedia3の状態ホルダー（`rememberPlayPauseButtonState`など）で`Player`から受け取る。`UiState`にも持つと、`Player`と食い違うおそれがあるため
- `MediaItem`は`MediaItemFactory`で作る（本物は`HlsMediaItemFactory`）。`MediaItem.Builder.setUri`はAndroidの`Uri.parse`を呼び、JVMのユニットテストで動かないため
- 再生位置の保存は`PlaybackPositionTracker`に分ける。Trackerは`Player`を直接扱わず、小さなinterfaceを受け取る（Fakeでテストするため）
- 再生画面のファイル：`PlayerScreen`（Route・Screen・枠・全画面）・`PlayerControls`（操作ボタン）・`VideoDetails`（説明の箱）・`PlayerStatus`（読み込み中・エラー・該当なし）

## テスト

- Kotest（FunSpec）で書く。JUnit 4は、Androidの環境が必要なもの（Compose UI Test、RoomのDAO）だけに使う
- モックのライブラリは使わず、Fakeを書いて差し替える
- ViewModelのテストは、kotlinx-coroutines-testとTurbineを使う
- ViewModelのテストでは、`MainDispatcherExtension`（`:core:testing`）を`extension(…)`で適用してMainを差し替え、Turbineで受け取るテストの本体を`runTest`で包む。包まないと、テスト用のディスパッチャーに渡した処理が実行されず、次の値を待ち続けるため（例：`VideoListViewModelTest`）
- 読み込み中を挟むかを確かめるときは、Fakeの取得に時間をかける（`FakeVideoRepository.fetchDelay`）。すぐに返すと、`StateFlow`が途中の値を飛ばし、テストが見逃すため
- ドメインルールは、結果が同じになる範囲ごとに、両端の値（境界値）と中の値（代表値）をテストに書いて確かめる
- Fakeは、使うモジュールの`src/test`の`fake`パッケージに置く（例：`:core:data`の`FakeVideoApi`）
- ほかのモジュールのテストでも使うFakeは、`src/testFixtures`の`fake`パッケージに置く（例：`:core:domain`の`FakePlaybackPositionRepository`）。`src/test`の中身は、ほかのモジュールから参照できないため。使う側は`testImplementation(testFixtures(project(":core:domain")))`で依存関係を追加する
- テスト用の道具（Fakeではないもの）は`:core:testing`に置く（例：`MainDispatcherExtension`）。featureモジュールにはConvention Pluginで`testImplementation`に追加してある
- `Player`のFakeは、`media3-test-utils`の`StubPlayer`を継承し、使う関数だけを上書きする（例：`FakePlayer`）。プロパティの名前は`Player`の関数と重ねない（`var mediaItem`は`setMediaItem`と衝突する）
- ViewModelの`onCleared`は、`ViewModelStore`に`ViewModelProvider.create(…)`で入れ、`ViewModelStore.clear()`で起こす（例：`PlayerViewModelTest`）。`lifecycle-viewmodel-testing`の`viewModelScenario`は、中でAndroidの`Bundle`を使い、Androidのモジュールのユニットテストで失敗するため
- RoomのDAOのテストは`src/androidTest`に置き、メモリ上のデータベースとアプリと同じ`BundledSQLiteDriver`で確かめる（例：`PlaybackPositionDaoTest`）。公式がRobolectricでのテストを推奨していないため
- JUnit 4のテストの関数名は、日本語をバッククォートで囲む（ktlintの`function-naming`が、囲まない日本語の関数名を指摘するため）。空白は入れない（空白を含む関数名は、AndroidではAPI level 30以上でしか使えず、`minSdk`は26のため）
- 書き方の例：`WatchStatusTest`（範囲ごとに`context`で分ける）、`GetVideoListWithWatchStatusUseCaseTest`（`Flow`をTurbineで確かめる）、`VideoRepositoryImplTest`（Fakeの呼ばれた回数でキャッシュを確かめる）

## コミット・PR

- タイトルはConventional Commitsの種類＋日本語
  - ブランチの中のコミット：「〜した」の形（例：`build: Spotlessをルートで適用した`）。本文は箇条書き
  - PR：名詞で終わる形（例：`ci: GitHub ActionsでCIを構築`）。Squash mergeで`main`のコミットのタイトルになるため
- コミット・PRのメッセージに、ほかのリポジトリのissueを`owner/repo#123`やURLの形で書かない。そのissueのページに、このリポジトリへの参照が表示されるため

## 日本語のルール

コードのコメント・コミット・PR・READMEは、日本語で書く。

- 技術用語は、公式ドキュメントで一般的な言い方を使う（例：プラグインを「適用する」、「依存関係を追加する」、「チェックする」、「取得する」、「バージョン」）。その場で新しい言い方を作らない
- 複合語は「の」でつながない（例：Androidアプリ、Kotlinバージョン）
- 一般的な語に英語を混ぜない（×ライブラリのPlugin）。固有名（Convention Plugin、Version Catalogなど）はそのまま書く
- 記号：並列は「・」、括弧は全角の（）、コードの要素はバッククォートで囲む
- コードのコメントは「なぜそうしたか」を書く。文末は常体（〜する）か体言止め
- コード・コミットの文は、正確さを優先した書き言葉で書く。話し言葉や、説明のための言い換え・例えを使わない。テストの名前も同じ（×ないIDなら → ○該当する動画がなければ）
- あいまいな言い方を使わない
- KDocは、1段落目に何を表すかを1文で書き（例：「動画の長さを表す値オブジェクト。」）、空行のあとに理由を書く

## 注意点

- ライブラリは新しいメジャーバージョン（Room 3・Navigation 3・Coil 3・Retrofit 3・Kotest 6）を使う。前のメジャーバージョンのAPIで書かず、公式ドキュメントで確かめる
- Lintは`warningsAsErrors = true`。警告は直す。抑えるときは、`@SuppressLint`などに理由のコメントを付ける
- Media3の`@UnstableApi`（`PlayerSurface`・`ContentFrame`・状態ホルダー・`EventLogger`など）は、`androidx.annotation.OptIn`で`@OptIn(UnstableApi::class)`を付け、理由のコメントを付ける。同じファイルでMaterial 3の`@ExperimentalMaterial3Api`も使うときは、Kotlinのほうを`@kotlin.OptIn(…)`と書く
- Lintの`UnusedResources`は、ライブラリのモジュール（`:core:ui`・`:feature:*`）では報告されない（`skip-libraries`の既定値が`true`）。使っていない文言があっても、Lintでは見つからない
- rcloneの設定ファイル（`~/.config/rclone/`）は読まない。R2の認証情報があり、`.claude/settings.json`のdenyで止めている
