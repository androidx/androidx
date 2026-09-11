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

# 2. Write the Hardcoded ASCII Keys to a temp file
# Keys taken from https://gradle.org/keys/
cat << 'EOF' > "$WORK_DIR/gradle-keys.asc"
-----BEGIN PGP PUBLIC KEY BLOCK-----

xsFNBGOtCzoBEAC7hGOPLFnfvQKzCZpJb3QYq8X9OiUL4tVa5mG0lDTeBBiuQCDy
Iyhpo8IypllGG6Wxj6ZJbhuHXcnXSu/atmtrnnjARMvDnQ20jX77B+g39ZYuqxgw
F/EkDYC6gtNUqzJ8IcxFMIQT+J6LCd3a/eTJWwDLUwSnGXVUPTXzYf4laSVdBDVp
jp6K+tDHQrLZ140DY4GSvT1SzcgR5+5C1Mda3XobIJNHe47AeZPzKuFzZSlKqvrX
QNexgGGjrEDWt9I3CXeNoOVVZvI2k6jAvUSZb+jN/YWpW+onDeV1S/7AUBaKE2TE
EJtidYIOuFsufSwLURwX0um17M47sgzxov9vZYDucGntZn4zKYcZsdkTTkrrgU7N
RSu90mqdL7rCxkUPsSeEUWFyhleGB108QBa5HiE/Z5T5C94kxD9JV1HAocFraTaZ
SrNr0dBvZH7SoLCUQZ6q3gXebLbLQgDSuApjn523927O1wdnig+xDgAqTP14sw9i
9OfvpNhCSolFL7mjGYKGfzTFo4pj5CzoKvvAXcsWY4HvwslWJvmrEqvo8Ss+YTII
fiRSL4DWurT+42yOoExPwcYNofNwEuyYy5Zr9edsXeodScvy/hlri3JuB3Ji142w
xFCuKUfrAh7hOw6QOXgIFyFXWrW0HH/8IoeJjxvG+6euxkGx8QZutyaY6wARAQAB
zSlHcmFkbGUgSW5jLiA8bWF2ZW4tcHVibGlzaGluZ0BncmFkbGUuY29tPsLBkQQT
AQgAOxYhBBvZemoVTngQ7gvIMuLzgwLIB149BQJjrQs6AhsDBQsJCAcCAiICBhUK
CQgLAgQWAgMBAh4HAheAAAoJEOLzgwLIB1491PkQAJLhZivNlDcMNGZb5f5PVUiz
6iZ/q62D6gD00NAE5JAxM9JugoNeRrjhibnAN2rwAlv6yW6Thc8dRZ/t/PrzivO5
f3f+P8rLd+M6XTStSXsDPaCNFl002ZJWeH40AQCw8vwgXL0oIvT2qyvJ+Y3/vJUg
vSCB1O1xKfs8jylb6oZKA4C4lv60IR3jLBb4BneTqXn5ZCHJt4g7+TY2jNY8fQeb
V0Sbq+W/3kcUry8Na0TnffdDP/yuonNx0jYNi72Bb5qoCv++L86WLDmVNbCaNhEf
JA1UGvaMDSn1bVop6bZ431t7omPjTwmoB3maHo2HKHQebzSIoTCanEtFgnffW5gT
LVwif8r97ipJgN3ohdhIdgY7bSKRoUugr3UlST9ScNFpz2Dw+IKWR1A4B8BPz2tc
/TXowLS3fc0DHJJYd5WqCyBTl9ndXTiRb8ImO4RdYyfbv+KfmWh93Cj9fBrN654S
RFGjilcJlZR7Vxn9m+E6tDxUI/fs0GWMf/9UY+jAJMPv3W1/7RMihGQfw51lXnnS
Jz9u6xJJKK5KL4L0hFYyfv2Zs24BQTq+h3lFDpPB4pfgDLm+Tbf7V0VlXUwAt3rq
FxsxxxIut6+0DcfsqWPUfu0wnSpNzKqwS/36hUDwFX+yBZU4kyTn1PMVvyxcXi3j
bcHUw1QpCiEeMi7FTjFhzsFNBGOtCzoBEADSUdEj7dz3jsz4EObAdNXnZnJ5zAkq
E4zbGtU94sXdBtxD1F++5dTNE0ZCVwJLtZnYvxYXYwHBEDB5ZWS7noTL9rXkgXpD
P5WGVLTYIMiGjPkVu2fWZZ78Tu4KIfRnkWdUoMQ2g7YNZ8cVU40cZlk63tRdt7Th
71g+K/RKWdqh7NK0laualahK+Glped0QEo1TfrEhNgT0JUCwWzuM4qWHDys7itF+
+xLJsPSwS/wAUqvsWqGzW/1KrYbbxgKX4vbrqL3jnk4IHvcKAub0uchLv9KR5Qps
VT86TmOB3WsAAlPdosW/ahAc2/XyiCxv5JEo8YpErBZ5TSgUy7lJNABS0JUVCeUC
q/AAZ2TScOwRX8aXCeYASfRHOZCiWrWy5nMGGnXVs42MMIML9d+Hr37BCCFT3Gbw
8WOTeGleE92sed5dBAjOPyQWP+IvYxF7zOyNs46RAVlJfg3G33VwEBQgJwLSl/sU
YqSHe9QubbxI0fiMsTJdZ6/5fbsXVnMbGe4kQDZbDTgylotiHfMCMNefgb0+yA6F
w+EHQeN/v/AtpcpT0w12AOpmlNy4+zPQE8Ai73gtJeTRpiuob3k1/JwvLHemB14C
txBGiHAyYHCjPqTPyQUIikj+R8mecG/60RfSmGe3HW7Hpt907BNEcc4s4V9uvJPH
IJdZS/gmtSp5VQARAQABwsF2BBgBCAAgFiEEG9l6ahVOeBDuC8gy4vODAsgHXj0F
AmOtCzoCGwwACgkQ4vODAsgHXj0ZAhAApDNUMc5H7Zsm5vC9F71CZBO29arMuiYV
P/k6oHWbJHu6VWOU9cn/FKnXcIF6H9WcaV/lshARxGsuXWwvW3MP79bINXBuxOYr
Mc2dEGXoRR6YyTqs8NmQumddWeTAZa1DXLAm6U/KpyuU7aShfJoNcdSOi+pLKyJJ
vM85zGYYeA2c3wD++5VaqFV4ptqa4dkbwNf9KSKPNn30Vm2BaCFaHyR7a3TJTZDr
Po+o7Mj75OlCsSz/UZFMOv5DnPU8dOeP7iaetXXqezKhVzJ6dbUgxPh+IRDOfi+L
ySR73YUgW/JHDfyAkeHPmsmSGWeW7hDsWlgiwBNVOIjEqOLyhsMV+aXHnJ28F25u
QhcnOeITIFYR7f+O/D64aEq2jx2nXQ0URU1CCZI2jlcofUTSOVLDgaK8mcc5Yrs2
ybcOYjDVtKCswfTwIrzEOG7ME/opHnv3GzwBlxUI7xp5d5ZQsLHREwHvVrI3QxxJ
h2eNTGMpg3jZdJ7/fPYuZ5FZvALl5A9w22h3lOuy3+ooWwh7X5iV1lNSSgGft1mh
SRv3NcygIVkxsMTzdOoTDp+GohoM6VJyW45xIbEHtyy9byCtvLIhOOSXXIN3TZz8
+T1wROd4CFsC8Ee2aL6yYTTSDyD+LV1qeuDKX5t/MnegA52oEsFWXay7rkg9TwZw
f7TkwC6aybc=
=AdlS
-----END PGP PUBLIC KEY BLOCK-----
-----BEGIN PGP PUBLIC KEY BLOCK-----

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
=V8Ww
-----END PGP PUBLIC KEY BLOCK-----
EOF

# 3. Manual De-armor (Convert multiple ASCII key blocks to Binary)
# Logic: Extract each Base64 payload block between headers and checksum lines, then decode into keyring.gpg
awk '
    /-----BEGIN PGP PUBLIC KEY BLOCK-----/ { in_block=1; past_headers=0; next }
    /-----END PGP PUBLIC KEY BLOCK-----/   { in_block=0; next }
    in_block && !past_headers && /^$/      { past_headers=1; next }
    in_block && past_headers && /^=/       { past_headers=0; next }
    in_block && past_headers               { print }
' "$WORK_DIR/gradle-keys.asc" | base64 -d > "$WORK_DIR/keyring.gpg" 2>/dev/null

if [ $? -ne 0 ] || [ ! -s "$WORK_DIR/keyring.gpg" ]; then
    echo "Error: Failed to convert public keys to binary format."
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
