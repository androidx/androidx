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

package androidx.room3.prepackage

import android.util.Log
import androidx.room3.DatabaseConfiguration
import androidx.room3.Room.LOG_TAG
import androidx.room3.util.copy
import androidx.room3.util.isMigrationRequired
import androidx.room3.util.readVersion
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel

/**
 * A driver that will copy a database file based on the [PrePackagedCopyConfig] in Room's
 * [DatabaseConfiguration] into the intended `fileName` of [open] such that the
 * [androidx.room3.RoomDatabase] is then initializing from an existing database file (a pre-packaged
 * database).
 *
 * This driver is only used if any of [androidx.room3.RoomDatabase.Builder.copyFromAssetPath],
 * [androidx.room3.RoomDatabase.Builder.copyFromFile] or
 * [androidx.room3.RoomDatabase.Builder.copyFromInputStream] are invoked in the builder.
 */
// TODO(b/339934813): Try to move this feature out of Room and into a set of utility drivers.
internal class PrePackagedCopySQLiteDriver(
    private val delegate: SQLiteDriver,
    private val configuration: DatabaseConfiguration,
    private val databaseVersion: Int,
) : SQLiteDriver by delegate {

    private var verified = false

    override fun open(fileName: String): SQLiteConnection {
        if (!verified) {
            verifyDatabaseFile(fileName)
            verified = true
        }
        return delegate.open(fileName)
    }

    private fun verifyDatabaseFile(fileName: String) {
        val databaseFile = File(fileName)

        // No database file found, copy and open.
        if (!databaseFile.exists()) {
            copyDatabase(databaseFile)
            return
        }

        // A database file is present, check if we need to re-copy it based on the version.
        val currentVersion =
            try {
                readVersion(databaseFile)
            } catch (e: IOException) {
                Log.w(LOG_TAG, "Unable to read database version.", e)
                return
            }

        if (currentVersion == databaseVersion) {
            return
        }
        val hasMigrationPath =
            configuration.migrationContainer.findMigrationPath(currentVersion, databaseVersion) !=
                null
        if (hasMigrationPath) {
            // There is a migration path and it will be prioritized, i.e. we won't be
            // performing a copy destructive migration.
            return
        }
        if (configuration.isMigrationRequired(currentVersion, databaseVersion)) {
            // From the current version to the desired version a migration is required, i.e.
            // we won't be performing a copy destructive migration.
            return
        }
        if (deleteDatabase(fileName)) {
            try {
                copyDatabase(databaseFile)
            } catch (e: IOException) {
                // We are more forgiving copying a database on a destructive migration since
                // there is already a database file that can be opened.
                Log.w(LOG_TAG, "Unable to copy database file.", e)
            }
        } else {
            Log.w(
                LOG_TAG,
                "Failed to delete database file ($fileName) for a copy destructive migration.",
            )
        }
    }

    private fun copyDatabase(destinationFile: File) {
        val copyConfig = checkNotNull(configuration.copyFromConfig)
        val input: ReadableByteChannel = Channels.newChannel(copyConfig.getInputStream())
        // An intermediate file is used so that we never end up with a half-copied database file
        // in the internal directory.
        val intermediateFile =
            File.createTempFile("room-copy-helper", ".tmp", configuration.context.cacheDir)
        intermediateFile.deleteOnExit()
        val output = FileOutputStream(intermediateFile).channel
        copy(input, output)
        val parent = destinationFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw IOException("Failed to create directories for ${destinationFile.absolutePath}")
        }
        configuration.prepackagedDatabaseCallback?.let { callback ->
            delegate.open(intermediateFile.path).use { callback.onOpenPrepackagedDatabase(it) }
        }
        if (!intermediateFile.renameTo(destinationFile)) {
            throw IOException(
                "Failed to move intermediate file (${intermediateFile.absolutePath}) to " +
                    "destination (${destinationFile.absolutePath})."
            )
        }
    }

    private fun deleteDatabase(fileName: String): Boolean {
        var deleted = false
        for (postfix in arrayOf("", "-wal", "-shm")) {
            val dbFile = File(fileName + postfix)
            if (dbFile.exists()) {
                deleted = deleted || dbFile.delete()
            }
        }
        return deleted
    }
}
