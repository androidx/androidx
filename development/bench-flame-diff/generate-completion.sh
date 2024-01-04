#!/usr/bin/env bash

function generate_completion_files() {
    for shell in bash fish zsh; do
        dst=completion_$shell.sh
        (export _BENCH_FLAME_DIFF_COMPLETE=$shell; ./bench-flame-diff.sh | sed -E "s_bench-flame-diff( |$)_bench-flame-diff.sh\1_" >| $dst)
        echo "Generated $dst"
    done
}

generate_completion_files
