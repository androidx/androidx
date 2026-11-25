#!/usr/bin/env bash
#
# Copyright 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Bash tab completion for AndroidX's ./gradlew script (b/554104890).
#
# Usage:
#   Add to ~/.bashrc (Linux) or ~/.bash_profile (macOS):
#     source /path/to/frameworks/support/development/gradlew-completion.sh
#
#   To also enable completion for an alias (e.g. `gw`):
#     complete -o nospace -F _androidx_gradlew_complete gw
#

# Check for settings.gradle in the current working directory ($PWD).
# Only activates when $PWD contains "frameworks/support" and ./settings.gradle
# declares `includeProject(`.
# Prints the path to settings.gradle on stdout and returns 0, or returns 1 if
# the current directory is not an AndroidX frameworks/support/ checkout.
_androidx_gradlew_find_settings() {
  if [[ "$PWD" == *frameworks/support* ]] &&
     [[ -f "$PWD/settings.gradle" ]] &&
     command grep -Fq 'includeProject(' "$PWD/settings.gradle" 2>/dev/null; then
    printf '%s\n' "$PWD/settings.gradle"
    return 0
  fi
  return 1
}

# Readline (Bash's line editor) splits words on characters in the built-in
# COMP_WORDBREAKS variable, which includes ':' by default. Because ':' acts as a
# word separator, Readline only replaces the text *after* the last ':' when
# applying completions.
#
# For example, if the user typed `:core:co`, Readline treats `co` as the active
# fragment. If COMPREPLY contained `:core:core-ktx`, Readline would insert the
# whole string after `:core:` and produce `:core::core:core-ktx`. To prevent
# that duplication, strip `:core:` from every COMPREPLY element so Readline sees
# `core-ktx` and produces `:core:core-ktx`.
#
# Arguments:
#   $1 (cur): The full token before the cursor (e.g. ":core:co").
# Mutates:
#   COMPREPLY: Bash's built-in output array of completion candidates.
_androidx_gradlew_ltrim_colon_completions() {
  local cur="$1"
  if [[ "$cur" == *:* && "${COMP_WORDBREAKS:-}" == *:* ]]; then
    # `${cur##*:}` strips the longest prefix ending in ':' (leaving text after
    # the last ':', e.g. "co" from ":core:co").
    local after_colon="${cur##*:}"
    # `${cur%"$after_colon"}` strips that suffix from the end of $cur, leaving
    # the prefix up to and including the last ':' (e.g. ":core:").
    local colon_word="${cur%"$after_colon"}"
    # `${#COMPREPLY[@]}` is the number of elements in the COMPREPLY array.
    local i=${#COMPREPLY[@]}
    while [[ $((--i)) -ge 0 ]]; do
      # `${str#"$prefix"}` removes a matching literal prefix from each entry.
      COMPREPLY[$i]="${COMPREPLY[$i]#"$colon_word"}"
    done
  fi
}

# Complete AndroidX Gradle project paths segment-by-segment from settings.gradle.
# Arguments:
#   $1 (cur): The current argument token being typed (e.g. "", ":co", ":core:co",
#             or "core:co" without a leading colon).
#   $2 (settings_file): Path to AndroidX's settings.gradle.
# Appends matching candidates to Bash's built-in COMPREPLY array.
_androidx_gradlew_complete_projects() {
  local cur="$1"
  local settings_file="$2"

  # Reject inputs with characters that cannot appear in Gradle project paths
  # (`=~` performs regex matching).
  if [[ -n "$cur" && ! "$cur" =~ ^[A-Za-z0-9_.:-]+$ ]]; then
    return 0
  fi

  # Gradle accepts project paths with or without a leading ':' (e.g. both
  # `:core:core` and `core:core`). Normalize to a leading ':' while searching
  # settings.gradle, and remember whether to strip it back off in the output.
  local strip_leading_colon=0
  local search_prefix=""
  if [[ -z "$cur" ]]; then
    search_prefix=":"
  elif [[ "$cur" == :* ]]; then
    search_prefix="$cur"
  else
    strip_leading_colon=1
    search_prefix=":$cur"
  fi

  # `base` is the path up to and including the last ':' in `search_prefix`
  # (e.g. ":core:" when `search_prefix` is ":core:co", or ":" when ":co").
  local after_colon="${search_prefix##*:}"
  local base="${search_prefix%"$after_colon"}"

  # Read each sorted completion line from the subshell into COMPREPLY.
  # - `IFS= read -r` preserves exact characters without trimming or backslash escaping.
  # - `< <(...)` is Bash process substitution: runs the pipeline in a subshell
  #   and feeds its stdout to the `while` loop without creating a subshell for the
  #   loop itself (so edits to COMPREPLY persist in the caller).
  local candidate
  while IFS= read -r candidate; do
    COMPREPLY+=("$candidate")
  done < <(
    # Pass parameters to awk via environment variables (ENVIRON[...]) rather
    # than `-v` so backslashes or special characters are never interpreted.
    GRADLEC_PREFIX="$search_prefix" \
    GRADLEC_BASE="$base" \
    GRADLEC_STRIP="$strip_leading_colon" \
    awk '
      # Pattern block: runs on every line of settings.gradle declaring an
      # includeProject(":path:to:project", ...) entry.
      /^[ \t]*includeProject\([ \t]*"/ {
        line = $0
        # Strip everything up through the opening quote, then slice up to the closing quote.
        sub(/^[ \t]*includeProject\([ \t]*"/, "", line)
        q = index(line, "\"")
        if (q == 0) next
        proj = substr(line, 1, q - 1)
        if (substr(proj, 1, 1) != ":") next
        projects[proj] = 1
      }
      # END block: runs once after all lines of settings.gradle are collected.
      END {
        prefix = ENVIRON["GRADLEC_PREFIX"]
        base = ENVIRON["GRADLEC_BASE"]
        # `+ 0` coerces the string "0"/"1" from ENVIRON into a numeric boolean.
        strip_colon = ENVIRON["GRADLEC_STRIP"] + 0
        plen = length(prefix)
        blen = length(base)

        for (proj in projects) {
          # Skip projects that do not start with what the user has typed so far.
          if (substr(proj, 1, plen) != prefix) continue

          # Take the portion after `base` (everything after the last typed `:`).
          rest = substr(proj, blen + 1)
          if (rest == "") continue

          # Find the next `:` to emit only one segment at a time.
          cpos = index(rest, ":")
          if (cpos == 0) {
            # No more colons: this is a leaf segment (an actual Gradle project).
            cand = proj
          } else {
            # There are deeper segments. Check if this intermediate prefix is
            # itself a registered project (e.g. `:core:core` alongside
            # `:core:core:integration-tests:testapp`):
            # - If yes, emit `:core:core` without a trailing `:` so the user can
            #   either finish the argument or type `:` for subprojects/tasks.
            # - If no (pure group like `:compose:`), emit with a trailing `:`.
            seg_proj = base substr(rest, 1, cpos - 1)
            if (seg_proj in projects) {
              cand = seg_proj
            } else {
              cand = seg_proj ":"
            }
          }

          # Deduplicate candidates across projects sharing the same group prefix.
          if (!(cand in seen)) {
            seen[cand] = 1
            if (strip_colon) {
              sub(/^:/, "", cand)
            }
            print cand
          }
        }
      }
    # LC_ALL=C ensures deterministic byte-order sorting regardless of user locale.
    ' "$settings_file" | LC_ALL=C sort
  )
}

# Complete common AndroidX gradlew and Gradle CLI flags when typing `-...`.
# Arguments:
#   $1 (cur): The current flag prefix being typed (e.g. "--st" or "-Pandroidx.").
# Appends matching flags to Bash's built-in COMPREPLY array.
_androidx_gradlew_complete_flags() {
  local cur="$1"
  local flags=(
    "--ci"
    "--no-ci"
    "--strict"
    "--clean"
    "--continue"
    "--offline"
    "--refresh-dependencies"
    "--rerun"
    "--rerun-tasks"
    "--no-daemon"
    "--stop"
    "--scan"
    "--dry-run"
    "--info"
    "--debug"
    "--stacktrace"
    "--quiet"
    "--warn"
    "--build-cache"
    "--no-build-cache"
    "--configuration-cache"
    "--no-configuration-cache"
    "--watch-fs"
    "--no-watch-fs"
    "--parallel"
    "--no-parallel"
    "--max-workers="
    "--dependency-verification="
    "--warning-mode="
    "--tests"
    "--help"
    "--version"
    "-Pandroidx.enableAffectedModuleDetection"
    "-Pandroidx.summarizeStderr"
    "-Pandroidx.validateNoUnrecognizedMessages"
    "-Pandroidx.verifyUpToDate"
    "-Pandroidx.useMaxDepVersions"
    "-Pandroidx.displayTestOutput="
    "-Pandroid.testInstrumentationRunnerArguments.class="
  )

  # if the current flag being typed in already has an `=`, we don't want to have further
  # suggestions, as the current flag is done. If we don't have this conditional, if the user types
  # `--max-workers=` (for example) and presses tab, then `--max-workers=` will appear again
  # immediately after.
  if [[ "$cur" == *=* ]]; then
    return 0
  fi

  local flag
  for flag in "${flags[@]}"; do
    # `== "$cur"*` performs glob prefix matching inside `[[ ... ]]`.
    if [[ "$flag" == "$cur"* ]]; then
      COMPREPLY+=("$flag")
    fi
  done

  # `compopt` (Bash 4+) dynamically toggles completion options per invocation.
  # Guard with `type compopt` because macOS ships Bash 3.2, which lacks it.
  if type compopt >/dev/null 2>&1; then
    if [[ "${#COMPREPLY[@]}" -eq 1 ]]; then
      case "${COMPREPLY[0]}" in
        *=)
          # Keep cursor adjacent to `=` so the user can type the option value.
          compopt -o nospace 2>/dev/null || true
          ;;
        *)
          # Single complete flag: re-enable trailing space after completion.
          compopt +o nospace 2>/dev/null || true
          ;;
      esac
    else
      compopt -o nospace 2>/dev/null || true
    fi
  fi
}

# Main Bash programmable completion entrypoint registered via `complete -F`.
# Reads Bash's built-in completion state variables:
#   COMP_LINE  - Full text of the current command line.
#   COMP_POINT - 0-based cursor index within COMP_LINE.
#   COMP_WORDS - Array of words on the command line (split by COMP_WORDBREAKS).
#   COMP_CWORD - Index in COMP_WORDS of the word containing the cursor.
# Populates Bash's built-in COMPREPLY array with completion candidates.
_androidx_gradlew_complete() {
  # Nothing to complete if the cursor is still on the command name itself (word 0).
  [[ "${COMP_CWORD:-0}" -le 0 ]] && return 0

  COMPREPLY=()

  # Extract the full current token up to COMP_POINT without splitting on ':'.
  # We derive `cur` from COMP_LINE/COMP_POINT instead of COMP_WORDS[COMP_CWORD]
  # because COMP_WORDBREAKS normally splits words on ':', which would hide
  # earlier segments like `:core:` from `cur`.
  local line="${COMP_LINE:0:${COMP_POINT:-${#COMP_LINE}}}"
  local cur=""
  case "$line" in
    *[[:space:]])
      # Cursor immediately follows whitespace, so the current token is empty.
      cur=""
      ;;
    *)
      # `${line##*[[:space:]]}` strips everything up through the last whitespace,
      # leaving the full whitespace-delimited token currently being typed.
      cur="${line##*[[:space:]]}"
      ;;
  esac

  local settings_file=""
  if ! settings_file="$(_androidx_gradlew_find_settings)"; then
    # Not in an AndroidX frameworks/support/ checkout: restore Bash's default
    # filename/path completion so other Gradle repos (and playground-projects)
    # still get normal tab completion.
    if type compopt >/dev/null 2>&1; then
      # Bash 4+: `compopt` overrides the `complete` spec for this Tab press only,
      # re-enabling default completion (`-o default`) and normal trailing spaces
      # (`+o nospace`).
      compopt -o default +o nospace 2>/dev/null || true
    else
      # Bash 3.2 fallback (macOS lacks `compopt`): manually populate filename
      # matches via `compgen -f`, appending `/` to directories since `-o nospace`
      # is active on the `complete` registration.
      local match
      while IFS= read -r match; do
        if [[ -d "$match" ]]; then
          COMPREPLY+=("$match/")
        else
          COMPREPLY+=("$match")
        fi
      done < <(compgen -f -- "$cur")
    fi
    return 0
  fi

  # Inside `[[ ... ]]`, `== -*` is glob matching: true when `cur` starts with `-`
  # (meaning the user is typing a CLI flag/option such as `--strict` or `-P...`).
  if [[ "$cur" == -* ]]; then
    _androidx_gradlew_complete_flags "$cur"
    return 0
  fi

  # Suppress trailing space after project path completions so the user can
  # keep drilling into `:subprojects` or append `:taskName`.
  if type compopt >/dev/null 2>&1; then
    compopt -o nospace 2>/dev/null || true
  fi

  _androidx_gradlew_complete_projects "$cur" "$settings_file"
  _androidx_gradlew_ltrim_colon_completions "$cur"
}

# Register `_androidx_gradlew_complete` for both bare `gradlew` and `./gradlew`
# (registering `./gradlew` explicitly is needed on Bash 3.2 on macOS).
# `-o nospace` prevents Bash from appending a space after partial `:group:` paths.
complete -o nospace -F _androidx_gradlew_complete gradlew ./gradlew
