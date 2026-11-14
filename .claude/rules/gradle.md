---
paths:
  - "**/build.gradle.kts"
  - "settings.gradle.kts"
  - "build-logic/**"
  - "gradle/libs.versions.toml"
---

# ビルドの設定

- モジュールの依存関係は`implementation`にする。公開する関数の引数・戻り値にそのモジュールの型が出てくるときだけ`api`にする（例：featureモジュールの`:core:domain`。`VideoListRoute`の引数に`VideoId`が出てくるため）
