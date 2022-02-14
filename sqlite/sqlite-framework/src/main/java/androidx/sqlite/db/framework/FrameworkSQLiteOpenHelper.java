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

package androidx.sqlite.db.framework;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.sqlite.db.SupportSQLiteCompat;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.util.ProcessLock;
import androidx.sqlite.util.SneakyThrow;

import java.io.File;
import java.util.UUID;

class FrameworkSQLiteOpenHelper implements SupportSQLiteOpenHelper {

    private static final String TAG = "SupportSQLite";

    private final Context mContext;
    private final String mName;
    private final Callback mCallback;
    private final boolean mUseNoBackupDirectory;
    private final boolean mAllowDataLossOnRecovery;
    private final Object mLock;

    // Delegate is created lazily
    private OpenHelper mDelegate;
    private boolean mWriteAheadLoggingEnabled;

    FrameworkSQLiteOpenHelper(
            Context context,
            String name,
            Callback callback) {
        this(context, name, callback, false);
    }

    FrameworkSQLiteOpenHelper(
            Context context,
            String name,
            Callback callback,
            boolean useNoBackupDirectory) {
        this(context, name, callback, useNoBackupDirectory, false);
    }

    FrameworkSQLiteOpenHelper(
            Context context,
            String name,
            Callback callback,
            boolean useNoBackupDirectory,
            boolean allowDataLossOnRecovery) {
        mContext = context;
        mName = name;
        mCallback = callback;
        mUseNoBackupDirectory = useNoBackupDirectory;
        mAllowDataLossOnRecovery = allowDataLossOnRecovery;
        mLock = new Object();
    }

