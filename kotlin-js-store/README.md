# Kotlin JS lock files

This directory holds yarn lock file for kotlin js / wasm js. The offline mirror is located at `prebuilts/external/androidx/javascript-for-kotlin`.

To update the prebuilts and lockfile, run `./gradlew kotlinNpmInstall kotlinWasmNpmInstall -Pandroidx.yarnOfflineMode=false && ./gradlew kotlinUpgradeYarnLock kotlinWasmUpgradeYarnLock`. You will need to upload changes from the `prebuilts/external/androidx/javascript-for-kotlin` together with changes to the lockfiles in `frameworks/support`.
