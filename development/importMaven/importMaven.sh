#! /bin/bash
# helper script to build importMaven and execute with the given arguments.
set -e
WORKING_DIR=`pwd`
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

ARTIFACTS=""
ARGUMENTS=""

# Loop through all arguments provided to the script
while [[ "$#" -gt 0 ]]; do
    case $1 in
        import-toml)
            # Priority Check: If this argument is present, print and exit immediately
            # TODO(b/458454333): Remove -Dmaven.repo.local flag when https://github.com/gradle/gradle/issues/38329 is fixed
            (cd $SCRIPT_DIR && ./gradlew -q -Dmaven.repo.local="$(mktemp -d)" -PimportToml)
            exit 0
            ;;
        --metalava-build-id)
            # Check if a value follows the flag
            if [[ -n "$2" && ! "$2" =~ ^- ]]; then
                ARGUMENTS="$ARGUMENTS -PmetalavaBuildId=$2"
                shift 2 # Shift past the flag and the value
            else
                echo "Error: Argument for --metalava-build-id is missing"
                exit 1
            fi
            ;;
        --androidx-build-id)
            if [[ -n "$2" && ! "$2" =~ ^- ]]; then
                ARGUMENTS="$ARGUMENTS -PandroidxBuildId=$2"
                shift 2
            else
                echo "Error: Argument for --androidx-build-id is missing"
                exit 1
            fi
            ;;
        --redownload)
            ARGUMENTS="$ARGUMENTS -Predownload"
            shift # Shift past the flag
            ;;
        --allow-jetbrains-dev)
            ARGUMENTS="$ARGUMENTS -PallowJetbrainsDev"
            shift # Shift past the flag
            ;;
        *)
            # Any argument that doesn't match the flags above is treated as the ARTIFACTS
            # We assume only one required argument is allowed.
            if [[ -z "$ARTIFACTS" ]]; then
                ARGUMENTS="$ARGUMENTS -Partifacts=$1"
                ARTIFACTS="$1"
                shift
            else
                echo "Error: Unknown option or multiple artifacts provided: $1"
                exit 1
            fi
            ;;
    esac
done

# Check if the required ARTIFACTS argument was set
if [[ -z "$ARTIFACTS" ]]; then
    echo "Error: Missing required argument 'ARTIFACTS'"
    echo "Usage: $0 [import-toml] [--metalava-build-id ID] [--androidx-build-id ID] [--redownload] [--allow-jetbrains-dev] ARTIFACTS"
    exit 1
fi

# run importMaven
# TODO(b/458454333): Remove -Dmaven.repo.local flag when https://github.com/gradle/gradle/issues/38329 is fixed
(cd $SCRIPT_DIR && ./gradlew -q -Dmaven.repo.local="$(mktemp -d)" $ARGUMENTS)
