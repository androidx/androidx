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

package androidx.room;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An open helper that will copy & open a pre-populated database if it doesn't exists in internal
 * storage.
 */
class SQLiteCopyOpenHelper implements SupportSQLiteOpenHelper {

    @NonNull
    private final Context mContext;
    @Nullable
    private final String mCopyFromAssetPath;
    @Nullable
    private final File mCopyFromFile;
    private final int mDatabaseVersion;
    @NonNull
    private final SupportSQLiteOpenHelper mDelegate;
    @Nullable
    private DatabaseConfiguration mDatabaseConfiguration;

    private boolean mVerified;

    SQLiteCopyOpenHelper(
            @NonNull Context context,
            @Nullable String copyFromAssetPath,
            @Nullable File copyFromFile,
            int databaseVersion,
            @NonNull SupportSQLiteOpenHelper supportSQLiteOpenHelper) {
        mContext = context;
        mCopyFromAssetPath = copyFromAssetPath;
        mCopyFromFile = copyFromFile;
        mDatabaseVersion = databaseVersion;
        mDelegate = supportSQLiteOpenHelper;
    }

    @Override
    public String getDatabaseName() {
        return mDelegate.getDatabaseName();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setWriteAheadLoggingEnabled(boolean enabled) {
        mDelegate.setWriteAheadLoggingEnabled(enabled);
    }

    @Override
    public synchronized SupportSQLiteDatabase getWritableDatabase() {
        if (!mVerified) {
            verifyDatabaseFile();
            mVerified = true;
        }
        return mDelegate.getWritableDatabase();
    }

    @Override
    public synchronized SupportSQLiteDatabase getReadableDatabase() {
        if (!mVerified) {
            verifyDatabaseFile();
            mVerified = true;
        }
        return mDelegate.getReadableDatabase();
    }

    @Override
    public synchronized void close() {
        mDelegate.close();
        mVerified = false;
    }

    // Can't be constructor param because the factory is needed by the database builder which in
    // turn is the one that actually builds the configuration.
    void setDatabaseConfiguration(@Nullable DatabaseConfiguration databaseConfiguration) {
        mDatabaseConfiguration = databaseConfiguration;
    }

    private void verifyDatabaseFile() {
        String databaseName = getDatabaseName();
        File databaseFile = mContext.getDatabasePath(databaseName);
        if (!databaseFile.exists()) {
            try {
                copyDatabaseFile(databaseFile);
                return;
            } catch (IOException e) {
                throw new RuntimeException("Unable to copy database file.", e);
            }
        }

        if (mDatabaseConfiguration == null) {
            return;
        }

        // A database file is present, check if we need to re-copy it.
        int currentVersion;
        try {
            currentVersion = DBUtil.readVersion(databaseFile);
        } catch (IOException e) {
            Log.w(Room.LOG_TAG, "Unable to read database version.", e);
            return;
        }

        if (currentVersion == mDatabaseVersion) {
            return;
        }

        if (mDatabaseConfiguration.isMigrationRequired(currentVersion, mDatabaseVersion)) {
            return;
        }

        if (mContext.deleteDatabase(databaseName)) {
            try {
                copyDatabaseFile(databaseFile);
            } catch (IOException e) {
                // We are more forgiving copying a database on a destructive migration since there
                // is already a database file that can be opened.
                Log.w(Room.LOG_TAG, "Unable to copy database file.", e);
            }
        }
    }

    private void copyDatabaseFile(File destinationFile) throws IOException {
        File parent = destinationFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create directories for "
                    + destinationFile.getAbsolutePath());
        }

        InputStream input;
        if (mCopyFromAssetPath != null) {
            input = mContext.getAssets().open(mCopyFromAssetPath);
        } else if (mCopyFromFile != null) {
            input = new FileInputStream(mCopyFromFile);
        } else {
            throw new IllegalStateException("copyFromAssetPath and copyFromFile = null!");
        }
        OutputStream output = new FileOutputStream(destinationFile);
        copy(input, output);
    }

    private void copy(InputStream input, OutputStream output) throws IOException {
        try {
            int length;
            byte[] buffer = new byte[1024 * 4];
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        } finally {
            input.close();
            output.close();
        }
    }
}
