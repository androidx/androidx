#! /usr/bin/env python3
import sys
import getopt
import json
import os

def usage(exitCode):
    print("""
        Usage:
        ./should_run_project.sh --project <project-name> --branch <current branch name>\n\
        project-name: the name of the project from the workflow file\n\
        branch: the target branch on the build\n\
        Branches whose name starts with 'compose' use the compose configuration whereas\n\
        everything else uses the main configuration\n\
        See ci-config.json for the configuration.
        """)
    sys.exit(exitCode)


# finds the configuration we should use based on the branch
def getCurrentConfig(branch: str):
    configFilePath = os.path.join(os.path.dirname(
        os.path.realpath(__file__)), "ci-config.json")
    configFile = open(configFilePath)
    ciConfig = json.load(configFile)
    configFile.close()
    if "compose-compiler" in branch:
        return ciConfig["compose"]
    else:
        return ciConfig["main"]


def main(argv):
    projectName = ''
    branch = ''
    try:
        opts, args = getopt.getopt(argv, "h", ["project=", "branch="])
    except getopt.GetoptError as err:
        print(err)
        usage(2)
    for opt, arg in opts:
        if opt == '-h':
            usage(0)
        elif opt in ("--project"):
            projectName = arg
        elif opt in ("--branch"):
            branch = arg
        else:
            print("invalid argument ", opt, "-", arg)
            usage(2)

    currentConfig = getCurrentConfig(branch)
    result=currentConfig["default"]
    if "include" in currentConfig:
        result = projectName in currentConfig["include"]
    # run exclude after include to give it priority
    if "exclude" in currentConfig:
        result = not (projectName in currentConfig["exclude"])
    print(str(result).lower())

if __name__ == "__main__":
    main(sys.argv[1:])
