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
import android.util.Log
import androidx.benchmark.Shell
import kotlin.collections.filter
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A [org.junit.rules.TestRule] that deletes any existing database files with name [databaseName]
 * for the target package before and after each test (regardless of test failure status), or does
 * nothing if no files are found.
 *
 * @param targetPackageName The name of the target package. Defaults to
 *   [PokedexConstants.POKEDEX_TARGET_PACKAGE_NAME].
 * @param databaseName The name of the database to delete. Defaults to
 *   [PokedexConstants.POKEDEX_DATABASE_NAME].
 */
internal class PokedexDatabaseCleanupRule(
    private val targetPackageName: String = PokedexConstants.POKEDEX_TARGET_PACKAGE_NAME,
    private val databaseName: String = PokedexConstants.POKEDEX_DATABASE_NAME,
) : TestWatcher() {
    override fun starting(description: Description) {
        deleteDatabaseFiles()
    }

    override fun finished(description: Description) {
        deleteDatabaseFiles()
    }

    @SuppressLint("SdCardPath") // We don't have access to the target context and need to hardcode
    private val databasePath = "/data/data/$targetPackageName/databases"

    fun deleteDatabaseFiles() {
        // First, check if the database directory exists
        val dirExists =
            Shell.executeScriptCaptureStdout(
                    "if [ -d $databasePath ]; then echo 'true'; else echo 'false'; fi"
                )
                .trim() == "true"

        if (!dirExists) {
            Log.d(
                "PokedexDatabaseCleanupRule",
                "Database directory $databasePath does not exist for package $targetPackageName. Nothing to delete.",
            )
            return
        }

        // List all files in the directory and filter for those starting with databaseName
        // Using -name for globbing directly in find or ls
        val foundDatabaseFiles =
            listDatabaseFiles(databasePath = databasePath, databaseName = databaseName).filter {
                it.isNotEmpty()
            }

        if (foundDatabaseFiles.isEmpty()) {
            Log.d(
                "PokedexDatabaseCleanupRule",
                "No database files found for package $targetPackageName with prefix '$databaseName'.",
            )
        } else {
            Log.d(
                "PokedexDatabaseCleanupRule",
                "Found database files: $foundDatabaseFiles. Removing.",
            )

            // Construct a single rm command for all found files for efficiency
            val filesToDelete = foundDatabaseFiles.joinToString(separator = " ") { it }
            Shell.executeScriptSilent("rm $filesToDelete")

            // Optionally, you might want to try deleting the entire directory
            // if you are certain no other files should be there and it's safe.
            // Shell.executeScriptSilent("rm -rf $databasePath")
            // If you delete the directory, you might want to recreate it if the app expects it to
            // exist.
        }
    }

    internal fun listDatabaseFiles(
        databaseName: String = this.databaseName,
        databasePath: String = this.databasePath,
    ): List<String> {
        // List all files in the directory and filter for those starting with databaseName
        // Using -name for globbing directly in find or ls
        return Shell.executeScriptCaptureStdout(
                """
                find $databasePath -maxdepth 1 -type f -name "$databaseName*"
            """
                    .trimIndent()
            )
            .split("\n")
    }
}
