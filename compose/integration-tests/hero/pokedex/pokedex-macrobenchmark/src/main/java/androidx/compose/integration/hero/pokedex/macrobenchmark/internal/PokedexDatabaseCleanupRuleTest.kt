/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.POKEDEX_DATABASE_NAME
import androidx.compose.integration.hero.pokedex.macrobenchmark.internal.PokedexConstants.POKEDEX_TARGET_PACKAGE_NAME
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PokedexDatabaseCleanupRuleTest {

    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun testDatabaseFilesAreDeleted() {
        val databaseCleanupRule =
            PokedexDatabaseCleanupRule(
                targetPackageName = POKEDEX_TARGET_PACKAGE_NAME,
                databaseName = POKEDEX_DATABASE_NAME,
            )
        @SuppressLint("SdCardPath")
        val dbDirectory = "/data/data/$POKEDEX_TARGET_PACKAGE_NAME/databases"
        Shell.executeScriptSilent("mkdir -p $dbDirectory")
        Shell.executeScriptSilent("touch $dbDirectory/$POKEDEX_DATABASE_NAME")
        val databaseFiles = Shell.executeScriptCaptureStdout("ls -1 $dbDirectory").split("\n")
        assert(databaseFiles.isNotEmpty()) {
            "Expected to find database files created for test in $dbDirectory, but found none."
        }
        val listedDatabaseFiles = databaseCleanupRule.listDatabaseFiles()
        assert(databaseFiles == listedDatabaseFiles) {
            """
                Expected output of listDatabaseFiles() to match output of ls -1 $dbDirectory.
                    Expected: $databaseFiles
                    Actual: $listedDatabaseFiles
            """
                .trimIndent()
        }

        val filesBeforeDeletion = databaseCleanupRule.listDatabaseFiles()
        databaseCleanupRule.deleteDatabaseFiles()
        val filesAfterDeletion = databaseCleanupRule.listDatabaseFiles()
        assert(filesBeforeDeletion.isNotEmpty()) {
            "Expected to have database files before deleting, but found none."
        }
        assert(filesAfterDeletion.isEmpty()) {
            "Expected to have no database files after deleting, but found $filesAfterDeletion."
        }
    }
}
