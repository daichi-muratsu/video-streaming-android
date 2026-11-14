# CLAUDE.md

<!-- 書き方の基準：コードから読み取れない重要なことだけ書く。200行未満にする。
     一部のモジュールだけで使うルールは`.claude/rules/`に`paths`を付けて置く -->

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
- ドメインルール（例：90%以上再生したら、次は最初から再生する）は`:core:domain`に書く。ViewModelやRepositoryの実装に書かない
- UseCaseは、ドメインルールを使うときか、複数のRepositoryを組み合わせるときだけ作る。Repositoryを呼ぶだけのUseCaseは作らない
- 再生位置・長さは`Duration`で持つ。秒・ミリ秒との変換は、取得元との境界（Data層でJSONを変換するところ、`Player`の値を変換するところ）だけで行う
- ネットにつなぐモジュールは、自分のマニフェストに`INTERNET`の権限を書く（例：`:core:data`）。権限はアプリ全体に与えられるので、書き忘れても動いてしまい、気づけないため
- 層・モジュールごとのルールは`.claude/rules/`に置く（`domain`・`data`・`ui`・`player`・`testing`・`gradle`）。該当するファイルを読み書きすると読み込まれる

## テスト

- Kotest（FunSpec）で書く。JUnit 4は、Androidの環境が必要なもの（Compose UI Test、RoomのDAO）だけに使う
- モックのライブラリは使わず、Fakeを書いて差し替える
- ドメインルールは、結果が同じになる範囲ごとに、両端の値（境界値）と中の値（代表値）をテストに書いて確かめる

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
- rcloneの設定ファイル（`~/.config/rclone/`）は読まない。R2の認証情報があり、`.claude/settings.json`のdenyで止めている
