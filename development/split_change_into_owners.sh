set -e

if [ ! -e .git ]; then
  echo "This script must be run from the root of the git repository"
  exit 1
fi

function usage() {
  echo "Usage: split_change_into_owners.sh <commit message>"
  echo
  echo "Splits changes in the current repository based on OWNERS files"
  exit 1
}

commitMessage="$1"
if [ "$commitMessage" == "" ]; then
  usage
fi

ownersFiles="$(find -name OWNERS)"
ownedDirs="$(echo "$ownersFiles" | sed 's|/OWNERS||' | sort -r)"

for d in $ownedDirs; do
  git add "$d"
  if git status | grep -i "changes to be committed" >/dev/null; then
    echo making commit for "$d"
    git commit -m "$commitMessage

This change includes files under $d"
  fi
done
