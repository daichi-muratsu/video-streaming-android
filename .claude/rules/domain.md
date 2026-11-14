---
paths:
  - "core/domain/**"
---

# Domain層

- 値オブジェクトは、1つの値なら`value class`、複数の値なら`data class`で作る（例：`VideoId`・`PlaybackPosition`・`License`）
- 値オブジェクトは、正しく作れば起きない値（負の再生位置など）を`init`の`require`で止める
- 視聴状態の判定は`WatchStatus.of`の1か所で行う。一覧画面の表示と再生を始める位置は、どちらもこの結果を使う
- 「視聴済み」になったことを別に保存しない。視聴済みの動画を見直すと、新しい再生位置で上書きし、表示も「視聴途中」などに変わる（表示と再生を始める位置を食い違わせないため）
- Repositoryは、1回だけの操作を`suspend`、変わり続けるデータを`Flow`にする。`Flow`を返す関数は`observe`で始める
- Repositoryは、取得の失敗を例外で伝え、該当するデータがない場合は`null`を返す
- UseCaseは`@Inject constructor`と`operator fun invoke`で作る（例：`GetResumePositionUseCase`）
