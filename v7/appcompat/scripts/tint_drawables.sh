#!/bin/bash

###############################################################################
#
#  Copyright (C) 2014 The Android Open Source Project
# 
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
# 
#       http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
###############################################################################

###################################################################################
#
# Tints all of the drawables in the appcompat drawable folders which are meant to
# be used with L's Drawable tinting. This script precomputes the Drawables for when
# use pre-L.
#
# Requires imagemagick to be in installed.
#
####################################################################################

RES_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )/../res" && pwd )

COLOR_CONTROL_NORMAL_LIGHT=#000000
COLOR_CONTROL_NORMAL_DARK=#ffffff
COLOR_CONTROL_NORMAL_DISABLED_LIGHT=#00000045
COLOR_CONTROL_NORMAL_DISABLED_DARK=#ffffff30
COLOR_ACCENT_LIGHT=#009688
COLOR_ACCENT_DARK=#80cbc4
COLOR_BACKGROUND_LIGHT=#fafafa
COLOR_BACKGROUND_DARK=#212121

NORMAL_NO_PREFIX=" \
    abc_ic_ab_back_mtrl_am_alpha.png
    abc_ic_go_search_api_mtrl_alpha.png \
    abc_ic_search_api_mtrl_alpha.png \
    abc_ic_commit_search_api_mtrl_alpha.png \
    abc_ic_clear_mtrl_alpha.png \
    abc_ic_menu_share_mtrl_alpha.png \
    abc_ic_menu_moreoverflow_mtrl_alpha.png \
    abc_ic_voice_search_api_mtrl_alpha.png \
    abc_textfield_search_default_mtrl_alpha.9.png \
    abc_textfield_default_mtrl_alpha.9.png \
    abc_list_divider_mtrl_alpha.9.png \
    abc_ic_cab_done_mtrl_alpha.png"

NORMAL_PREFIX_DEFAULT="abc_spinner_mtrl_am_alpha.9.png"

NORMAL_DISABLED="abc_textfield_default_mtrl_alpha.9.png"

ACTIVATED_NO_PREFIX="abc_textfield_search_activated_mtrl_alpha.9.png \
    abc_textfield_activated_mtrl_alpha.9.png"

ACTIVATED_PREFIX_CHECKED="abc_spinner_mtrl_am_alpha.9.png"

ACTIVATED_PREFIX_SELECTED="abc_tab_indicator_mtrl_alpha.9.png"

ACTIVATED_PREFIX_PRESSED="abc_spinner_mtrl_am_alpha.9.png"

BACKGROUND_MULTIPLY="abc_popup_background_mtrl_mult.9.png"

function tintDrawable {
  METHOD=$1
  SOURCE=`basename $2`
  COLOR=$3
  THEME=$4
  SUFFIX=$5

  OUTPUT=${SOURCE%_mtrl*png}$SUFFIX$THEME.${SOURCE#*.}

  # If we're dealing with a 9-patch then we need to make sure we do not re-color
  # the 1px border which contain the strechable area definitions
  REGION_ARGS=
  if [[ "$SOURCE" == *".9.png" ]]; then
    WIDTH=`identify -format "%w" $SOURCE`
    HEIGHT=`identify -format "%h" $SOURCE`
    REGION_ARGS="-region "$(($WIDTH - 2))x$(($HEIGHT - 2))!+1+1""
  fi

  convert $SOURCE \( +clone $REGION_ARGS -fill "$COLOR" -colorize 100% \)  -compose $METHOD -composite $OUTPUT

  # ImageMagick's Multiply doesn't do alpha composition, so we need to copy the alpha channel over from source
  if [[ "$METHOD" == "Multiply" ]]; then
    convert $OUTPUT $SOURCE -compose copy-opacity -composite $OUTPUT
  fi

  if command -v optipng &>/dev/null; then
    # if optipng is installed, run the output through it
    optipng $OUTPUT -quiet
  fi

  echo "Tinted $SOURCE -> $OUTPUT"
}

export -f tintDrawable

function tintDrawables {
  method=$1
  files=$2
  color_light=$3
  color_dark=$4
  suffix=$5

  echo "------------------------------------"
  echo "   Tinting Drawables"
  echo "      Method: $method"
  echo "      Suffix: $suffix"
  echo "------------------------------------"

  if [[ "$color_light" == "$color_dark" ]]; then
    # if color_light and color_dark are the same, then only produce one file with a _material theme
    for f in $files; do
      find $RES_DIR -name $f -execdir bash -c 'tintDrawable "$0" "$1" "$2" "_material" "$3"' $method {} $color_dark $suffix \;
    done
  else
    for f in $files; do
      # if color_light and color_dark are different, produce two appropriate files
      find $RES_DIR -name $f -execdir bash -c 'tintDrawable "$0" "$1" "$2" "_material_dark" "$3"' $method {} $color_dark $suffix \;
      find $RES_DIR -name $f -execdir bash -c 'tintDrawable "$0" "$1" "$2" "_material_light" "$3"' $method {} $color_light $suffix \;
    done
  fi
  echo -e "\n"
}

if ! command -v convert &>/dev/null; then
  echo "ImageMagick is not installed. Exiting..."
  exit 1
fi

tintDrawables "Src-In" "$NORMAL_NO_PREFIX" $COLOR_CONTROL_NORMAL_LIGHT $COLOR_CONTROL_NORMAL_DARK
tintDrawables "Src-In" "$NORMAL_PREFIX_DEFAULT" $COLOR_CONTROL_NORMAL_LIGHT $COLOR_CONTROL_NORMAL_DARK "_default"
tintDrawables "Src-In" "$NORMAL_DISABLED" $COLOR_CONTROL_NORMAL_DISABLED_LIGHT $COLOR_CONTROL_NORMAL_DISABLED_DARK "_disabled"
tintDrawables "Src-In" "$ACTIVATED_NO_PREFIX" $COLOR_ACCENT_LIGHT $COLOR_ACCENT_DARK
tintDrawables "Src-In" "$ACTIVATED_PREFIX_CHECKED" $COLOR_ACCENT_LIGHT $COLOR_ACCENT_DARK "_checked"
tintDrawables "Src-In" "$ACTIVATED_PREFIX_PRESSED" $COLOR_ACCENT_LIGHT $COLOR_ACCENT_DARK "_pressed"
tintDrawables "Src-In" "$ACTIVATED_PREFIX_SELECTED" $COLOR_ACCENT_LIGHT $COLOR_ACCENT_DARK "_selected"
tintDrawables "Multiply" "$BACKGROUND_MULTIPLY" $COLOR_BACKGROUND_LIGHT $COLOR_BACKGROUND_DARK
