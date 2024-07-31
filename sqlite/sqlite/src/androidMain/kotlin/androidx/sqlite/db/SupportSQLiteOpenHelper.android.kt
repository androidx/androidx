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
package androidx.sqlite.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import android.util.Pair
import androidx.sqlite.db.SupportSQLiteOpenHelper.Callback
import androidx.sqlite.db.SupportSQLiteOpenHelper.Factory
import java.io.Closeable
import java.io.File
import java.io.IOException

/**
 * An interface to map the behavior of [android.database.sqlite.SQLiteOpenHelper]. Note that since
 * that class requires overriding certain methods, support implementation uses [Factory.create] to
 * create this and [Callback] to implement the methods that should be overridden.
 */
@Suppress("AcronymName") // SQL is a known term and should remain capitalized
public interface SupportSQLiteOpenHelper : Closeable {
    /**
     * Return the name of the SQLite database being opened, as given to the constructor. `null`
     * indicates an in-memory database.
     */
    public val databaseName: String?

    /**
     * Enables or disables the use of write-ahead logging for the database.
     *
     * See [SupportSQLiteDatabase.enableWriteAheadLogging] for details.
     *
     * Write-ahead logging cannot be used with read-only databases so the value of this flag is
     * ignored if the database is opened read-only.
     *
     * @param enabled True if write-ahead logging should be enabled, false if it should be disabled.
     */
    public fun setWriteAheadLoggingEnabled(enabled: Boolean)

    /**
     * Create and/or open a database that will be used for reading and writing. The first time this
     * is called, the database will be opened and [Callback.onCreate], [Callback.onUpgrade] and/or
     * [Callback.onOpen] will be called.
     *
     * Once opened successfully, the database is cached, so you can call this method every time you
     * need to write to the database. (Make sure to call [close] when you no longer need the
     * database.) Errors such as bad permissions or a full disk may cause this method to fail, but
     * future attempts may succeed if the problem is fixed.
     *
     * Database upgrade may take a long time, you should not call this method from the application
     * main thread, including from [ContentProvider.onCreate()].
     *
     * @return a read/write database object valid until [close] is called
     * @throws SQLiteException if the database cannot be opened for writing
     */
    public val writableDatabase: SupportSQLiteDatabase

    /**
     * Create and/or open a database. This will be the same object returned by [writableDatabase]
     * unless some problem, such as a full disk, requires the database to be opened read-only. In
     * that case, a read-only database object will be returned. If the problem is fixed, a future
     * call to [writableDatabase] may succeed, in which case the read-only database object will be
     * closed and the read/write object will be returned in the future.
     *
     * Like [writableDatabase], this method may take a long time to return, so you should not call
     * it from the application main thread, including from [ContentProvider.onCreate()].
     *
     * @return a database object valid until [writableDatabase] or [close] is called.
     * @throws SQLiteException if the database cannot be opened
     */
    public val readableDatabase: SupportSQLiteDatabase

    /** Close any open database object. */
    override fun close()

    /**
     * Creates a new Callback to get database lifecycle events.
     *
     * Handles various lifecycle events for the SQLite connection, similar to
     * [room-runtime.SQLiteOpenHelper].
     */
    public abstract class Callback(
        /**
         * Version number of the database (starting at 1); if the database is older,
         * [Callback.onUpgrade] will be used to upgrade the database; if the database is newer,
         * [Callback.onDowngrade] will be used to downgrade the database.
         */
        @JvmField public val version: Int
    ) {
        /**
         * Called when the database connection is being configured, to enable features such as
         * write-ahead logging or foreign key support.
         *
         * This method is called before [onCreate], [onUpgrade], [onDowngrade], or [onOpen] are
         * called. It should not modify the database except to configure the database connection as
         * required.
         *
         * This method should only call methods that configure the parameters of the database
         * connection, such as [SupportSQLiteDatabase.enableWriteAheadLogging]
         * [SupportSQLiteDatabase.setForeignKeyConstraintsEnabled],
         * [SupportSQLiteDatabase.setLocale], [SupportSQLiteDatabase.setMaximumSize], or executing
         * PRAGMA statements.
         *
         * @param db The database.
         */
        public open fun onConfigure(db: SupportSQLiteDatabase) {}

        /**
         * Called when the database is created for the first time. This is where the creation of
         * tables and the initial population of the tables should happen.
         *
         * @param db The database.
         */
        public abstract fun onCreate(db: SupportSQLiteDatabase)

        /**
         * Called when the database needs to be upgraded. The implementation should use this method
         * to drop tables, add tables, or do anything else it needs to upgrade to the new schema
         * version.
         *
         * The SQLite ALTER TABLE documentation can be found
         * [here](http://sqlite.org/lang_altertable.html). If you add new columns you can use ALTER
         * TABLE to insert them into a live table. If you rename or remove columns you can use ALTER
         * TABLE to rename the old table, then create the new table and then populate the new table
         * with the contents of the old table.
         *
         * This method executes within a transaction. If an exception is thrown, all changes will
         * automatically be rolled back.
         *
         * @param db The database.
         * @param oldVersion The old database version.
         * @param newVersion The new database version.
         */
        public abstract fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int)

