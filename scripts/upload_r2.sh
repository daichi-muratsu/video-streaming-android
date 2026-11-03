#!/usr/bin/env bash
#
# 変換した動画・サムネイル・JSONを、Cloudflare R2にアップロードする。
#
# 使い方: scripts/upload_r2.sh <作業フォルダ> [rcloneに渡すオプション...]
# 例:     scripts/upload_r2.sh ~/video-work
#         scripts/upload_r2.sh ~/video-work --dry-run   # 実際には送らず、送るファイルだけを表示する
#
# アップロードするもの:
#   <作業フォルダ>/out/<id>/...       → <送り先>/<id>/...（HLS）
#   <作業フォルダ>/thumbnails/<id>.jpg → <送り先>/<id>/thumbnail.jpg（作品ごとのフォルダにまとめる）
#   data/videos.json                  → <送り先>/videos.json
#
# 送り先は、rcloneのリモート名とバケット名（既定は r2:video-streaming）。
# 環境変数 R2_DEST で変えられる。認証情報は rclone の設定ファイルにあり、このスクリプトには書かない。
#
# rclone copy を使うので、R2にあってこちらにないファイルは消さない（sync と違い、消す操作はしない）。
# 2回目以降は、変わったファイルだけを送る。
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "使い方: $0 <作業フォルダ> [rcloneに渡すオプション...]" >&2
  exit 1
fi

work_dir="$1"
shift
dest="${R2_DEST:-r2:video-streaming}"
repo_root="$(cd "$(dirname "$0")/.." && pwd)"

hls_dir="$work_dir/out"
thumbnail_dir="$work_dir/thumbnails"
json="$repo_root/data/videos.json"

for path in "$hls_dir" "$thumbnail_dir"; do
  if [[ ! -d "$path" ]]; then
    echo "フォルダが見つかりません: $path" >&2
    exit 1
  fi
done
if [[ ! -f "$json" ]]; then
  echo "JSONが見つかりません: $json" >&2
  exit 1
fi

# ファイルの種類（Content-Type）を拡張子ごとに明示する。
# rcloneの自動判定では、.ts が別の種類（text/vnd.trolltech.linguist）と判定されることがあるため。
upload() {
  local source="$1" target="$2" pattern="$3" content_type="$4"
  shift 4
  echo "== $pattern → $target"
  rclone copy "$source" "$target" \
    --include "$pattern" \
    --header-upload "Content-Type: $content_type" \
    --stats-one-line --stats 5s --stats-log-level NOTICE \
    "$@"
}

# サムネイルは、手元では <作業フォルダ>/thumbnails/<id>.jpg にまとめて置き
# （変換の出力と分けて、場面を選び直しやすくするため）、R2では作品ごとのフォルダに送る
upload_thumbnails() {
  local file id found=false
  for file in "$thumbnail_dir"/*.jpg; do
    [[ -e "$file" ]] || continue
    found=true
    id="$(basename "$file" .jpg)"
    echo "== $(basename "$file") → $dest/$id/thumbnail.jpg"
    rclone copyto "$file" "$dest/$id/thumbnail.jpg" \
      --header-upload "Content-Type: image/jpeg" \
      "$@"
  done
  if [[ "$found" == false ]]; then
    echo "サムネイルが見つかりません: $thumbnail_dir/*.jpg" >&2
    exit 1
  fi
}

# アプリが存在しないファイルを読みにいかないように、
# 細切れの動画 → プレイリスト → サムネイル → JSON の順に送る
upload "$hls_dir" "$dest" "*.ts" "video/mp2t" "$@"
upload "$hls_dir" "$dest" "*.m3u8" "application/vnd.apple.mpegurl" "$@"
upload_thumbnails "$@"
upload "$(dirname "$json")" "$dest" "videos.json" "application/json" "$@"

echo "完了: $dest"
