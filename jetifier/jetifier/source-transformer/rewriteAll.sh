set -e
work_path="$PWD"

script_path="$(cd $(dirname $0) && pwd)"

"$script_path/rewriteMake.py"
"$script_path/rewritePackageNames.py"
