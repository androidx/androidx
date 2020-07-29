#!/bin/bash

BASE_URL="https://plugins.jetbrains.com"
SCRIPT_PATH="$( cd "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
STUDIO_DIR=`ls -d $SCRIPT_PATH/../studio/*`
STUDIO_BUILD=`cat "$STUDIO_DIR"/android-studio/product-info.json \
  | jq -r '.productCode + "-" + .buildNumber'`

if [[ -z "$1" ]] || [[ "$1" == "help" ]]; then
  echo -n \
"usage: plugins <command> [<args>]

A CLI for JB's plugin marketplace that supports querying and installing plugins that support the current version of Studio.

Commands:
  help        	Display this help text
  ls [<query>]	Query plugin marketplace by plugin name for plugin ids supporting the current version of Studio
  install <id>	Download and install plugins by plugin id
"
elif [[ $1 == "ls" ]]; then
  QUERY="$2"
  curl -s "$BASE_URL/plugins/list?build=$STUDIO_BUILD"     \
    | egrep -o "<name>[^<]+</name><id>[a-zA-Z0-9\.]+</id>" \
    | sed 's/<id>/id: /g'                                  \
    | sed 's/<\/id>//g'                                    \
    | sed 's/<name>/name: /g'                              \
    | sed 's/<\/name>/>/g'                                 \
    | grep -i "$QUERY"                                     \
    | column -t -s\>
elif [[ $1 == "install" ]]; then
  ID="$2"
  wget "$BASE_URL/pluginManager?action=download&id=$ID&build=$STUDIO_BUILD" -O ~/.dustinlam_plugins_download \
    && unzip -od "$STUDIO_DIR/android-studio/plugins" ~/.dustinlam_plugins_download
elif [[ $1 == "help" ]]; then
  echo "ls [query]"
  echo "install [id]"
fi
