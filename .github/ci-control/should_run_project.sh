# Decides whether we should build a project in the current branch based on the ci-config.json file.
#
# Prints "true" if the project should be built, "false" otherwise. Any other output should fail the build.
#
# It is often possible that a playground build breaks due to unrelated reasons like new
# private prebuilts or build plugin updates.
# In these cases, we want an easy way to disable projects and also have ability to re-enable
# them via automation (e.g. scheduled workflows)
#!/usr/bin/env bash
set -x
PROJECT_NAME=""
BRANCH=""
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
CI_CONFIG="${SCRIPT_DIR}/ci-config.json"

function usage() {
    echo "./should_run_project.sh --project <project-name> --branch <current branch name>\n\
        project-name: the name of the project from the workflow file\n\
        branch: the target branch on the build\n\
        Branches whose name starts with 'compose' use the compose configuration whereas\n\
        everything else uses the main configuration\n\
        See ci-config.json for the configuration."
    exit 1
}

while [ $# -gt 0 ]; do
    case $1 in
        -h|"-?")
            usage
            ;;
        --project)
            PROJECT_NAME=$2
            shift
            ;;
        --branch)
            BRANCH=$2
            shift
            ;;
        *)
            echo "invalid argument $1"
            usage
    esac
    shift
done

if  [[ $PROJECT_NAME == "" ]]; then
    echo "Missing projectName argument"
    usage
fi

if  [[ $BRANCH == "" ]]; then
    echo "Missing branch argument"
    usage
fi

if [[ $BRANCH == *compose* ]]; then
    # compose branch, use compose configuration
    CONFIG_KEY="compose"
else
    CONFIG_KEY="main"
fi

# search for the include list of the selected configuration
EXPLICITLY_INCLUDED=`jq --arg _config $CONFIG_KEY --arg _proj $PROJECT_NAME '.[$_config]?.include? + []|any(contains($_proj))' $CI_CONFIG`
# search for the exclude list of the selected configuration
EXPLICITLY_EXCLUDED=`jq --arg _config $CONFIG_KEY --arg _proj $PROJECT_NAME '.[$_config]?.exclude? + []|any(contains($_proj))' $CI_CONFIG`
# get the default value from the selected configuration
DEFAULT_VALUE=`jq --arg _config $CONFIG_KEY '.[$_config].default' $CI_CONFIG`

# check for explicit exclude
if [[ $EXPLICITLY_EXCLUDED == "true" ]] ; then
    echo "false"
# check for explicit include
elif [[ $EXPLICITLY_INCLUDED == "true" ]] ; then
    echo "true"
else 
    # default value specified in the config file
    echo $DEFAULT_VALUE
fi
