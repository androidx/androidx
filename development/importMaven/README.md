Helper script to download prebuilts for offline builds.

It can download maven artifacts or KMP prebuilts.

# Quickstart

## download single artifact
`./importMaven.sh import-artifact --artifacts "androidx.room:room-runtime:2.4.2"`

## download multiple artifacts
`./importMaven.sh import-artifact --artifacts "androidx.room:room-runtime:2.4.2,com.squareup.okio:okio:3.0.0"`

## download konan prebuilts needed for kotlin native
`./importMaven.sh import-konan-binaries --konan-compiler-version 1.6.1`

## download everything in the dependencies file of AndroidX
`./importMaven.sh import-toml`

## download an androidx prebuilt (via androidx.dev)
`./importMaven.sh import-artifact --androidx-build-id 123 --artifacts "androidx.room:room-runtime:2.5.0-SNAPSHOT"`

## download metalava
`./importMaven.sh import-artifact --metalava-build-id 8660637 --redownload  --artifacts "com.android.tools.metalava:metalava:1.0.0-alpha06"`

## verbose logging
`./importMaven.sh import-artifact --verbose --artifacts "androidx.room:room-runtime:2.4.2"`

# More Help:

For full list of options, please execute one of the commands with `--help`
```
./importMaven.sh --help
./importMaven.sh import-artifact --help
./importMaven.sh import-konan-prebuilts --help
./importMaven.sh import-toml --help
```