/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.integration.hero.pokedex.macrobenchmark.internal

import android.annotation.SuppressLint
import android.util.Log
import androidx.benchmark.Shell
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A [org.junit.rules.TestWatcher] that clears the cache directory for the target package before and
 * after each test to ensure consistency in benchmarks (like Coil disk cache).
 *
 * @param targetPackageName The name of the target package. Defaults to
 *   [PokedexConstants.POKEDEX_TARGET_PACKAGE_NAME].
 */
internal class PokedexCacheCleanupRule(
    private val targetPackageName: String = PokedexConstants.POKEDEX_TARGET_PACKAGE_NAME
) : TestWatcher() {
    override fun starting(description: Description) {
        deleteCacheFiles()
    }

    override fun finished(description: Description) {
        deleteCacheFiles()
    }

    @SuppressLint("SdCardPath") // We don't have access to the target context and need to hardcode
    private val cachePath = "/data/user/0/$targetPackageName/cache"

    fun deleteCacheFiles() {
        val dirExists =
            Shell.executeScriptCaptureStdout(
                    "if [ -d $cachePath ]; then echo 'true'; else echo 'false'; fi"
                )
                .trim() == "true"

        if (!dirExists) {
            Log.d(
                "PokedexCacheCleanupRule",
                "Cache directory $cachePath does not exist for package $targetPackageName. Nothing to delete.",
            )
            return
        }

        Log.d("PokedexCacheCleanupRule", "Clearing cache directory: $cachePath")
        Shell.executeScriptSilent("rm -rf $cachePath/*")
    }
}
