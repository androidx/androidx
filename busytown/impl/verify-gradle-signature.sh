#!/bin/bash

SCRIPT_DIR="$(dirname "$(readlink -f "$0")")"

# Configuration
PROPS_FILE="$SCRIPT_DIR/../../gradle/wrapper/gradle-wrapper.properties"

# 1. Locate and parse gradle-wrapper.properties
if [[ ! -f "$PROPS_FILE" ]]; then
    echo "Error: Could not find properties file at '$PROPS_FILE'."
    echo "Please ensure you are running this script from the project root."
    exit 1
fi

# Extract distributionUrl
# 1. grep the line
# 2. cut after the first '='
DIST_PATH=$(grep "^distributionUrl=" "$PROPS_FILE" | cut -d'=' -f2)

if [[ -z "$DIST_PATH" ]]; then
    echo "Error: 'distributionUrl' not found in $PROPS_FILE."
    exit 1
fi

# Define File and Signature based on the extracted path
DISTRIBUTION_FILE="$SCRIPT_DIR/$DIST_PATH"
DISTRIBUTION_SIGNATURE="${DISTRIBUTION_FILE}.asc"

WRAPPER_FILE="$SCRIPT_DIR/../../gradle/wrapper/gradle-wrapper.jar"
WRAPPER_SIGNATURE="${WRAPPER_FILE}.asc"

# Check if input files exist
if [[ ! -f "$DISTRIBUTION_FILE" ]]; then
    echo "Error: File '$DISTRIBUTION_FILE' not found."
    echo "Ensure the relative path in gradle-wrapper.properties is correct relative to this script."
    exit 1
fi
if [[ ! -f "$DISTRIBUTION_SIGNATURE" ]]; then
    echo "Error: Signature '$DISTRIBUTION_SIGNATURE' not found."
    exit 1
fi
if [[ ! -f "$WRAPPER_FILE" ]]; then
    echo "Error: Wrapper '$WRAPPER_FILE' not found."
    exit 1
fi
if [[ ! -f "$WRAPPER_SIGNATURE" ]]; then
    echo "Error: Signature '$WRAPPER_SIGNATURE' not found."
    exit 1
fi

# Check for required tools
if ! command -v gpgv &> /dev/null; then
    echo "Error: 'gpgv' is not installed."
    exit 1
fi
if ! command -v base64 &> /dev/null; then
    echo "Error: 'base64' tool is required for manual key conversion."
    exit 1
fi

# Create a temporary directory
WORK_DIR=$(mktemp -d)

# Ensure cleanup on exit
cleanup() {
    rm -rf "$WORK_DIR"
}
trap cleanup EXIT

# 2. Write the Hardcoded ASCII Key to a temp file
# Key is taken from https://gradle.org/keys/
cat << 'EOF' > "$WORK_DIR/gradle-key.asc"
-----BEGIN PGP PUBLIC KEY BLOCK-----

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
f7TkwC6aybc=
=AdlS
-----END PGP PUBLIC KEY BLOCK-----
EOF

# 3. Manual De-armor (Convert ASCII to Binary)
# Logic: Skip headers, stop at checksum, decode body.
awk '/^$/{body=1; next} /^=/{body=0; exit} body {print}' "$WORK_DIR/gradle-key.asc" | \
base64 -d > "$WORK_DIR/keyring.gpg" 2>/dev/null

if [ $? -ne 0 ] || [ ! -s "$WORK_DIR/keyring.gpg" ]; then
    echo "Error: Failed to convert public key to binary format."
    exit 1
fi

# 4. Use gpgv to verify using the temp binary keyring
if OUTPUT=$(gpgv --keyring "$WORK_DIR/keyring.gpg" "$DISTRIBUTION_SIGNATURE" "$DISTRIBUTION_FILE" 2>&1); then
    if OUTPUT=$(gpgv --keyring "$WORK_DIR/keyring.gpg" "$WRAPPER_SIGNATURE" "$WRAPPER_FILE" 2>&1); then
        exit 0
    else
        echo "$OUTPUT"
        echo ""
        echo "❌ FAILURE: The gradle wrapper signature is invalid."
        exit 1
    fi
else
    echo "$OUTPUT"
    echo ""
    echo "❌ FAILURE: The gradle distribution signature is invalid."
    exit 1
fi
