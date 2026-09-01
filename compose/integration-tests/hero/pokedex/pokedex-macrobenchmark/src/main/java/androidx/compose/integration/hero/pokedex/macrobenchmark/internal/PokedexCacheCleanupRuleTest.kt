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
import androidx.benchmark.Shell
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.POKEDEX_TARGET_PACKAGE_NAME
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PokedexCacheCleanupRuleTest {

    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun testCacheFilesAreDeleted() {
        val cacheCleanupRule =
            PokedexCacheCleanupRule(targetPackageName = POKEDEX_TARGET_PACKAGE_NAME)
        @SuppressLint("SdCardPath")
        val cacheDirectory = "/data/user/0/$POKEDEX_TARGET_PACKAGE_NAME/cache"
        Shell.executeScriptSilent("mkdir -p $cacheDirectory")
        Shell.executeScriptSilent("touch $cacheDirectory/test_cache_file")
        val cacheFiles =
            Shell.executeScriptCaptureStdout("ls -1 $cacheDirectory").split("\n").filter {
                it.isNotBlank()
            }
        assert(cacheFiles.isNotEmpty()) {
            "Expected to find cache files created for test in $cacheDirectory, but found none."
        }

        cacheCleanupRule.deleteCacheFiles()
        val filesAfterDeletion =
            Shell.executeScriptCaptureStdout("ls -1 $cacheDirectory").split("\n").filter {
                it.isNotBlank()
            }
        assert(filesAfterDeletion.isEmpty()) {
            "Expected to have no cache files after deleting, but found $filesAfterDeletion."
        }
    }
}
