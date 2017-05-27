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

package android.arch.persistence.db;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.DefaultDatabaseErrorHandler;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

/**
 * An interface to map the behavior of {@link android.database.sqlite.SQLiteOpenHelper}.
 * Note that since that class requires overriding certain methods, support implementation
 * uses {@link Factory#create(Configuration)} to create this and {@link Callback} to implement
 * the methods that should be overridden.
 */
@SuppressWarnings("unused")
public interface SupportSQLiteOpenHelper {
    /**
     * Return the name of the SQLite database being opened, as given to
     * the constructor.
     */
    String getDatabaseName();

    /**
     * Enables or disables the use of write-ahead logging for the database.
     *
     * Write-ahead logging cannot be used with read-only databases so the value of
     * this flag is ignored if the database is opened read-only.
     *
     * @param enabled True if write-ahead logging should be enabled, false if it
     *                should be disabled.
     * @see SupportSQLiteDatabase#enableWriteAheadLogging()
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    void setWriteAheadLoggingEnabled(boolean enabled);

    /**
     * Create and/or open a database that will be used for reading and writing.
     * The first time this is called, the database will be opened and
     * {@link Callback#onCreate}, {@link Callback#onUpgrade} and/or {@link Callback#onOpen} will be
     * called.
     *
     * <p>Once opened successfully, the database is cached, so you can
     * call this method every time you need to write to the database.
     * (Make sure to call {@link #close} when you no longer need the database.)
     * Errors such as bad permissions or a full disk may cause this method
     * to fail, but future attempts may succeed if the problem is fixed.</p>
     *
     * <p class="caution">Database upgrade may take a long time, you
     * should not call this method from the application main thread, including
     * from {@link android.content.ContentProvider#onCreate ContentProvider.onCreate()}.
     *
     * @return a read/write database object valid until {@link #close} is called
     * @throws SQLiteException if the database cannot be opened for writing
     */
    SupportSQLiteDatabase getWritableDatabase();

    /**
     * Create and/or open a database.  This will be the same object returned by
     * {@link #getWritableDatabase} unless some problem, such as a full disk,
     * requires the database to be opened read-only.  In that case, a read-only
     * database object will be returned.  If the problem is fixed, a future call
     * to {@link #getWritableDatabase} may succeed, in which case the read-only
     * database object will be closed and the read/write object will be returned
     * in the future.
     *
     * <p class="caution">Like {@link #getWritableDatabase}, this method may
     * take a long time to return, so you should not call it from the
     * application main thread, including from
     * {@link android.content.ContentProvider#onCreate ContentProvider.onCreate()}.
     *
     * @return a database object valid until {@link #getWritableDatabase}
     * or {@link #close} is called.
     * @throws SQLiteException if the database cannot be opened
     */
    SupportSQLiteDatabase getReadableDatabase();

    /**
     * Close any open database object.
     */
    void close();

    /**
     * Matching callback methods from {@link android.database.sqlite.SQLiteOpenHelper}.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    abstract class Callback {
        /**
         * Called when the database connection is being configured, to enable features such as
         * write-ahead logging or foreign key support.
         * <p>
         * This method is called before {@link #onCreate}, {@link #onUpgrade}, {@link #onDowngrade},
         * or {@link #onOpen} are called. It should not modify the database except to configure the
         * database connection as required.
         * </p>
         * <p>
         * This method should only call methods that configure the parameters of the database
         * connection, such as {@link SupportSQLiteDatabase#enableWriteAheadLogging}
         * {@link SupportSQLiteDatabase#setForeignKeyConstraintsEnabled},
         * {@link SupportSQLiteDatabase#setLocale},
         * {@link SupportSQLiteDatabase#setMaximumSize}, or executing PRAGMA statements.
         * </p>
         *
         * @param db The database.
         */
        public void onConfigure(SupportSQLiteDatabase db) {

        }

        /**
         * Called when the database is created for the first time. This is where the
         * creation of tables and the initial population of the tables should happen.
         *
         * @param db The database.
         */
        public abstract void onCreate(SupportSQLiteDatabase db);