        /**
         * Called when the database needs to be downgraded. This is strictly similar to [onUpgrade]
         * method, but is called whenever current version is newer than requested one. However, this
         * method is not abstract, so it is not mandatory for a customer to implement it. If not
         * overridden, default implementation will reject downgrade and throws SQLiteException
         *
         * This method executes within a transaction. If an exception is thrown, all changes will
         * automatically be rolled back.
         *
         * @param db The database.
         * @param oldVersion The old database version.
         * @param newVersion The new database version.
         */
        public open fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            throw SQLiteException(
                "Can't downgrade database from version $oldVersion to $newVersion"
            )
        }

        /**
         * Called when the database has been opened. The implementation should check
         * [SupportSQLiteDatabase.isReadOnly] before updating the database.
         *
         * This method is called after the database connection has been configured and after the
         * database schema has been created, upgraded or downgraded as necessary. If the database
         * connection must be configured in some way before the schema is created, upgraded, or
         * downgraded, do it in [onConfigure] instead.
         *
         * @param db The database.
         */
        public open fun onOpen(db: SupportSQLiteDatabase) {}

        /**
         * The method invoked when database corruption is detected. Default implementation will
         * delete the database file.
         *
         * @param db the [SupportSQLiteDatabase] object representing the database on which
         *   corruption is detected.
         */
        public open fun onCorruption(db: SupportSQLiteDatabase) {
            // the following implementation is taken from {@link DefaultDatabaseErrorHandler}.
            Log.e(TAG, "Corruption reported by sqlite on database: $db.path")
            // is the corruption detected even before database could be 'opened'?
            if (!db.isOpen) {
                // database files are not even openable. delete this database file.
                // NOTE if the database has attached databases, then any of them could be corrupt.
                // and not deleting all of them could cause corrupted database file to remain and
                // make the application crash on database open operation. To avoid this problem,
                // the application should provide its own {@link DatabaseErrorHandler} impl class
                // to delete ALL files of the database (including the attached databases).
                db.path?.let { deleteDatabaseFile(it) }
                return
            }
            var attachedDbs: List<Pair<String, String>>? = null
            try {
                // Close the database, which will cause subsequent operations to fail.
                // before that, get the attached database list first.
                try {
                    attachedDbs = db.attachedDbs
                } catch (e: SQLiteException) {
                    /* ignore */
                }
                try {
                    db.close()
                } catch (e: IOException) {
                    /* ignore */
                }
            } finally {
                // Delete all files of this corrupt database and/or attached databases
                // attachedDbs = null is possible when the database is so corrupt that even
                // "PRAGMA database_list;" also fails. delete the main database file
                attachedDbs?.forEach { p -> deleteDatabaseFile(p.second) }
                    ?: db.path?.let { deleteDatabaseFile(it) }
            }
        }

        private fun deleteDatabaseFile(fileName: String) {
            if (
                fileName.equals(":memory:", ignoreCase = true) ||
                    fileName.trim { it <= ' ' }.isEmpty()
            ) {
                return
            }
            Log.w(TAG, "deleting the database file: $fileName")
            try {
                SQLiteDatabase.deleteDatabase(File(fileName))
            } catch (e: Exception) {
                /* print warning and ignore exception */
                Log.w(TAG, "delete failed: ", e)
            }
        }

        internal companion object {
            private const val TAG = "SupportSQLite"
        }
    }

    /** The configuration to create an SQLite open helper object using [Factory]. */
    public class Configuration
    @Suppress("ExecutorRegistration") // For backwards compatibility
    constructor(
        /** Context to use to open or create the database. */
        @JvmField public val context: Context,
        /** Name of the database file, or null for an in-memory database. */
        @JvmField public val name: String?,
        /** The callback class to handle creation, upgrade and downgrade. */
        @JvmField public val callback: Callback,
        /** If `true` the database will be stored in the no-backup directory. */
        @JvmField @Suppress("ListenerLast") public val useNoBackupDirectory: Boolean = false,
        /**
         * If `true` the database will be delete and its data loss in the case that it cannot be
         * opened.
         */
        @JvmField @Suppress("ListenerLast") public val allowDataLossOnRecovery: Boolean = false
    ) {

        /** Builder class for [Configuration]. */
        public open class Builder internal constructor(context: Context) {
            private val context: Context
            private var name: String? = null
            private var callback: Callback? = null
            private var useNoBackupDirectory = false
            private var allowDataLossOnRecovery = false

            /**
             * Throws an [IllegalArgumentException] if the [Callback] is `null`.
             *
             * Throws an [IllegalArgumentException] if the [Context] is `null`.
             *
             * Throws an [IllegalArgumentException] if the [String] database name is `null`.
             * [Context.getNoBackupFilesDir]
             *
             * @return The [Configuration] instance
             */
            public open fun build(): Configuration {
                val callback = callback
                requireNotNull(callback) { "Must set a callback to create the configuration." }
                require(!useNoBackupDirectory || !name.isNullOrEmpty()) {
                    "Must set a non-null database name to a configuration that uses the " +
                        "no backup directory."
                }
                return Configuration(
                    context,
                    name,
                    callback,
                    useNoBackupDirectory,
                    allowDataLossOnRecovery
                )
            }

            init {
                this.context = context
            }

            /**
             * @param name Name of the database file, or null for an in-memory database.
             * @return This builder instance.
             */
            public open fun name(name: String?): Builder = apply { this.name = name }

            /**
             * @param callback The callback class to handle creation, upgrade and downgrade.
             * @return This builder instance.
             */
            public open fun callback(callback: Callback): Builder = apply {
                this.callback = callback
            }

            /**
             * Sets whether to use a no backup directory or not.
             *
             * @param useNoBackupDirectory If `true` the database file will be stored in the
             *   no-backup directory.
             * @return This builder instance.
             */
            public open fun noBackupDirectory(useNoBackupDirectory: Boolean): Builder = apply {
                this.useNoBackupDirectory = useNoBackupDirectory
            }

            /**
             * Sets whether to delete and recreate the database file in situations when the database
             * file cannot be opened, thus allowing for its data to be lost.
             *
             * @param allowDataLossOnRecovery If `true` the database file might be recreated in the
             *   case that it cannot be opened.
             * @return this
             */
            public open fun allowDataLossOnRecovery(allowDataLossOnRecovery: Boolean): Builder =
                apply {
                    this.allowDataLossOnRecovery = allowDataLossOnRecovery
                }
        }

        public companion object {
            /**
             * Creates a new Configuration.Builder to create an instance of Configuration.
             *
             * @param context to use to open or create the database.
             */
            @JvmStatic
            public fun builder(context: Context): Builder {
                return Builder(context)
            }
        }
    }

    /** Factory class to create instances of [SupportSQLiteOpenHelper] using [Configuration]. */
    public fun interface Factory {
        /**
         * Creates an instance of [SupportSQLiteOpenHelper] using the given configuration.
         *
         * @param configuration The configuration to use while creating the open helper.
         * @return A SupportSQLiteOpenHelper which can be used to open a database.
         */
        public fun create(configuration: Configuration): SupportSQLiteOpenHelper
    }
}
