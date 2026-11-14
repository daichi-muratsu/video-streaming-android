---
paths:
  - "core/data/**"
---

# Data層

- クラスは`internal`にする。ほかのモジュールが使うのは、`:core:domain`のRepositoryのinterfaceだけにする
- DTO・Entityはドメインモデルと別のクラスにし、変換は変換元と同じファイルの拡張関数`toDomain()`で書く（例：`VideoDto.toDomain()`・`PlaybackPositionEntity.toDomain()`）。変換の失敗は例外のまま投げる
- DTOの項目に既定値を付けない（JSONに項目がないときに、変換の時点で失敗させるため）
- JSONの変換には`networkJson`（`ignoreUnknownKeys = true`）だけを使う
- Retrofitのinterface・DAOとRepositoryの間に、DataSourceのinterfaceを作らない（取得先が1つずつで、呼び出しを中継するだけのクラスになるため）。テストでは、Retrofitのinterface・DAOを直接Fakeにする
- DAOの関数は、DBの操作で名前を付ける（`upsert`・`getByVideoId`・`observeAll`）
- 上書きには`@Upsert`を使う（`@Insert(onConflict = REPLACE)`は、既存の行を削除してから挿入するため）
- Roomの列名には単位を入れる（例：`position_millis`）
- テーブルを変えるときは`version`を上げ、`core/data/schemas/`に書き出されるスキーマのJSONもコミットする
