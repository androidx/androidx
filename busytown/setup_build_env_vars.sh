#!/bin/bash
set -e

# Function to setup common environment variables
setup_build_env_vars() {
  CHECKOUT_DIR="$(cd ../../.. && pwd)"
  export CHECKOUT_DIR
  OUT_DIR="$CHECKOUT_DIR/out"
  if [ "$DIST_DIR" == "" ]; then
    DIST_DIR="$OUT_DIR/dist"
  fi
  export DIST_DIR
}