    private OpenHelper getDelegate() {
        // getDelegate() is lazy because we don't want to File I/O until the call to
        // getReadableDatabase() or getWritableDatabase(). This is better because the call to
        // a getReadableDatabase() or a getWritableDatabase() happens on a background thread unless
        // queries are allowed on the main thread.

        // We defer computing the path the database from the constructor to getDelegate()
        // because context.getNoBackupFilesDir() does File I/O :(
        synchronized (mLock) {
            if (mDelegate == null) {
                final FrameworkSQLiteDatabase[] dbRef = new FrameworkSQLiteDatabase[1];
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && mName != null
                        && mUseNoBackupDirectory) {
                    File file = new File(
                            SupportSQLiteCompat.Api21Impl.getNoBackupFilesDir(mContext),
                            mName
                    );
                    mDelegate = new OpenHelper(mContext, file.getAbsolutePath(), dbRef, mCallback,
                            mAllowDataLossOnRecovery);
                } else {
                    mDelegate = new OpenHelper(mContext, mName, dbRef, mCallback,
                            mAllowDataLossOnRecovery);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    SupportSQLiteCompat.Api16Impl.setWriteAheadLoggingEnabled(mDelegate,
                            mWriteAheadLoggingEnabled);
                }
            }
            return mDelegate;
        }
    }

    @Override
    public String getDatabaseName() {
        return mName;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setWriteAheadLoggingEnabled(boolean enabled) {
        synchronized (mLock) {
            if (mDelegate != null) {
                SupportSQLiteCompat.Api16Impl.setWriteAheadLoggingEnabled(mDelegate, enabled);
            }
            mWriteAheadLoggingEnabled = enabled;
        }
    }

    @Override
    public SupportSQLiteDatabase getWritableDatabase() {
        return getDelegate().getSupportDatabase(true);
    }

    @Override
    public SupportSQLiteDatabase getReadableDatabase() {
        return getDelegate().getSupportDatabase(false);
    }

    @Override
    public void close() {
        getDelegate().close();
    }

    static class OpenHelper extends SQLiteOpenHelper {
        /**
         * This is used as an Object reference so that we can access the wrapped database inside
         * the constructor. SQLiteOpenHelper requires the error handler to be passed in the
         * constructor.
         */
        final FrameworkSQLiteDatabase[] mDbRef;
        final Context mContext;
        final Callback mCallback;
        final boolean mAllowDataLossOnRecovery;
        // see b/78359448
        private boolean mMigrated;
        // see b/193182592
        private final ProcessLock mLock;
        private boolean mOpened;

        OpenHelper(Context context, String name, final FrameworkSQLiteDatabase[] dbRef,
                final Callback callback, boolean allowDataLossOnRecovery) {
            super(context, name, null, callback.version,
                    new DatabaseErrorHandler() {
                        @Override
                        public void onCorruption(SQLiteDatabase dbObj) {
                            callback.onCorruption(getWrappedDb(dbRef, dbObj));
                        }
                    });
            mContext = context;
            mCallback = callback;
            mDbRef = dbRef;
            mAllowDataLossOnRecovery = allowDataLossOnRecovery;
            mLock = new ProcessLock(name == null ? UUID.randomUUID().toString() : name,
                    context.getCacheDir(), false);
        }

        SupportSQLiteDatabase getSupportDatabase(boolean writable) {
            try {
                mLock.lock(!mOpened && getDatabaseName() != null);
                mMigrated = false;
                final SQLiteDatabase db = innerGetDatabase(writable);
                if (mMigrated) {
                    // there might be a connection w/ stale structure, we should re-open.
                    close();
                    return getSupportDatabase(writable);
                }
                return getWrappedDb(db);
            } finally {
                mLock.unlock();
            }
        }

        private SQLiteDatabase innerGetDatabase(boolean writable) {
            String name = getDatabaseName();
            if (name != null) {
                File databaseFile = mContext.getDatabasePath(name);
                File parentFile = databaseFile.getParentFile();
                if (parentFile != null) {
                    parentFile.mkdirs();
                    if (!parentFile.isDirectory()) {
                        Log.w(TAG, "Invalid database parent file, not a directory: " + parentFile);
                    }
                }
            }

            try {
                return getWritableOrReadableDatabase(writable);
            } catch (Throwable t) {
                // No good, just try again...
                super.close();
            }

            try {
                // Wait before trying to open the DB, ideally enough to account for some slow I/O.
                // Similar to android_database_SQLiteConnection's BUSY_TIMEOUT_MS but not as much.
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignore, and continue
            }

            final Throwable openRetryError;
            try {
                return getWritableOrReadableDatabase(writable);
            } catch (Throwable t) {
                super.close();
                openRetryError = t;
            }
            if (openRetryError instanceof CallbackException) {
                // Callback error (onCreate, onUpgrade, onOpen, etc), possibly user error.
                final CallbackException callbackException = (CallbackException) openRetryError;
                final Throwable cause = callbackException.getCause();
                switch (callbackException.getCallbackName()) {
                    case ON_CONFIGURE:
                    case ON_CREATE:
                    case ON_UPGRADE:
                    case ON_DOWNGRADE:
                        SneakyThrow.reThrow(cause);
                        break;
                    case ON_OPEN:
                    default:
                        break;
                }
                // If callback exception is not an SQLiteException, then more certainly it is not
                // recoverable.
                if (!(cause instanceof SQLiteException)) {
                    SneakyThrow.reThrow(cause);
                }
            } else if (openRetryError instanceof SQLiteException) {
                // Ideally we are looking for SQLiteCantOpenDatabaseException and similar, but
                // corruption can manifest in others forms.
                if (name == null || !mAllowDataLossOnRecovery) {
                    SneakyThrow.reThrow(openRetryError);
                }
            } else {
                SneakyThrow.reThrow(openRetryError);
            }

            // Delete the database and try one last time. (mAllowDataLossOnRecovery == true)
            mContext.deleteDatabase(name);
            try {
                return getWritableOrReadableDatabase(writable);
            } catch (CallbackException ex) {
                // Unwrap our exception to avoid disruption with other try-catch in the call stack.
                SneakyThrow.reThrow(ex.getCause());
                return null; // Unreachable code, but compiler doesn't know it.
            }
        }

        private SQLiteDatabase getWritableOrReadableDatabase(boolean writable) {
            if (writable) {
                return super.getWritableDatabase();
            } else {
                return super.getReadableDatabase();
            }
        }

        FrameworkSQLiteDatabase getWrappedDb(SQLiteDatabase sqLiteDatabase) {
            return getWrappedDb(mDbRef, sqLiteDatabase);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            try {
                mCallback.onCreate(getWrappedDb(sqLiteDatabase));
            } catch (Throwable t) {
                throw new CallbackException(CallbackName.ON_CREATE, t);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            mMigrated = true;
            try {
                mCallback.onUpgrade(getWrappedDb(sqLiteDatabase), oldVersion, newVersion);
            } catch (Throwable t) {
                throw new CallbackException(CallbackName.ON_UPGRADE, t);
            }
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            try {
                mCallback.onConfigure(getWrappedDb(db));
            } catch (Throwable t) {
                throw new CallbackException(CallbackName.ON_CONFIGURE, t);
            }
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            mMigrated = true;
            try {
                mCallback.onDowngrade(getWrappedDb(db), oldVersion, newVersion);
            } catch (Throwable t) {
                throw new CallbackException(CallbackName.ON_DOWNGRADE, t);
            }
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            if (!mMigrated) {
                // if we've migrated, we'll re-open the db so we should not call the callback.
                try {
                    mCallback.onOpen(getWrappedDb(db));
                } catch (Throwable t) {
                    throw new CallbackException(CallbackName.ON_OPEN, t);
                }
            }
            mOpened = true;
        }

        @Override
        @SuppressWarnings("UnsynchronizedOverridesSynchronized") // No need sync due to locks.
        public void close() {
            try {
                mLock.lock();
                super.close();
                mDbRef[0] = null;
                mOpened = false;
            } finally {
                mLock.unlock();
            }
        }

        static FrameworkSQLiteDatabase getWrappedDb(FrameworkSQLiteDatabase[] refHolder,
                SQLiteDatabase sqLiteDatabase) {
            FrameworkSQLiteDatabase dbRef = refHolder[0];
            if (dbRef == null || !dbRef.isDelegate(sqLiteDatabase)) {
                refHolder[0] = new FrameworkSQLiteDatabase(sqLiteDatabase);
            }
            return refHolder[0];
        }

        private static final class CallbackException extends RuntimeException {

            private final CallbackName mCallbackName;
            private final Throwable mCause;

            CallbackException(CallbackName callbackName, Throwable cause) {
                super(cause);
                mCallbackName = callbackName;
                mCause = cause;
            }

            public CallbackName getCallbackName() {
                return mCallbackName;
            }

            @NonNull
            @Override
            @SuppressWarnings("UnsynchronizedOverridesSynchronized") // Not needed, cause is final
            public Throwable getCause() {
                return mCause;
            }
        }

        enum CallbackName {
            ON_CONFIGURE,
            ON_CREATE,
            ON_UPGRADE,
            ON_DOWNGRADE,
            ON_OPEN
        }
    }
}
