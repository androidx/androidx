#!/bin/bash
set -e
cd "$(dirname "$0")"

echo "--------------------------------------------------------"
echo "This command will remove those files added to .gitignore"
git clean -ndX
read -p "Do you want to remove this files? (y/n) " yn

case $yn in
	y ) git clean -fdX;;
	n ) exit;;
	* ) exit 1;;
esac
