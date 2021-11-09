#!/bin/sh

#
#  Copyright (C) 2015 The Android Open Source Project
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

#
# This needs to be ran from the root folder in the Android source tree
#

function parentdirname() {
  FULL_PATH=`readlink -f $1`
  echo `dirname $FULL_PATH`
}

function rundeps() {
  LIB_PATH=$1
  LIB_PARENT_PATH=`parentdirname $LIB_PATH`
  OUTPUT_FILE=`basename $LIB_PARENT_PATH`-`basename $LIB_PATH`.log
  make deps-license PROJ_PATH=$LIB_PATH DEP_PATH=frameworks/support > $OUTPUT_FILE
}

rundeps frameworks/support/customtabs
rundeps frameworks/support/design
rundeps frameworks/support/percent
rundeps frameworks/support/recommendation
rundeps frameworks/support/v4
rundeps frameworks/support/v7/appcompat
rundeps frameworks/support/v7/cardview
rundeps frameworks/support/v7/mediarouter
rundeps frameworks/support/v7/palette
rundeps frameworks/support/v7/gridlayout
rundeps frameworks/support/v7/preference
rundeps frameworks/support/v7/recyclerview
rundeps frameworks/support/v13
rundeps frameworks/support/v14/preference
rundeps frameworks/support/v17/leanback
rundeps frameworks/support/v17/preference-leanback
