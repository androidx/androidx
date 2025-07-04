#!/bin/bash
set -e

# Function to get seconds from a date string
get_seconds() {
  local date_string="$1"
  local os
  os=$(uname -s)

  if [[ "$os" == "Darwin" ]]; then
    # macOS (BSD date)
    date -j -f "%Y-%m-%d %H:%M:%S" "$date_string" +%s 2>/dev/null
  elif [[ "$os" == "Linux" ]]; then
    # Linux (GNU date)
    date -d "$date_string" +%s 2>/dev/null
  else
    echo "Error: Unsupported operating system for get_seconds." >&2
    return 1
  fi
}

# Function to initialize start time
initialize_start_time() {
  date "+%Y-%m-%d %H:%M:%S"
}

# Function to record build metrics
record_build_metrics() {
  local start_time="$1"
  echo "Script execution started at $start_time"
  local end_time
  end_time="$(date "+%Y-%m-%d %H:%M:%S")"
  echo "Script execution completed at $end_time"

  local start_seconds
  local end_seconds
  start_seconds=$(get_seconds "$start_time")
  end_seconds=$(get_seconds "$end_time")

  if [ -z "$start_seconds" ] || [ -z "$end_seconds" ]; then
    echo "Error: Could not parse start or end time for metrics." >&2
    return 1
  fi

  local difference=$((end_seconds - start_seconds))
  local difference_formatted

  if [ "$difference" -gt 60 ]; then
    difference_formatted="$((difference / 60)) minutes"
  else
    difference_formatted="$difference seconds"
  fi

  if [ -z "$DIST_DIR" ]; then
      echo "Error: DIST_DIR is not set. Cannot write build metrics." >&2
      return 1
  fi

  echo "$difference_formatted" > "$DIST_DIR/build_metrics.txt"
  echo "Build metrics written to $DIST_DIR/build_metrics.txt: $difference_formatted"
}