        /**
         * Called when the database needs to be upgraded. The implementation
         * should use this method to drop tables, add tables, or do anything else it
         * needs to upgrade to the new schema version.
         *
         * <p>
         * The SQLite ALTER TABLE documentation can be found
         * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
         * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
         * you can use ALTER TABLE to rename the old table, then create the new table and then
         * populate the new table with the contents of the old table.
         * </p><p>
         * This method executes within a transaction.  If an exception is thrown, all changes
         * will automatically be rolled back.
         * </p>
         *
         * @param db         The database.
         * @param oldVersion The old database version.
         * @param newVersion The new database version.
         */
        public abstract void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion);

        /**
         * Called when the database needs to be downgraded. This is strictly similar to
         * {@link #onUpgrade} method, but is called whenever current version is newer than requested
         * one.
         * However, this method is not abstract, so it is not mandatory for a customer to
         * implement it. If not overridden, default implementation will reject downgrade and
         * throws SQLiteException
         *
         * <p>
         * This method executes within a transaction.  If an exception is thrown, all changes
         * will automatically be rolled back.
         * </p>
         *
         * @param db         The database.
         * @param oldVersion The old database version.
         * @param newVersion The new database version.
         */
        public void onDowngrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
            throw new SQLiteException("Can't downgrade database from version "
                    + oldVersion + " to " + newVersion);
        }

        /**
         * Called when the database has been opened.  The implementation
         * should check {@link SupportSQLiteDatabase#isReadOnly} before updating the
         * database.
         * <p>
         * This method is called after the database connection has been configured
         * and after the database schema has been created, upgraded or downgraded as necessary.
         * If the database connection must be configured in some way before the schema
         * is created, upgraded, or downgraded, do it in {@link #onConfigure} instead.
         * </p>
         *
         * @param db The database.
         */
        public void onOpen(SupportSQLiteDatabase db) {

        }
    }

    /**
     * The configuration to create an SQLite open helper object using {@link Factory}.
     */
    @SuppressWarnings("WeakerAccess")
    class Configuration {
        /**
         * Context to use to open or create the database.
         */
        @NonNull
        public final Context context;
        /**
         * Name of the database file, or null for an in-memory database.
         */
        @Nullable
        public final String name;
        /**
         * Version number of the database (starting at 1); if the database is older,
         * {@link SupportSQLiteOpenHelper.Callback#onUpgrade(SupportSQLiteDatabase, int, int)}
         * will be used to upgrade the database; if the database is newer,
         * {@link SupportSQLiteOpenHelper.Callback#onDowngrade(SupportSQLiteDatabase, int, int)}
         * will be used to downgrade the database.
         */
        public final int version;
        /**
         * The callback class to handle creation, upgrade and downgrade.
         */
        @NonNull
        public final SupportSQLiteOpenHelper.Callback callback;
        /**
         * The {@link DatabaseErrorHandler} to be used when sqlite reports database
         * corruption, or null to use the default error handler.
         */
        @Nullable
        public final DatabaseErrorHandler errorHandler;

        Configuration(@NonNull Context context, @Nullable String name,
                int version, @Nullable DatabaseErrorHandler errorHandler,
                @NonNull Callback callback) {
            this.context = context;
            this.name = name;
            this.version = version;
            this.callback = callback;
            this.errorHandler = errorHandler;
        }

        /**
         * Creates a new Configuration.Builder to create an instance of Configuration.
         *
         * @param context to use to open or create the database.
         */
        public static Builder builder(Context context) {
            return new Builder(context);
        }

        /**
         * Builder class for {@link Configuration}.
         */
        public static class Builder {
            Context mContext;
            String mName;
            int mVersion = 1;
            SupportSQLiteOpenHelper.Callback mCallback;
            DatabaseErrorHandler mErrorHandler;

            public Configuration build() {
                if (mCallback == null) {
                    throw new IllegalArgumentException("Must set a callback to create the"
                            + " configuration.");
                }
                if (mContext == null) {
                    throw new IllegalArgumentException("Must set a non-null context to create"
                            + " the configuration.");
                }
                if (mErrorHandler == null) {
                    mErrorHandler = new DefaultDatabaseErrorHandler();
                }
                return new Configuration(mContext, mName, mVersion, mErrorHandler,
                        mCallback);
            }

            Builder(@NonNull Context context) {
                mContext = context;
            }

            /**
             * @param errorHandler The {@link DatabaseErrorHandler} to be used when sqlite
             *                     reports database corruption, or null to use the default error
             *                     handler.
             * @return This
             */
            public Builder errorHandler(@Nullable DatabaseErrorHandler errorHandler) {
                mErrorHandler = errorHandler;
                return this;
            }

            /**
             * @param name Name of the database file, or null for an in-memory database.
             * @return This
             */
            public Builder name(@Nullable String name) {
                mName = name;
                return this;
            }

            /**
             * @param callback The callback class to handle creation, upgrade and downgrade.
             * @return this
             */
            public Builder callback(@NonNull Callback callback) {
                mCallback = callback;
                return this;
            }

            /**
             * @param version Version number of the database (starting at 1); if the database is
             * older,
             * {@link SupportSQLiteOpenHelper.Callback#onUpgrade(SupportSQLiteDatabase, int, int)}
             * will be used to upgrade the database; if the database is newer,
             * {@link SupportSQLiteOpenHelper.Callback#onDowngrade(SupportSQLiteDatabase, int, int)}
             * will be used to downgrade the database.
             * @return this
             */
            public Builder version(int version) {
                mVersion = version;
                return this;
            }
        }
    }

    /**
     * Factory class to create instances of {@link SupportSQLiteOpenHelper} using
     * {@link Configuration}.
     */
    interface Factory {
        /**
         * Creates an instance of {@link SupportSQLiteOpenHelper} using the given configuration.
         *
         * @param configuration The configuration to use while creating the open helper.
         *
         * @return A SupportSQLiteOpenHelper which can be used to open a database.
         */
        SupportSQLiteOpenHelper create(Configuration configuration);
    }
}
