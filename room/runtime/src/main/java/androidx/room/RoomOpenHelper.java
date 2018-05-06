/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.util.List;

/**
 * An open helper that holds a reference to the configuration until the database is opened.
 *
 * @hide
 */
@SuppressWarnings("unused")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RoomOpenHelper extends SupportSQLiteOpenHelper.Callback {
    @Nullable
    private DatabaseConfiguration mConfiguration;
    @NonNull
    private final Delegate mDelegate;
    @NonNull
    private final String mIdentityHash;
    /**
     * Room v1 had a bug where the hash was not consistent if fields are reordered.
     * The new has fixes it but we still need to accept the legacy hash.
     */
    @NonNull // b/64290754
    private final String mLegacyHash;

    public RoomOpenHelper(@NonNull DatabaseConfiguration configuration, @NonNull Delegate delegate,
            @NonNull String identityHash, @NonNull String legacyHash) {
        super(delegate.version);
        mConfiguration = configuration;
        mDelegate = delegate;
        mIdentityHash = identityHash;
        mLegacyHash = legacyHash;
    }

    public RoomOpenHelper(@NonNull DatabaseConfiguration configuration, @NonNull Delegate delegate,
            @NonNull String legacyHash) {
        this(configuration, delegate, null, legacyHash);
    }

    @Override
    public void onConfigure(SupportSQLiteDatabase db) {
        super.onConfigure(db);
    }

    @Override
    public void onCreate(SupportSQLiteDatabase db) {
        updateIdentity(db);
        mDelegate.createAllTables(db);
        mDelegate.onCreate(db);
    }

    @Override
    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
        boolean migrated = false;
        if (mConfiguration != null) {
            List<Migration> migrations = mConfiguration.migrationContainer.findMigrationPath(
                    oldVersion, newVersion);
            if (migrations != null) {
                for (Migration migration : migrations) {
                    migration.migrate(db);
                }
                mDelegate.validateMigration(db);
                updateIdentity(db);
                migrated = true;
            }
        }
        if (!migrated) {
            if (mConfiguration != null && !mConfiguration.isMigrationRequiredFrom(oldVersion)) {
                mDelegate.dropAllTables(db);
                mDelegate.createAllTables(db);
            } else {
                throw new IllegalStateException("A migration from " + oldVersion + " to "
                        + newVersion + " was required but not found. Please provide the "
                        + "necessary Migration path via "
                        + "RoomDatabase.Builder.addMigration(Migration ...) or allow for "
                        + "destructive migrations via one of the "
                        + "RoomDatabase.Builder.fallbackToDestructiveMigration* methods.");
            }
        }
    }

    @Override
    public void onDowngrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onOpen(SupportSQLiteDatabase db) {
        super.onOpen(db);
        checkIdentity(db);
        mDelegate.onOpen(db);
        // there might be too many configurations etc, just clear it.
        mConfiguration = null;
    }

    private void checkIdentity(SupportSQLiteDatabase db) {
        String identityHash = null;
        if (hasRoomMasterTable(db)) {
            Cursor cursor = db.query(new SimpleSQLiteQuery(RoomMasterTable.READ_QUERY));
            //noinspection TryFinallyCanBeTryWithResources
            try {
                if (cursor.moveToFirst()) {
                    identityHash = cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        if (!mIdentityHash.equals(identityHash) && !mLegacyHash.equals(identityHash)) {
            throw new IllegalStateException("Room cannot verify the data integrity. Looks like"
                    + " you've changed schema but forgot to update the version number. You can"
                    + " simply fix this by increasing the version number.");
        }
    }

    private void updateIdentity(SupportSQLiteDatabase db) {
        createMasterTableIfNotExists(db);
        db.execSQL(RoomMasterTable.createInsertQuery(mIdentityHash));
    }

    private void createMasterTableIfNotExists(SupportSQLiteDatabase db) {
        db.execSQL(RoomMasterTable.CREATE_QUERY);
    }

    private static boolean hasRoomMasterTable(SupportSQLiteDatabase db) {
        Cursor cursor = db.query("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name='"
                + RoomMasterTable.TABLE_NAME + "'");
        //noinspection TryFinallyCanBeTryWithResources
        try {
            return cursor.moveToFirst() && cursor.getInt(0) != 0;
        } finally {
            cursor.close();
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract static class Delegate {
        public final int version;

        public Delegate(int version) {
            this.version = version;
        }

        protected abstract void dropAllTables(SupportSQLiteDatabase database);

        protected abstract void createAllTables(SupportSQLiteDatabase database);

        protected abstract void onOpen(SupportSQLiteDatabase database);

        protected abstract void onCreate(SupportSQLiteDatabase database);

        /**
         * Called after a migration run to validate database integrity.
         *
         * @param db The SQLite database.
         */
        protected abstract void validateMigration(SupportSQLiteDatabase db);
    }

}
