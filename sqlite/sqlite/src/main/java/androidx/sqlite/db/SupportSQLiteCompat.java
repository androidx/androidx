/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.sqlite.db;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.io.File;
import java.util.List;

/**
 * Helper for accessing features in {@link SupportSQLiteOpenHelper}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SupportSQLiteCompat {
    private SupportSQLiteCompat() { }
    /**
     * Class for accessing functions that require SDK version 16 and higher.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(16)
    public static final class Api16Impl {

        /**
         * Cancels the operation and signals the cancellation listener. If the operation has not yet
         * started, then it will be canceled as soon as it does.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static void cancel(@NonNull CancellationSignal cancellationSignal) {
            cancellationSignal.cancel();
        }

        /**
         * Creates a cancellation signal, initially not canceled.
         *
         * @return a new cancellation signal
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public static CancellationSignal createCancellationSignal() {
            return new CancellationSignal();
        }

        /**
         * Deletes a database including its journal file and other auxiliary files
         * that may have been created by the database engine.
         *
         * @param file The database file path.
         * @return True if the database was successfully deleted.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @SuppressWarnings("StreamFiles")
        public static boolean deleteDatabase(@NonNull File file) {
            return SQLiteDatabase.deleteDatabase(file);
        }

        /**
         * Runs the provided SQL and returns a cursor over the result set.
         *
         * @param sql the SQL query. The SQL string must not be ; terminated
         * @param selectionArgs You may include ?s in where clause in the query,
         *     which will be replaced by the values from selectionArgs. The
         *     values will be bound as Strings.
         * @param editTable the name of the first table, which is editable
         * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
         * If the operation is canceled, then {@link OperationCanceledException} will be thrown
         * when the query is executed.
         * @param cursorFactory the cursor factory to use, or null for the default factory
         * @return A {@link Cursor} object, which is positioned before the first entry. Note that
         * {@link Cursor}s are not synchronized, see the documentation for more details.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public static Cursor rawQueryWithFactory(@NonNull SQLiteDatabase sQLiteDatabase,
                @NonNull String sql, @NonNull String[] selectionArgs,
                @NonNull String editTable, @NonNull CancellationSignal cancellationSignal,
                @NonNull SQLiteDatabase.CursorFactory cursorFactory) {
            return sQLiteDatabase.rawQueryWithFactory(cursorFactory, sql, selectionArgs, editTable,
                    cancellationSignal);
        }

        /**
         * Sets whether foreign key constraints are enabled for the database.
         *
         * @param enable True to enable foreign key constraints, false to disable them.
         *
         * @throws IllegalStateException if the are transactions is in progress
         * when this method is called.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static void setForeignKeyConstraintsEnabled(@NonNull SQLiteDatabase sQLiteDatabase,
                boolean enable) {
            sQLiteDatabase.setForeignKeyConstraintsEnabled(enable);
        }

        /**
         * This method disables the features enabled by
         * {@link SQLiteDatabase#enableWriteAheadLogging()}.
         *
         * @throws IllegalStateException if there are transactions in progress at the
         * time this method is called.  WAL mode can only be changed when there are no
         * transactions in progress.
         *
         * @see SQLiteDatabase#enableWriteAheadLogging
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static void disableWriteAheadLogging(@NonNull SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.disableWriteAheadLogging();
        }

        /**
         * Returns true if write-ahead logging has been enabled for this database.
         *
         * @return True if write-ahead logging has been enabled for this database.
         *
         * @see SQLiteDatabase#enableWriteAheadLogging
         * @see SQLiteDatabase#ENABLE_WRITE_AHEAD_LOGGING
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static boolean isWriteAheadLoggingEnabled(@NonNull SQLiteDatabase sQLiteDatabase) {
            return sQLiteDatabase.isWriteAheadLoggingEnabled();
        }

        /**
         * Sets {@link SQLiteDatabase#ENABLE_WRITE_AHEAD_LOGGING} flag if {@code enabled} is {@code
         * true}, unsets otherwise.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static void setWriteAheadLoggingEnabled(@NonNull SQLiteOpenHelper sQLiteOpenHelper,
                boolean enabled) {
            sQLiteOpenHelper.setWriteAheadLoggingEnabled(enabled);
        }

        private Api16Impl() {}
    }

    /**
     * Helper for accessing functions that require SDK version 19 and higher.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(19)
    public static final class Api19Impl {
        /**
         * Return the URI at which notifications of changes in this Cursor's data
         * will be delivered.
         *
         * @return Returns a URI that can be used with
         * {@link ContentResolver#registerContentObserver(android.net.Uri, boolean, ContentObserver)
         * ContentResolver.registerContentObserver} to find out about changes to this Cursor's
         * data. May be null if no notification URI has been set.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public static Uri getNotificationUri(@NonNull Cursor cursor) {
            return cursor.getNotificationUri();
        }


        /**
         * Returns true if this is a low-RAM device.  Exactly whether a device is low-RAM
         * is ultimately up to the device configuration, but currently it generally means
         * something with 1GB or less of RAM.  This is mostly intended to be used by apps
         * to determine whether they should turn off certain features that require more RAM.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static boolean isLowRamDevice(@NonNull ActivityManager activityManager) {
            return activityManager.isLowRamDevice();
        }

        private Api19Impl() {}
    }

    /**
     * Helper for accessing functions that require SDK version 21 and higher.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(21)
    public static final class Api21Impl {

        /**
         * Returns the absolute path to the directory on the filesystem.
         *
         * @return The path of the directory holding application files that will not
         *         be automatically backed up to remote storage.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public static File getNoBackupFilesDir(@NonNull Context context) {
            return context.getNoBackupFilesDir();
        }

        private Api21Impl() {}
    }

    /**
     * Helper for accessing functions that require SDK version 23 and higher.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(23)
    public static final class Api23Impl {

        /**
         * Sets a {@link Bundle} that will be returned by {@link Cursor#getExtras()}.
         *
         * @param extras {@link Bundle} to set, or null to set an empty bundle.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static void setExtras(@NonNull Cursor cursor, @NonNull Bundle extras) {
            cursor.setExtras(extras);
        }

        private Api23Impl() {}
    }

    /**
     * Helper for accessing functions that require SDK version 29 and higher.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(29)
    public static final class Api29Impl {

        /**
         * Similar to {@link Cursor#setNotificationUri(ContentResolver, Uri)}, except this version
         * allows to watch multiple content URIs for changes.
         *
         * @param cr The content resolver from the caller's context. The listener attached to
         * this resolver will be notified.
         * @param uris The content URIs to watch.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static void setNotificationUris(@NonNull Cursor cursor, @NonNull ContentResolver cr,
                @NonNull List<Uri> uris) {
            cursor.setNotificationUris(cr, uris);
        }

        /**
         * Return the URIs at which notifications of changes in this Cursor's data
         * will be delivered, as previously set by {@link #setNotificationUris}.
         *
         * @return Returns URIs that can be used with
         * {@link ContentResolver#registerContentObserver(android.net.Uri, boolean, ContentObserver)
         * ContentResolver.registerContentObserver} to find out about changes to this Cursor's
         * data. May be null if no notification URI has been set.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public static List<Uri> getNotificationUris(@NonNull Cursor cursor) {
            return cursor.getNotificationUris();
        }

        private Api29Impl() {}
    }

}
