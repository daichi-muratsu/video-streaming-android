# CLAUDE.md

<!-- 書き方の基準：コードから読み取れない重要なことだけ書く。200行未満にする -->

## コマンド

- 全体の確認：`./gradlew build`（ビルド・Lint・ユニットテスト・ktlint。CIも同じ内容を実行する）
- ktlintの指摘を直す：`./gradlew spotlessApply`
- 1つのテストクラスだけ実行する
  - `:core:domain`：`./gradlew :core:domain:test --tests "<クラスの完全修飾名>"`
  - Androidモジュール：`./gradlew :<モジュール>:testDebugUnitTest --tests "<クラスの完全修飾名>"`（例：`:feature:player`）

<!-- Androidモジュールの`--tests`でKotestのクラスを絞り込めるかは、まだ確かめていない。最初のテストを書いたときに確かめる -->

## 設計のルール

- 依存の向きは「UI層 → Domain層 ← Data層」。Repositoryのinterfaceは`:core:domain`、実装は`:core:data`に置く
- `:core:domain`は純粋なKotlinにする。Androidのクラスと依存関係を使わない
- featureモジュール同士は依存させない。画面遷移は`:app`でつなぐ
- 値オブジェクトは、1つの値なら`value class`、複数の値なら`data class`で作る（`VideoId`、`PlaybackPosition`、`License`など）
- ドメインルール（例：95%以上見たら、次は最初から再生する）は`:core:domain`に書く。ViewModelやRepositoryの実装に書かない
- UseCaseは、ドメインルールを使うときか、複数のRepositoryを組み合わせるときだけ作る。Repositoryを呼ぶだけのUseCaseは作らない

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
- ドメインルールの境界値は、プロパティベーステスト（kotest-property）でも確かめる

<!-- 最初のテストを書いたら、Kotestのテストの例を追記する -->

## コミット・PR

- タイトルはConventional Commitsの種類＋日本語
  - ブランチの中のコミット：「〜した」の形（例：`build: Spotlessをルートで適用した`）。本文は箇条書き
  - PR：名詞で終わる形（例：`ci: GitHub ActionsでCIを構築`）。Squash mergeで`main`のコミットのタイトルになるため
- コミット・PRのメッセージに、ほかのリポジトリのissueを`owner/repo#123`やURLの形で書かない。そのissueのページに、このリポジトリへの参照が表示されるため

## 日本語のルール

コードのコメント・コミット・PR・READMEは、日本語で書く。

- 技術用語は、公式ドキュメントで一般的な言い方を使う（例：プラグインを「適用する」、「依存関係を追加する」、「チェックする」、「取得する」、「バージョン」）
- 複合語は「の」でつながない（例：Androidアプリ、Kotlinバージョン）
- 一般的な語に英語を混ぜない（×ライブラリのPlugin）。固有名（Convention Plugin、Version Catalogなど）はそのまま書く
- 記号：並列は「・」、括弧は全角の（）、コードの要素はバッククォートで囲む
- コードのコメントは「なぜそうしたか」を書く。文末は常体（〜する）か体言止め
- KDocは、1段落目に何を表すかを1文で書き（例：「動画の長さを表す値オブジェクト。」）、空行のあとに理由を書く

## 注意点

- ライブラリは新しいメジャーバージョン（Room 3・Navigation 3・Coil 3・Retrofit 3・Kotest 6）を使う。前のメジャーバージョンのAPIで書かず、公式ドキュメントで確かめる
- Lintは`warningsAsErrors = true`。警告は直す。抑えるときは、`@SuppressLint`などに理由のコメントを付ける
- rcloneの設定ファイル（`~/.config/rclone/`）は読まない。R2の認証情報があり、`.claude/settings.json`のdenyで止めている
