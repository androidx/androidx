#!/bin/bash
#
# Script to generate a CL with Android Test Hub test dashboard configurations
# for all libraries, which will display recent test failures. The test dashboard
# with failures for all AndroidX libraries is at go/androidx-ath, each of these
# dashboards will display a subset of these failures.
#
# Creates the set of libraries by grouping the test configs from the latest
# build by prefix. For libraries that already have test dashboards, just the
# query line is updated.
#
# If the library flag is used, the configuration is created/updated for just the
# one provided library instead of for all libraries.
#
# Sample usage to create/update dashboards for all libraries:
#
#   ./development/create_library_test_dashboard.sh
#
# Sample usage to create/update dashboard for just the paging library:
#
#   ./development/create_library_test_dashboard.sh --library paging

source gbash.sh || exit

DEFINE_string library "" "If provided, the one library to create a dashboard for."

# Parse the command line arguments
gbash::init_google "$@"

printf "\n"
printf "=================================================================== \n"
printf "== Fetch test configs from the latest green builds \n"
printf "=================================================================== \n"

# Fetch entries from both branches, as they can be different
branches=("aosp-androidx-main" "androidx-platform-dev")

allZipEntries=""
for branch in ${branches[@]}; do
  # Find the ID of the latest build
  # --latest and --list_zip_entries don't work together for fetch_artifact: b/226554339
  latestBuild=$(/google/data/ro/projects/android/ab lkgb \
      --branch $branch --target androidx_device_tests \
      --raw --custom_raw_format '{o[buildId]}')
  if [ -z "$latestBuild" ]; then
    printf "ERROR: Failed to fetch ID of latest green build for $branch \n"
    exit
  else
    printf "Using latest green build $latestBuild of $branch \n"
  fi

  # Get the list of test configs from the latest build
  branchZipEntries=$(/google/data/ro/projects/android/fetch_artifact \
      --bid $latestBuild --target androidx_device_tests \
      --list_zip_entries androidTest.zip)

  if [ -z "$branchZipEntries" ]; then
    printf "ERROR: Failed to fetch test configs from build $latestBuild for $branch \n"
    exit
  fi

  allZipEntries+="$branchZipEntries\n"
done

# Create list of unique .xml file names
testZipEntries=$(grep ".xml" <<< $allZipEntries | sort | uniq)

# Use the one library provided, or all library names (based on prefixes of the
# test entries) if no library was provided.
if [ -z $FLAGS_library ]; then
  # The config file names are of the form `library-rest-of-config-name.xml` or
  # `libraryRestOfConfigName.xml`. Truncate them to just `library`.
  libraryNames=$(sed 's/\(\([a-z]\|[0-9]\)*\)\([A-Z]\|-\).*/\1/' <<< $testZipEntries \
    | uniq)
else
  libraryNames=$FLAGS_library
fi

printf "\n"
printf "=================================================================== \n"
printf "== Create (if needed) and sync g4 workspace \n"
printf "=================================================================== \n"

client="$(p4 g4d -f androidx-test-dashboards --multichange)"
cd "$client"

# Revert all local changes to prevent merge conflicts when syncing.
# This is OK since we always want to start with a fresh CitC client
g4 revert ...
g4 sync

printf "\n"
printf "=================================================================== \n"
printf "== Create a CL with the test dashboard configuration \n"
printf "=================================================================== \n"

for library in $libraryNames; do
  filePath="wireless/android/busytown/ath_config/configs/prod/androidx/$library.gcl"

  # Filter test entries to ones starting with the library name
  # Separate entries with "|" delimiter and remove trailing "|"
  libraryTests=$(grep "^${library}\(-\|[A-Z]\)" <<< "$testZipEntries" \
    | tr '\n' "|" \
    | sed "s/|$//")
  query="query = 'branch:(aosp-androidx-main|androidx-platform-dev) module:($libraryTests)'"

  if [ -f "$filePath" ]; then
    # Update just the query line if the file already exists
    printf "Updating dashboard configuration for $library\n"
    sed -i "s/query = '.*'/$query/" $filePath
  else
    printf "Creating dashboard configuration for $library\n"
    cat > $filePath <<EOL
// To preview this dashboard, visit android-build.googleplex.com/builds/tests/search
// and copy the query string below into the search bar.
ath_config = {
  $query

  tab = 'TAB_TESTS'

  filters = {
    failure_rate = {
      high_failure_rate = true
      med_failure_rate = true
    }
  }
}
EOL
  fi
done

# Construct CL description
clDesc="Create and update AndroidX library test dashboards

The following script was used to create this config:
https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:development/create_library_test_dashboard.sh
"

# Grab the CL number generated from running `g4 change`.
clNum=$(g4 change --desc "$clDesc" | tail -1 | awk '{print $2}')
printf "View pending changes at http://cl/${clNum} \n"

