#!/bin/bash

file_count=$1
file_size=$2
size_unit=$3

function check_input_param() {
  if [[ "a" == "a"$file_count || "a" == "a"$file_size || "a" == "a"$size_unit ]]; then
    echo "Error param input !"
    echo "Type in like this: $0 [file_count] [file-size] [size-unit]"
    echo "param list as follow:"
    echo "[file_count]: Enter the maximum number of files."
    echo "[file_size]: The file max size of output file, which must be an integer."
    echo "[size_unit]: Only support K/M/G. They mean xxxKB/xxxMB/xxxGB."
    exit
  fi
}

function check_file_size_if_integer() {
  if [ -n "$file_size" -a "$file_size" = "${file_size//[^0-9]/}" ]; then
    echo "file_size=$file_size"
  else
    echo "[file_size] error: The file size of output file, which must be an integer."
    exit
  fi
}

function check_size_unit() {
  if [[ "K" != $size_unit && "M" != $size_unit && "G" != $size_unit ]]; then
    echo "[size_unit] error: Only support K/M/G. They mean xxxKB/xxxMB/xxxGB."
    exit
  fi
}

function create_random_file() {
  tmp_out_file_name="${out_file_name}.tmp"
  dd if=/dev/urandom of=$tmp_out_file_name bs=1$size_unit count=$1 conv=notrunc
  mv $tmp_out_file_name $out_file_name
}

check_input_param
check_file_size_if_integer
check_size_unit

suffix=(".zip" ".wmx" ".mp3" ".txt" ".mp4" ".avi" ".tar" ".so" ".dta" "")
suffix_length=${#suffix[@]}
declare -i count=0
declare -i tmp_file_count=$(expr $RANDOM % $file_count + 1)
echo "create files:$tmp_file_count"
while [ $count -lt $tmp_file_count ]; do
  len=$(expr $RANDOM % 10 + 1)
  out_file_name="$(openssl rand -base64 128 | md5 | cut -c1-$len )${suffix[$(expr $len % $suffix_length )]}"
  tmp_file_size=$(expr $RANDOM % $file_size + 1)
  create_random_file $tmp_file_size
  ((count++))
done
