/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.sqlite.db.framework

import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.sqlite.db.SupportSQLiteCompat
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.util.ProcessLock
import java.io.File
import java.util.UUID

internal class FrameworkSQLiteOpenHelper @JvmOverloads constructor(
    private val context: Context,
    private val name: String?,
    private val callback: SupportSQLiteOpenHelper.Callback,
    private val useNoBackupDirectory: Boolean = false,
    private val allowDataLossOnRecovery: Boolean = false
) : SupportSQLiteOpenHelper {

    // Delegate is created lazily
    private val lazyDelegate = lazy {
        // OpenHelper initialization code
        val openHelper: OpenHelper

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                name != null &&
                useNoBackupDirectory
        ) {
            val file = File(
                SupportSQLiteCompat.Api21Impl.getNoBackupFilesDir(context),
                name
            )
            openHelper = OpenHelper(
                context = context,
                name = file.absolutePath,
                dbRef = DBRefHolder(null),
                callback = callback,
                allowDataLossOnRecovery = allowDataLossOnRecovery
            )
        } else {
            openHelper = OpenHelper(
                context = context,
                name = name,
                dbRef = DBRefHolder(null),
                callback = callback,
                allowDataLossOnRecovery = allowDataLossOnRecovery
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            SupportSQLiteCompat.Api16Impl.setWriteAheadLoggingEnabled(
                openHelper,
                writeAheadLoggingEnabled
            )
        }
        return@lazy openHelper
    }

    private var writeAheadLoggingEnabled = false

    // getDelegate() is lazy because we don't want to File I/O until the call to
    // getReadableDatabase() or getWritableDatabase(). This is better because the call to
    // a getReadableDatabase() or a getWritableDatabase() happens on a background thread unless
    // queries are allowed on the main thread.

    // We defer computing the path the database from the constructor to getDelegate()
    // because context.getNoBackupFilesDir() does File I/O :(
    private val delegate: OpenHelper by lazyDelegate

    override val databaseName: String?
        get() = name

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        if (lazyDelegate.isInitialized()) {
            // Use 'delegate', it is already initialized
            SupportSQLiteCompat.Api16Impl.setWriteAheadLoggingEnabled(delegate, enabled)
        }
        writeAheadLoggingEnabled = enabled
    }

    override val writableDatabase: SupportSQLiteDatabase
        get() = delegate.getSupportDatabase(true)

    override val readableDatabase: SupportSQLiteDatabase
        get() = delegate.getSupportDatabase(false)

    override fun close() {
        if (lazyDelegate.isInitialized()) {
            delegate.close()
        }
    }

    private class OpenHelper(
        val context: Context,
        name: String?,
        /**
         * This is used as an Object reference so that we can access the wrapped database inside
         * the constructor. SQLiteOpenHelper requires the error handler to be passed in the
         * constructor.
         */
        val dbRef: DBRefHolder,
        val callback: SupportSQLiteOpenHelper.Callback,
        val allowDataLossOnRecovery: Boolean
    ) : SQLiteOpenHelper(
        context, name, null, callback.version,
        DatabaseErrorHandler { dbObj ->
            callback.onCorruption(
                getWrappedDb(
                    dbRef,
                    dbObj
                )
            )
        }) {
        // see b/78359448
        private var migrated = false

        // see b/193182592
        private val lock: ProcessLock = ProcessLock(
            name = name ?: UUID.randomUUID().toString(),
            lockDir = context.cacheDir,
            processLock = false
        )
        private var opened = false

        fun getSupportDatabase(writable: Boolean): SupportSQLiteDatabase {
            return try {
                lock.lock(!opened && databaseName != null)
                migrated = false
                val db = innerGetDatabase(writable)
                if (migrated) {
                    // there might be a connection w/ stale structure, we should re-open.
                    close()
                    return getSupportDatabase(writable)
                }
                getWrappedDb(db)
            } finally {
                lock.unlock()
            }
        }

        private fun innerGetDatabase(writable: Boolean): SQLiteDatabase {
            val name = databaseName
            val isOpen = opened
            if (name != null && !isOpen) {
                val databaseFile = context.getDatabasePath(name)
                val parentFile = databaseFile.parentFile
                if (parentFile != null) {
                    parentFile.mkdirs()
                    if (!parentFile.isDirectory) {
                        Log.w(TAG, "Invalid database parent file, not a directory: $parentFile")
                    }
                }
            }
            try {
                return getWritableOrReadableDatabase(writable)
            } catch (t: Throwable) {
                // No good, just try again...
                super.close()
            }
            try {
                // Wait before trying to open the DB, ideally enough to account for some slow I/O.
                // Similar to android_database_SQLiteConnection's BUSY_TIMEOUT_MS but not as much.
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                // Ignore, and continue
            }
            val openRetryError: Throwable = try {
                return getWritableOrReadableDatabase(writable)
            } catch (t: Throwable) {
                super.close()
                t
            }
            if (openRetryError is CallbackException) {
                // Callback error (onCreate, onUpgrade, onOpen, etc), possibly user error.
                val cause = openRetryError.cause
                when (openRetryError.callbackName) {
                    CallbackName.ON_CONFIGURE,
                    CallbackName.ON_CREATE,
                    CallbackName.ON_UPGRADE,
                    CallbackName.ON_DOWNGRADE -> throw cause
                    CallbackName.ON_OPEN -> {}
                }
                // If callback exception is not an SQLiteException, then more certainly it is not
                // recoverable.
                if (cause !is SQLiteException) {
                    throw cause
                }
            } else if (openRetryError is SQLiteException) {
                // Ideally we are looking for SQLiteCantOpenDatabaseException and similar, but
                // corruption can manifest in others forms.
                if (name == null || !allowDataLossOnRecovery) {
                    throw openRetryError
                }
            } else {
                throw openRetryError
            }

            // Delete the database and try one last time. (mAllowDataLossOnRecovery == true)
            context.deleteDatabase(name)
            try {
                return getWritableOrReadableDatabase(writable)
            } catch (ex: CallbackException) {
                // Unwrap our exception to avoid disruption with other try-catch in the call stack.
                throw ex.cause
            }
        }

        private fun getWritableOrReadableDatabase(writable: Boolean): SQLiteDatabase {
            return if (writable) {
                super.getWritableDatabase()
            } else {
                super.getReadableDatabase()
            }
        }

        fun getWrappedDb(sqLiteDatabase: SQLiteDatabase): FrameworkSQLiteDatabase {
            return getWrappedDb(dbRef, sqLiteDatabase)
        }

        override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
            try {
                callback.onCreate(getWrappedDb(sqLiteDatabase))
            } catch (t: Throwable) {
                throw CallbackException(CallbackName.ON_CREATE, t)
            }
        }

        override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            migrated = true
            try {
                callback.onUpgrade(getWrappedDb(sqLiteDatabase), oldVersion, newVersion)
            } catch (t: Throwable) {
                throw CallbackException(CallbackName.ON_UPGRADE, t)
            }
        }

        override fun onConfigure(db: SQLiteDatabase) {
            if (!migrated && callback.version != db.version) {
                // Reduce the prepared statement cache to the minimum allowed (1) to avoid
                // issues with queries executed during migrations. Note that when a migration is
                // done the connection is closed and re-opened to avoid stale connections, which
                // in turns resets the cache max size. See b/271083856
                db.setMaxSqlCacheSize(1)
            }
            try {
                callback.onConfigure(getWrappedDb(db))
            } catch (t: Throwable) {
                throw CallbackException(CallbackName.ON_CONFIGURE, t)
            }
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            migrated = true
            try {
                callback.onDowngrade(getWrappedDb(db), oldVersion, newVersion)
            } catch (t: Throwable) {
                throw CallbackException(CallbackName.ON_DOWNGRADE, t)
            }
        }

        override fun onOpen(db: SQLiteDatabase) {
            if (!migrated) {
                // if we've migrated, we'll re-open the db so we should not call the callback.
                try {
                    callback.onOpen(getWrappedDb(db))
                } catch (t: Throwable) {
                    throw CallbackException(CallbackName.ON_OPEN, t)
                }
            }
            opened = true
        }

        // No need sync due to locks.
        override fun close() {
            try {
                lock.lock()
                super.close()
                dbRef.db = null
                opened = false
            } finally {
                lock.unlock()
            }
        }

        private class CallbackException(
            val callbackName: CallbackName,
            override val cause: Throwable
        ) : RuntimeException(cause)

        internal enum class CallbackName {
            ON_CONFIGURE, ON_CREATE, ON_UPGRADE, ON_DOWNGRADE, ON_OPEN
        }

        companion object {
            fun getWrappedDb(
                refHolder: DBRefHolder,
                sqLiteDatabase: SQLiteDatabase
            ): FrameworkSQLiteDatabase {
                val dbRef = refHolder.db
                return if (dbRef == null || !dbRef.isDelegate(sqLiteDatabase)) {
                    FrameworkSQLiteDatabase(sqLiteDatabase).also { refHolder.db = it }
                } else {
                    dbRef
                }
            }
        }
    }

    companion object {
        private const val TAG = "SupportSQLite"
    }

    /**
     * This is used as an Object reference so that we can access the wrapped database inside
     * the constructor. SQLiteOpenHelper requires the error handler to be passed in the
     * constructor.
     */
    private class DBRefHolder(var db: FrameworkSQLiteDatabase?)
}
