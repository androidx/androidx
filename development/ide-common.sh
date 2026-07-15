# Shared logic for studiow and codew

function show_project_subsets() {
  echo "Project subsets:"
  echo " m, main"
  echo "  Open the project subset main: non-Compose Jetpack libraries"
  echo
  echo " c, compose"
  echo "  Open the project subset compose"
  echo
  echo " ca, camera"
  echo "  Open the project subset camera"
  echo
  echo " f, flan"
  echo "  Open the project subset flan: Fragment, Lifecycle, Activity, and Navigation"
  echo
  echo " m3, material3"
  echo "  Open the project subset material3 and material catalog"
  echo
  echo " media"
  echo "  Open the project subset media: Media, Media2, and MediaRouter"
  echo
  echo " kmp"
  echo "  Open the project subset KMP: Projects that have KMP builds"
  echo
  echo " w, wear"
  echo "  Open the project subset for Wear OS libraries"
  echo
  echo " g, glance"
  echo "  Open the project subset for glance projects"
  echo
  echo " x, xr"
  echo "  Open the project subset for XR projects"
  echo
  echo
  echo " native"
  echo "  Open the project subset for native projects"
  echo
  echo " a, all"
  echo "  Open the project subset all"
  echo
}

function show_usage_options() {
  echo "OPTIONS"
  echo
  echo " --clear-caches"
  echo "   Clear generated caches (but not user settings) before launching"
  echo
  echo " --clean"
  echo "   Clear (with backup) generated files (settings, caches, etc) before launching"
  echo "   Also implies --clear-caches"
  echo
  echo " --reinstall"
  echo "   Remove and re-download the IDE itself. Also implies --clean"
  echo
}

# Parses project subset/prefix argument.
# Usage: parse_project_arg <arg>
# Sets: OUT_PROJECT_SUBSET, OUT_PROJECT_PREFIX
# Returns 0 if parsed successfully, 1 otherwise.
function parse_project_arg() {
  local subsetArg="$1"
  OUT_PROJECT_SUBSET=""
  OUT_PROJECT_PREFIX=""

  case "$subsetArg" in
    m|main)
      OUT_PROJECT_SUBSET=main
      ;;
    c|compose)
      OUT_PROJECT_SUBSET=compose
      ;;
    ca|camera)
      OUT_PROJECT_SUBSET=camera
      ;;
    f|flan)
      OUT_PROJECT_SUBSET=flan
      ;;
    m3|material3)
      OUT_PROJECT_PREFIX=":compose:material3,:compose:integration-tests:material-catalog"
      ;;
    media)
      OUT_PROJECT_SUBSET=media
      ;;
    w|wear)
      OUT_PROJECT_SUBSET=wear
      ;;
    g|glance)
      OUT_PROJECT_SUBSET=glance
      ;;
    k|kmp)
      OUT_PROJECT_SUBSET=kmp
      ;;
    native)
      OUT_PROJECT_SUBSET=native
      ;;
    a|all)
      OUT_PROJECT_SUBSET=all
      ;;
    t|tools)
      OUT_PROJECT_SUBSET=tools
      ;;
    wm|window)
      OUT_PROJECT_SUBSET=window
      ;;
    x|xr)
      OUT_PROJECT_SUBSET=xr
      ;;
    :*)
      OUT_PROJECT_PREFIX=$subsetArg
      ;;
    *)
      return 1
      ;;
  esac
  return 0
}

# Shared arguments parsing logic for studiow and codew.
# Usage: parse_ide_args "$@"
# Sets: clearCaches, cleanSettings, reinstall, acceptTos, projectSubset, projectPrefix
# Returns 0 if parsed successfully, 1 otherwise.
function parse_ide_args() {
  clearCaches=false
  cleanSettings=false
  reinstall=false
  acceptTos=false
  projectSubset=""
  projectPrefix=""

  local subsetArg=""

  while [ "$1" != "" ]; do
    local arg="$1"
    shift
    # parse options
    if [ "$arg" == "--acceptTos" ]; then
      acceptTos=true
      continue
    fi
    if [ "$arg" == "--clear-caches" ]; then
      clearCaches=true
      continue
    fi
    if [ "$arg" == "--clean" ]; then
      clearCaches=true
      cleanSettings=true
      continue
    fi
    if [ "$arg" == "--reinstall" ]; then
      clearCaches=true
      cleanSettings=true
      reinstall=true
      continue
    fi
    # parse arguments
    subsetArg="$arg"
    if parse_project_arg "$subsetArg"; then
      if [ "$projectSubset" != "" ] || [ "$projectPrefix" != "" ]; then
        echo "Unrecognized argument '$subsetArg', cannot specify project subset or prefix more than once"
        return 1
      fi
      projectSubset=$OUT_PROJECT_SUBSET
      projectPrefix=$OUT_PROJECT_PREFIX
    else
      echo "Unrecognized argument: '$subsetArg'"
      return 1
    fi
  done

  if [ "$projectSubset" == "" ] && [ "$projectPrefix" == "" ]; then
    echo "Project subset or project prefix is required"
    return 1
  fi
  return 0
}

# ensures the nonexistence of a file or directory, and makes a backup
function backup_and_remove() {
  path="$1"
  backup_dir_name="$2"
  backup="$(dirname $path)/$backup_dir_name/$(basename $path)"
  if [ -e "$path" ]; then
    echo "Moving $path to $backup"
    rm -rf "$backup"
    mkdir -p "$(dirname $backup)"
    mv "$path" "$backup"
  fi
}

function show_usage() {
  local script_name="$1"
  echo "Usage: ${script_name} [--clear-caches] [--clean] [--reinstall] <project subset>|<comma-separated project prefixes>"
  echo
  show_usage_options
  show_project_subsets
  echo "Or use project prefixes to scope down even further. For example:"
  echo " ${script_name} :palette:palette"
  echo " ${script_name} :room3,:sqlite"
  echo
}

function setup_env() {
  local script_name="$1"
  if [ "$projectSubset" != "" ]; then
    unset PROJECT_PREFIX
    export ANDROIDX_PROJECTS=$projectSubset

    echo "Tip: Opening a full project subset ('$projectSubset') can be slow and memory-intensive."
    echo "     If you only need a specific module or set of modules, specify them using project prefixes to speed up launch time."
    echo "     ${script_name} will automatically load all required dependencies for you."
    echo "     Example: ./${script_name} :compose:material"
    echo
  else
    unset ANDROIDX_PROJECTS
    export PROJECT_PREFIX=$projectPrefix
  fi
}
