SCRIPT_DIR="$(cd $(dirname $0) && pwd)"

if [ "$(uname)" = "Darwin" ]; then
  VIRTUAL_ENV_INSTALL_COMMAND="pip3 install --require-hashes -r base-requirements.txt"
  else
  VIRTUAL_ENV_INSTALL_COMMAND="sudo apt-get install virtualenv python3-venv"
fi


# check if virtualenv is installed
if !(pyenv_version=$(virtualenv --version > /dev/null 2>&1)); then
  echo "virtualenv is not installed. Please install with '$VIRTUAL_ENV_INSTALL_COMMAND'"
  exit 1
fi

# create virtualenv
virtualenv androidx_project_creator

# install necessary tools
androidx_project_creator/bin/pip3 install --require-hashes -r $SCRIPT_DIR/requirements.txt

# run project creator
androidx_project_creator/bin/python3 $SCRIPT_DIR/create_project.py "$@"


# clean up virtualenv directory
rm -rf ./androidx-project_creator
