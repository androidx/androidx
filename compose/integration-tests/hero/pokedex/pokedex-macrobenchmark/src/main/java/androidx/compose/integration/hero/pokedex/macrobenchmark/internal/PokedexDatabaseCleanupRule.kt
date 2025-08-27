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

    private fun deleteDatabaseFiles() {
        val foundDatabaseFiles =
            Shell.executeScriptCaptureStdout("ls -1a $databasePath | grep '$databaseName'")
                .split("\n")
                .filter { it.isNotEmpty() }
        if (foundDatabaseFiles.isEmpty()) {
            Log.d(
                "PokedexDatabaseCleanupRule",
                "No database files found for package $targetPackageName.",
            )
        } else {
            Log.d(
                "PokedexDatabaseCleanupRule",
                "Found database files: $foundDatabaseFiles. Removing.",
            )
        }
        for (databaseFile in foundDatabaseFiles) {
            Shell.executeScriptSilent("rm $databasePath/$databaseFile")
        }
    }
}
