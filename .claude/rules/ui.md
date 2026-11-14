---
paths:
  - "feature/**"
  - "core/ui/**"
  - "app/**"
---

# UI層

## ViewModel・UiState

- ViewModelは`StateFlow<UiState>`を公開し、画面からの操作は関数で受け取る
- `UiState`は`sealed interface`で作り、読み込み中・成功・エラー・空を表す（例：`VideoListUiState`）。「読み込み中かつエラー」のような組み合わせを作れないようにするため。空の一覧は`Empty`で表し、`Success`に入れない
- UseCaseの`Flow`は`stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), …)`で公開し、画面は`collectAsStateWithLifecycle`で受け取る（画面が見えないあいだは受け取りを止めるため）。`init`で読み込みを始めない（例：`VideoListViewModel`）
- 1回だけ取得するデータ（`suspend`の取得）は、`flow { emit(…) }`にして`SharingStarted.Lazily`で公開する（止めても減らせる処理がなく、`WhileSubscribed`だと画面に戻ったときに取得し直すため。例：`PlayerViewModel`）。`Lazily`は受け取る側が現れるまで始めないので、表示に使わなくても画面で受け取る
- 失敗は`Flow`の`catch`で`UiState`にする。`try`〜`catch (e: Exception)`は`CancellationException`まで受け止めるので使わない
- 再読み込みは、合図の`Flow`から`flatMapLatest`で作り直し、`catch`は`flatMapLatest`の内側に置く（外側に置くと、失敗したときに合図を受け取る`Flow`ごと終わり、再読み込みが効かなくなるため）

## 画面・部品

- 画面は`XxxRoute`と`XxxScreen`に分ける。`XxxRoute`がViewModelとつなぎ、`XxxScreen`は状態を受け取って表示するだけにする
- ドメインモデルを画面用のクラスに写さない。組み合わせ・判定はDomain層で済ませ、表示の形式（再生時間の`9:56`など）は画面側の関数で整える（例：`formatDuration`）
- featureモジュール同士で使う表示の関数・部品は`:core:ui`に置く（例：`VideoCard`）
- `Player`などから状態を読む部品と、値だけを受け取って表示する部品に分ける（例：`PlayerControlsOverlay`と`PlayerControls`）。値だけを受け取る部品を、プレビューとUIテストで確かめるため

## リソース・テーマ

- 文言はfeatureモジュールの`strings.xml`に置き、名前の先頭にモジュールの接頭辞を付ける（例：`video_list_`・`player_`・`core_ui_`）。リソースはアプリ全体で1つにまとめられ、名前が重なると上書きされるため
- Lintの`UnusedResources`は、ライブラリのモジュール（`:core:ui`・`:feature:*`）では報告されない（`skip-libraries`の既定値が`true`）。使っていない文言があっても、Lintでは見つからない
- アイコンは、Material SymbolsのXMLを`res/drawable`に置く（例：`player_ic_play_arrow`）。`material-icons`のライブラリは、公式がすすめていないので使わない
- アイコンのXMLの`android:tint="?attr/colorControlNormal"`は外し、色は`Icon`の`tint`で付ける（AppCompatのテーマの属性で、リソースのリンクが失敗するため）
- テーマは`:core:ui`の`VideoStreamingTheme`を使う
- プレビューは`@PreviewLightDark`で明暗の両方を作り、`VideoStreamingTheme(dynamicColor = false)`で囲む（壁紙で色が変わらないようにするため）。プレビューのデータは1か所にまとめる（例：`previewVideos`）

## 画面遷移

- 画面遷移は`:app`の`VideoStreamingNavDisplay`でつなぐ。キーは`@Serializable`の`NavKey`にし、`rememberViewModelStoreNavEntryDecorator()`でViewModelを画面ごとに作る
- キーの引数（動画のIDなど）は、Hiltのassisted injectionでViewModelに渡す（`hiltViewModel<VM, VM.Factory>(creationCallback = …)`。例：`PlayerKey`・`PlayerRoute`）
- キーとassisted injectionの引数は`String`にする（`value class`は、キーではDomain層に`kotlinx.serialization`の注釈を持ち込み、assisted injectionではKotlinが関数の名前を書き換えて、Daggerが`Factory`を作れないため）
- キーの引数は`SavedStateHandle`に保存しない（プロセスが終了されても、`rememberNavBackStack`が保存したキーから戻るため）。画面の中で変わる値（関連動画で替えた動画のIDなど）は、キーから戻らないので`SavedStateHandle`に保存する（例：`PlayerViewModel`の`getStateFlow`）
