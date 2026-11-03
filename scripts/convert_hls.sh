#!/usr/bin/env bash
#
# 元の動画を、3つの画質（360p / 720p / 1080p）のHLSに変換する。
#
# 使い方: scripts/convert_hls.sh <元の動画> <出力先のフォルダ>
# 例:     scripts/convert_hls.sh ~/video-work/source/big_buck_bunny_1080p_h264.mov ~/video-work/out/big-buck-bunny
#
# 出力:
#   <出力先>/master.m3u8              画質の一覧（アプリが最初に読む）
#   <出力先>/{360p,720p,1080p}/       画質ごとのプレイリストと、6秒ごとに分けたファイル
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "使い方: $0 <元の動画> <出力先のフォルダ>" >&2
  exit 1
fi

input="$1"
output_dir="$2"

if [[ ! -f "$input" ]]; then
  echo "元の動画が見つかりません: $input" >&2
  exit 1
fi
# 前の結果と混ざらないように、出力先がすでにあれば止める
if [[ -e "$output_dir" ]]; then
  echo "出力先がすでにあります。消してから実行してください: $output_dir" >&2
  exit 1
fi

# キーフレームを2秒ごとにするため、元の動画のfpsからフレーム数を計算する（24fpsなら48）
frame_rate=$(ffprobe -v error -select_streams v:0 -show_entries stream=r_frame_rate -of default=noprint_wrappers=1:nokey=1 "$input")
gop=$(awk -F/ '{ printf "%d", $1 / $2 * 2 + 0.5 }' <<< "$frame_rate")
echo "fps: $frame_rate → キーフレームの間隔: $gop フレーム"

mkdir -p "$output_dir"

ffmpeg -hide_banner -nostdin -i "$input" \
  -filter_complex "[0:v]split=3[v1][v2][v3];[v1]scale=640:-2[v1o];[v2]scale=1280:-2[v2o];[v3]scale=1920:-2[v3o]" \
  -map "[v1o]" -c:v:0 libx264 -b:v:0 800k  -maxrate:v:0 856k  -bufsize:v:0 1200k \
  -map "[v2o]" -c:v:1 libx264 -b:v:1 2800k -maxrate:v:1 2996k -bufsize:v:1 4200k \
  -map "[v3o]" -c:v:2 libx264 -b:v:2 5000k -maxrate:v:2 5350k -bufsize:v:2 7500k \
  -map 0:a:0 -map 0:a:0 -map 0:a:0 -c:a aac -b:a 128k -ac 2 \
  -pix_fmt yuv420p -preset veryfast -g "$gop" -keyint_min "$gop" -sc_threshold 0 \
  -f hls -hls_time 6 -hls_playlist_type vod \
  -hls_segment_filename "$output_dir/%v/seg_%03d.ts" \
  -master_pl_name master.m3u8 \
  -var_stream_map "v:0,a:0,name:360p v:1,a:1,name:720p v:2,a:2,name:1080p" \
  "$output_dir/%v/index.m3u8"

echo "完了: $output_dir/master.m3u8"
