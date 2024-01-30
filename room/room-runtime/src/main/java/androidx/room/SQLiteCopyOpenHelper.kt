/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.room

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.Room.LOG_TAG
import androidx.room.util.copy
import androidx.room.util.readVersion
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.util.ProcessLock
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.Callable

/**
 * An open helper that will copy & open a pre-populated database if it doesn't exists in internal
 * storage.
 */
@Suppress("BanSynchronizedMethods")
internal class SQLiteCopyOpenHelper(
    private val context: Context,
    private val copyFromAssetPath: String?,
    private val copyFromFile: File?,
    private val copyFromInputStream: Callable<InputStream>?,
    private val databaseVersion: Int,
    override val delegate: SupportSQLiteOpenHelper
) : SupportSQLiteOpenHelper, DelegatingOpenHelper {
    private lateinit var databaseConfiguration: DatabaseConfiguration
    private var verified = false

    override val databaseName: String?
        get() = delegate.databaseName

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        delegate.setWriteAheadLoggingEnabled(enabled)
    }

    override val writableDatabase: SupportSQLiteDatabase
        get() {
            if (!verified) {
                verifyDatabaseFile(true)
                verified = true
            }
            return delegate.writableDatabase
        }

    override val readableDatabase: SupportSQLiteDatabase
        get() {
            if (!verified) {
                verifyDatabaseFile(false)
                verified = true
            }
            return delegate.readableDatabase
        }

    @Synchronized
    override fun close() {
        delegate.close()
        verified = false
    }

    // Can't be constructor param because the factory is needed by the database builder which in
    // turn is the one that actually builds the configuration.
    fun setDatabaseConfiguration(databaseConfiguration: DatabaseConfiguration) {
        this.databaseConfiguration = databaseConfiguration
    }

    private fun verifyDatabaseFile(writable: Boolean) {
        val name = checkNotNull(databaseName)
        val databaseFile = context.getDatabasePath(name)
        val processLevelLock = (databaseConfiguration.multiInstanceInvalidation)
        val copyLock = ProcessLock(
            name,
            context.filesDir,
            processLevelLock
        )
        try {
            // Acquire a copy lock, this lock works across threads and processes, preventing
            // concurrent copy attempts from occurring.
            copyLock.lock()
            if (!databaseFile.exists()) {
                try {
                    // No database file found, copy and be done.
                    copyDatabaseFile(databaseFile, writable)
                    return
                } catch (e: IOException) {
                    throw RuntimeException("Unable to copy database file.", e)
                }
            }

            // A database file is present, check if we need to re-copy it.
            val currentVersion = try {
                readVersion(databaseFile)
            } catch (e: IOException) {
                Log.w(LOG_TAG, "Unable to read database version.", e)
                return
            }
            if (currentVersion == databaseVersion) {
                return
            }
            if (databaseConfiguration.isMigrationRequired(currentVersion, databaseVersion)) {
                // From the current version to the desired version a migration is required, i.e.
                // we won't be performing a copy destructive migration.
                return
            }
            if (context.deleteDatabase(name)) {
                try {
                    copyDatabaseFile(databaseFile, writable)
                } catch (e: IOException) {
                    // We are more forgiving copying a database on a destructive migration since
                    // there is already a database file that can be opened.
                    Log.w(LOG_TAG, "Unable to copy database file.", e)
                }
            } else {
                Log.w(
                    LOG_TAG, "Failed to delete database file ($name) for " +
                        "a copy destructive migration."
                )
            }
        } finally {
            copyLock.unlock()
        }
    }

    @Throws(IOException::class)
    private fun copyDatabaseFile(destinationFile: File, writable: Boolean) {
        val input: ReadableByteChannel
        if (copyFromAssetPath != null) {
            input = Channels.newChannel(context.assets.open(copyFromAssetPath))
        } else if (copyFromFile != null) {
            input = FileInputStream(copyFromFile).channel
        } else if (copyFromInputStream != null) {
            val inputStream = try {
                copyFromInputStream.call()
            } catch (e: Exception) {
                throw IOException("inputStreamCallable exception on call", e)
            }
            input = Channels.newChannel(inputStream)
        } else {
            throw IllegalStateException(
                "copyFromAssetPath, copyFromFile and copyFromInputStream are all null!"
            )
        }

        // An intermediate file is used so that we never end up with a half-copied database file
        // in the internal directory.
        val intermediateFile = File.createTempFile(
            "room-copy-helper", ".tmp", context.cacheDir
        )
        intermediateFile.deleteOnExit()
        val output = FileOutputStream(intermediateFile).channel
        copy(input, output)
        val parent = destinationFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw IOException(
                "Failed to create directories for ${destinationFile.absolutePath}"
            )
        }

        // Temporarily open intermediate database file using FrameworkSQLiteOpenHelper and dispatch
        // the open pre-packaged callback. If it fails then intermediate file won't be copied making
        // invoking pre-packaged callback a transactional operation.
        dispatchOnOpenPrepackagedDatabase(intermediateFile, writable)
        if (!intermediateFile.renameTo(destinationFile)) {
            throw IOException(
                "Failed to move intermediate file (${intermediateFile.absolutePath}) to " +
                    "destination (${destinationFile.absolutePath})."
            )
        }
    }

    private fun dispatchOnOpenPrepackagedDatabase(databaseFile: File, writable: Boolean) {
        if (databaseConfiguration.prepackagedDatabaseCallback == null
        ) {
            return
        }
        createFrameworkOpenHelper(databaseFile).use { helper ->
            val db = if (writable) helper.writableDatabase else helper.readableDatabase
            databaseConfiguration.prepackagedDatabaseCallback!!.onOpenPrepackagedDatabase(db)
        }
    }

    private fun createFrameworkOpenHelper(databaseFile: File): SupportSQLiteOpenHelper {
        val version = try {
            readVersion(databaseFile)
        } catch (e: IOException) {
            throw RuntimeException("Malformed database file, unable to read version.", e)
        }
        val factory = FrameworkSQLiteOpenHelperFactory()
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(databaseFile.absolutePath)
            .callback(object : SupportSQLiteOpenHelper.Callback(version.coerceAtLeast(1)) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    // If pre-packaged database has a version < 1 we will open it as if it was
                    // version 1 because the framework open helper does not allow version < 1.
                    // The database will be considered as newly created and onCreate() will be
                    // invoked, but we do nothing and reset the version back so Room later runs
                    // migrations as usual.
                    if (version < 1) {
                        db.version = version
                    }
                }
            })
            .build()
        return factory.create(configuration)
    }
}
