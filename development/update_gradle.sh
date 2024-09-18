#!/bin/bash

set -e

# Check if the user has provided the version as an argument
if [ -z "$1" ]; then
  echo "Error: No version provided. Usage: $0 <gradle-version>"
  exit 1
fi

VERSION="$1"
DEST_DIR="../../tools/external/gradle"
WRAPPER_FILES=("gradle/wrapper/gradle-wrapper.properties" "playground-common/gradle/wrapper/gradle-wrapper.properties")

BASE_URL="https://services.gradle.org/distributions"
ZIP_FILE="gradle-${VERSION}-bin.zip"
SHA_FILE="${ZIP_FILE}.sha256"

# Function to check if a URL is valid by checking the HTTP status code
check_url() {
  local url="$1"

  echo "Checking URL: $url"

  http_status=$(curl -L --silent --head --write-out "%{http_code}" --output /dev/null "$url")

  if [ "$http_status" -ne 200 ]; then
    echo "Error: URL returned status code $http_status. The file doesn't exist at: $url"
    exit 1
  else
    echo "URL is valid: $url"
  fi
}

check_url "$BASE_URL/$ZIP_FILE"
check_url "$BASE_URL/$SHA_FILE"

echo "Cleaning destination directory: $DEST_DIR"
rm -rf "$DEST_DIR"/*
mkdir -p "$DEST_DIR"

echo "Downloading Gradle ${VERSION}..."
curl -Lo "$DEST_DIR/$ZIP_FILE" "$BASE_URL/$ZIP_FILE"
curl -Lo "$DEST_DIR/$SHA_FILE" "$BASE_URL/$SHA_FILE"

GRADLE_SHA256SUM=$(cat "$DEST_DIR/$SHA_FILE")

echo "Downloaded Gradle ${VERSION} with SHA256: $GRADLE_SHA256SUM"

update_gradle_wrapper_properties() {
  local file="$1"
  echo "Updating $file..."

  if [ "$(uname)" = "Darwin" ]; then
    sed -i '' "
      s|distributionUrl=.*tools/external/gradle/.*|distributionUrl=../../../../tools/external/gradle/${ZIP_FILE}|;
      s|distributionUrl=https\\\://services.gradle.org/distributions/.*|distributionUrl=https\\\://services.gradle.org/distributions/${ZIP_FILE}|;
      s|distributionSha256Sum=.*|distributionSha256Sum=${GRADLE_SHA256SUM}|
    " "$file"
  else
    sed -i "
      s|distributionUrl=.*tools/external/gradle/.*|distributionUrl=../../../../tools/external/gradle/${ZIP_FILE}|;
      s|distributionUrl=https\\\://services.gradle.org/distributions/.*|distributionUrl=https\\\://services.gradle.org/distributions/${ZIP_FILE}|;
      s|distributionSha256Sum=.*|distributionSha256Sum=${GRADLE_SHA256SUM}|
    " "$file"
  fi

  echo "Updated $file."
}

for file in "${WRAPPER_FILES[@]}"; do
  update_gradle_wrapper_properties "$file"
done

echo "Gradle binary downloaded, and the wrapper properties updated successfully!"

echo "Testing the setup with './gradlew bOS --dry-run'..."
if ./gradlew bOS --dry-run; then
  echo "Download and setup successful!"
  echo "You can now upload changes in $(pwd) and $DEST_DIR to Gerrit!"
fi
