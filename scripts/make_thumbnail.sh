#!/usr/bin/env bash
#
# 元の動画の指定した時間の場面から、サムネイル（16:9、1280×720のJPEG）を作る。
#
# 使い方: scripts/make_thumbnail.sh <元の動画> <時間> <出力先のファイル>
# 例:     scripts/make_thumbnail.sh ~/video-work/source/big_buck_bunny_1080p_h264.mov 120 ~/video-work/thumbnails/big-buck-bunny.jpg
#
# <時間> は秒（120）か、時:分:秒（00:02:00）で指定する。
# 出力先のファイルがすでにあれば上書きする（場面を選び直すときのため）。
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "使い方: $0 <元の動画> <時間> <出力先のファイル>" >&2
  exit 1
fi

input="$1"
time="$2"
output="$3"

if [[ ! -f "$input" ]]; then
  echo "元の動画が見つかりません: $input" >&2
  exit 1
fi

mkdir -p "$(dirname "$output")"

# 真ん中を16:9に切り抜いてから、1280×720に縮める
# 横に長い動画（シネマサイズ）は左右を、縦に長い動画（4:3など）は上下を切る
ffmpeg -hide_banner -nostdin -loglevel error -y \
  -ss "$time" -i "$input" \
  -frames:v 1 \
  -vf "crop='min(iw,ih*16/9)':'min(ih,iw*9/16)',scale=1280:720" \
  -q:v 3 \
  "$output"

echo "完了: $output"
