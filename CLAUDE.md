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

<!-- 一覧画面を作ったら、実際のファイル名とUiStateの例を追記する -->

### 再生

- ExoPlayerはPlayerViewModelで持ち、`onCleared`で解放する。ViewModelに渡すContextは`ApplicationContext`にする
- 画面とViewModelは`Player`インターフェースに対して書き、ExoPlayerの型に依存しない（後で`MediaController`に差し替えるため）
- 再生位置の保存は`PlaybackPositionTracker`に分ける。Trackerは`Player`を直接扱わず、小さなinterfaceを受け取る（Fakeでテストするため）

<!-- 再生画面を作ったら、実際のファイル名を追記する -->

## テスト

- Kotest（FunSpec）で書く。JUnit 4は、Androidの環境が必要なもの（Compose UI Test、RoomのDAO）だけに使う
- モックのライブラリは使わず、Fakeを書いて差し替える
- ViewModelのテストは、kotlinx-coroutines-testとTurbineを使う
- ドメインルールは、結果が同じになる範囲ごとに、両端の値（境界値）と中の値（代表値）をテストに書いて確かめる
- Fakeは、使うモジュールの`src/test`の`fake`パッケージに置く（例：`:core:data`の`FakeVideoApi`）
- ほかのモジュールのテストでも使うFakeは、`src/testFixtures`の`fake`パッケージに置く（例：`:core:domain`の`FakePlaybackPositionRepository`）。`src/test`の中身は、ほかのモジュールから参照できないため。使う側は`testImplementation(testFixtures(project(":core:domain")))`で依存関係を追加する
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
- コード・コミットの文は、正確さを優先した書き言葉で書く。話し言葉や、説明のための言い換え・例えを使わない
- あいまいな言い方を使わない
- KDocは、1段落目に何を表すかを1文で書き（例：「動画の長さを表す値オブジェクト。」）、空行のあとに理由を書く

## 注意点

- ライブラリは新しいメジャーバージョン（Room 3・Navigation 3・Coil 3・Retrofit 3・Kotest 6）を使う。前のメジャーバージョンのAPIで書かず、公式ドキュメントで確かめる
- Lintは`warningsAsErrors = true`。警告は直す。抑えるときは、`@SuppressLint`などに理由のコメントを付ける
- rcloneの設定ファイル（`~/.config/rclone/`）は読まない。R2の認証情報があり、`.claude/settings.json`のdenyで止めている
