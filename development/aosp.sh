#!/bin/bash

function fn_aosp_ls {
  CHANGE_ID=$1
  curl -s "https://android-review.googlesource.com/changes/$CHANGE_ID?o=CURRENT_REVISION" | tail -n +2
}

function fn_aosp_checkout {
  CHANGE_ID=$1
  REPO_ID=`fn_aosp_ls $CHANGE_ID \
    | jq "(._number|tostring) + \"/\" + (.revisions | to_entries[].value._number|tostring)"`

  if [ -z $DEP_CHANGE_ID ]; then
    echo "Failed to parse aosp command for Change-Id: $CHANGE_ID"
    exit 1
  fi

  repo download "platform/frameworks/support" ${REPO_ID:1:-1}  \
    && BRANCH=`git log -1 | grep Change-Id | awk '{print $2}'` \
    && git checkout -B "aosp/$BRANCH"                          \
    && git branch --set-upstream-to=aosp/androidx-main
}

function fn_aosp_merged {
  CHANGE_ID=$1
  echo $CHANGE_ID
  fn_aosp_ls $CHANGE_ID
}

function fn_git_changeid {
  BRANCH=$1
  git log $BRANCH -1 | grep Change-Id | awk '{print $2}'
}

function fn_git_check_uncommitted_changes {
  if [[ `git status --porcelain` ]]; then
    echo "Uncommitted changes; exiting"
    exit
  fi
}

# Check that jq is installed.
if [ -z `which jq` ]; then
    echo "jq not installed; exiting"
    exit
fi

if [[ -z "$1" ]] || [[ "$1" == "help" ]]; then
  echo -n \
"usage: aosp <command> [<args>]

A CLI for Gerrit UI that works with the cli tool, repo, to make it easier to search, checkout, and rebase changes. This script currently only works within frameworks/support.

Commands:
  help               	Display this help text
  ls [<gerrit-email>]	Query gerrit for open CLs; you must run this before aosp c. <gerrit-email> defaults to `whoami`@google.com
  c <id>             	Force checkout a change in gerrit by the id returned from aosp ls command into a branch named `whoami`/<change-id>
  prune              	Delete any branches whose HEAD has a Change-Id for a CL that has been MERGED or ABANDONED in Gerrit
  r                  	Checkout the latest patchset of HEAD~1's Change-Id, and cherry-pick HEAD ontop of it
"
elif [[ "$1" == "ls" ]]; then
  OWNER="${2:-`whoami`@google.com}"
  curl -s "https://android-review.googlesource.com/changes/?q=owner:$OWNER+status:open&o=CURRENT_REVISION" \
    | tail -n +2 \
    | jq -r ".[] | [._number, .subject, (.revisions | to_entries[].value._number), (.revisions[.current_revision].fetch.http | .url, .ref)] | @csv" \
    | awk -F, '{ printf "%s,%s/%s,%s,%s,%s\n", NR, $1, $3, $2, $4, $5}' \
    | column -t -s, \
    | tee ~/.dustinlam_aosp
elif [[ "$1" == "prune" ]]; then
  git branch           \
    | grep -v "^\*"    \
    | awk '{print $1}' \
    | while read line
    do
      CHANGE_ID=`fn_git_changeid $line`
      if [ ! -z "$CHANGE_ID" ]; then
        STATUS=`fn_aosp_ls "$CHANGE_ID" | jq .status`
        if [[ $STATUS == "\"MERGED\"" ]] || [[ $STATUS == "\"ABANDONED\"" ]]; then
          git branch -D $line
        fi
      else
        echo "Failed to prune $line due to missing Change-Id"
      fi
    done
elif [[ "$1" == "c" ]]; then
  fn_git_check_uncommitted_changes

  PATCH=`cat ~/.dustinlam_aosp | sed -n "$2p" | awk '{print $2}'`      \
  && repo download "platform/frameworks/support" "$PATCH"    \
  && BRANCH=`git log -1 | grep Change-Id | awk '{print $2}'` \
  && git checkout -B "`whoami`/$BRANCH"                     \
  && git branch --set-upstream-to=aosp/androidx-main
elif [[ "$1" == "r" ]]; then
  fn_git_check_uncommitted_changes

  HEAD_COMMIT_HASH=`git rev-parse HEAD`
  CURRENT_BRANCH=`git branch | grep \* | awk '{print $2}'`
  DEP_CHANGE_ID=`git log --skip 1 -1 | grep Change-Id | awk '{print $2}'`
  if [ -z $DEP_CHANGE_ID ]; then
    echo "Dependent change is missing Change-Id"
    exit 1
  fi

  fn_aosp_checkout $DEP_CHANGE_ID   \
    && git checkout -B $CURRENT_BRANCH \
    && git cherry-pick $HEAD_COMMIT_HASH
fi
