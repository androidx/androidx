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

package com.android.support.room.testing;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.room.DatabaseConfiguration;
import com.android.support.room.Room;
import com.android.support.room.RoomDatabase;
import com.android.support.room.RoomOpenHelper;
import com.android.support.room.migration.Migration;
import com.android.support.room.migration.bundle.DatabaseBundle;
import com.android.support.room.migration.bundle.EntityBundle;
import com.android.support.room.migration.bundle.FieldBundle;
import com.android.support.room.migration.bundle.SchemaBundle;
import com.android.support.room.util.TableInfo;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that can be used in your Instrumentation tests that can create the database in an
 * older schema.
 * <p>
 * You must copy the schema json files (created by passing {@code room.schemaLocation} argument
 * into the annotation processor) into your test assets and pass in the path for that folder into
 * the constructor. This class will read the folder and extract the schemas from there.
 * <pre>
 * android {
 *   defaultConfig {
 *     javaCompileOptions {
 *       annotationProcessorOptions {
 *         arguments = ["room.schemaLocation": "$projectDir/schemas".toString()]
 *       }
 *     }
 *   }
 *   sourceSets {
 *     androidTest.assets.srcDirs += files("$projectDir/schemas".toString())
 *   }
 * }
 * </pre>
 */
public class MigrationTestHelper extends TestWatcher {
    private static final String TAG = "MigrationTestHelper";
    private final Context mContext;
    private final String mAssetsFolder;
    private final SupportSQLiteOpenHelper.Factory mOpenFactory;
    private List<WeakReference<SupportSQLiteDatabase>> mManagedDatabases = new ArrayList<>();
    private boolean mTestStarted;

    /**
     * Creates a new migration helper with an asset folder and the context.
     *
     * @param context      The context to read assets and create the database.
     * @param assetsFolder The asset folder in the assets directory.
     */
    public MigrationTestHelper(Context context, String assetsFolder,
            SupportSQLiteOpenHelper.Factory openFactory) {
        mContext = context;
        if (assetsFolder.endsWith("/")) {
            assetsFolder = assetsFolder.substring(0, assetsFolder.length() - 1);
        }
        mAssetsFolder = assetsFolder;
        mOpenFactory = openFactory;
    }

    @Override
    protected void starting(Description description) {
        super.starting(description);
        mTestStarted = true;
    }

    /**
     * Creates the database in the given version.
     * If the database file already exists, it tries to delete it first. If delete fails, throws
     * an exception.
     *
     * @param name    The name of the database.
     * @param version The version in which the database should be created.
     * @return A database connection which has the schema in the requested version.
     * @throws IOException If it cannot find the schema description in the assets folder.
     */
    @SuppressWarnings("SameParameterValue")
    public SupportSQLiteDatabase createDatabase(String name, int version) throws IOException {
        File dbPath = mContext.getDatabasePath(name);
        if (dbPath.exists()) {
            Log.d(TAG, "deleting database file " + name);
            if (!dbPath.delete()) {
                throw new IllegalStateException("there is a database file and i could not delete"
                        + " it. Make sure you don't have any open connections to that database"
                        + " before calling this method.");
            }
        }
        SchemaBundle schemaBundle = loadSchema(version);
        RoomDatabase.MigrationContainer container = new RoomDatabase.MigrationContainer();
        DatabaseConfiguration configuration = new DatabaseConfiguration(
                mContext, name, mOpenFactory, container);
        RoomOpenHelper roomOpenHelper = new RoomOpenHelper(configuration,
                new CreatingDelegate(schemaBundle.getDatabase()),
                schemaBundle.getDatabase().getIdentityHash());
        return openDatabase(name, version, roomOpenHelper);
    }

    /**
     * Runs the given set of migrations on the provided database.
     * <p>
     * It uses the same algorithm that Room uses to choose migrations so the migrations instances
     * that are provided to this method must be sufficient to bring the database from current
     * version to the desired version.
     * <p>
     * After the migration, the method validates the database schema to ensure that migration
     * result matches the expected schema. Handling of dropped tables depends on the
     * {@code validateDroppedTables} argument. If set to true, the verification will fail if it
     * finds a table that is not registered in the Database. If set to false, extra tables in the
     * database will be ignored (this is the runtime library behavior).
     *
     * @param name       The database name. You must first create this database via
     *                   {@link #createDatabase(String, int)}.
     * @param version    The final version after applying the migrations.
     * @param validateDroppedTables If set to true, validation will fail if the database has unknown
     *                           tables.
     * @param migrations The list of available migrations.
     * @throws IOException           If it cannot find the schema for {@code toVersion}.
     * @throws IllegalStateException If the schema validation fails.
     */
    public SupportSQLiteDatabase runMigrationsAndValidate(String name, int version,
            boolean validateDroppedTables, Migration... migrations) throws IOException {
        File dbPath = mContext.getDatabasePath(name);
        if (!dbPath.exists()) {
            throw new IllegalStateException("Cannot find the database file for " + name + ". "
                    + "Before calling runMigrations, you must first create the database via "
                    + "createDatabase.");
        }
        SchemaBundle schemaBundle = loadSchema(version);
        RoomDatabase.MigrationContainer container = new RoomDatabase.MigrationContainer();
        container.addMigrations(migrations);
        DatabaseConfiguration configuration = new DatabaseConfiguration(
                mContext, name, mOpenFactory, container);
        RoomOpenHelper roomOpenHelper = new RoomOpenHelper(configuration,
                new MigratingDelegate(schemaBundle.getDatabase(), validateDroppedTables),
                schemaBundle.getDatabase().getIdentityHash());
        return openDatabase(name, version, roomOpenHelper);
    }

    private SupportSQLiteDatabase openDatabase(String name, int version,
            RoomOpenHelper roomOpenHelper) {
        SupportSQLiteOpenHelper.Configuration config =
                SupportSQLiteOpenHelper.Configuration
                        .builder(mContext)
                        .callback(roomOpenHelper)
                        .name(name)
                        .version(version)
                        .build();
        SupportSQLiteDatabase db = mOpenFactory.create(config).getWritableDatabase();
        mManagedDatabases.add(new WeakReference<>(db));
        return db;
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        for (WeakReference<SupportSQLiteDatabase> dbRef : mManagedDatabases) {
            SupportSQLiteDatabase db = dbRef.get();
            if (db != null && db.isOpen()) {
                try {
                    db.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * Registers a database connection to be automatically closed when the test finishes.
     * <p>
     * This only works if {@code MigrationTestHelper} is registered as a Junit test rule via
     * {@link org.junit.Rule Rule} annotation.
     *
     * @param db The database connection that should be closed after the test finishes.
     */
    public void closeWhenFinished(SupportSQLiteDatabase db) {
        if (!mTestStarted) {
            throw new IllegalStateException("You cannot register a database to be closed before"
                    + " the test starts. Maybe you forgot to annotate MigrationTestHelper as a"
                    + " test rule? (@Rule)");
        }
        mManagedDatabases.add(new WeakReference<>(db));
    }

    private SchemaBundle loadSchema(int version) throws IOException {
        InputStream input = mContext.getAssets().open(mAssetsFolder + "/" + version + ".json");
        return SchemaBundle.deserialize(input);
    }

    private static TableInfo toTableInfo(EntityBundle entityBundle) {
        return new TableInfo(entityBundle.getTableName(), toColumnMap(entityBundle));
    }

    private static Map<String, TableInfo.Column> toColumnMap(EntityBundle entity) {
        Map<String, TableInfo.Column> result = new HashMap<>();
        for (FieldBundle bundle : entity.getFields()) {
            TableInfo.Column column = toColumn(entity, bundle);
            result.put(column.name, column);
        }
        return result;
    }

    private static TableInfo.Column toColumn(EntityBundle entity, FieldBundle field) {
        return new TableInfo.Column(field.getColumnName(), field.getAffinity(),
                findPrimaryKeyPosition(entity, field));
    }

    private static int findPrimaryKeyPosition(EntityBundle entity, FieldBundle field) {
        List<String> columnNames = entity.getPrimaryKey().getColumnNames();
        int i = 0;
        for (String columnName : columnNames) {
            i++;
            if (field.getColumnName().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return 0;
    }

    class MigratingDelegate extends RoomOpenHelperDelegate {
        private final boolean mVerifyDroppedTables;
        MigratingDelegate(DatabaseBundle databaseBundle, boolean verifyDroppedTables) {
            super(databaseBundle);
            mVerifyDroppedTables = verifyDroppedTables;
        }

        @Override
        protected void createAllTables(SupportSQLiteDatabase database) {
            throw new UnsupportedOperationException("Was expecting to migrate but received create."
                    + "Make sure you have created the database first.");
        }

        @Override
        protected void validateMigration(SupportSQLiteDatabase db) {
            final Map<String, EntityBundle> tables = mDatabaseBundle.getEntitiesByTableName();
            for (EntityBundle entity : tables.values()) {
                final TableInfo expected = toTableInfo(entity);
                final TableInfo found = TableInfo.read(db, entity.getTableName());
                if (!expected.equals(found)) {
                    throw new IllegalStateException(
                            "Migration failed. expected:" + expected + " , found:" + found);
                }
            }
            if (mVerifyDroppedTables) {
                // now ensure tables that should be removed are removed.
                Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'"
                                + " AND name NOT IN(?, ?)",
                        new String[]{Room.MASTER_TABLE_NAME, "android_metadata"});
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    while (cursor.moveToNext()) {
                        final String tableName = cursor.getString(0);
                        if (!tables.containsKey(tableName)) {
                            throw new IllegalStateException("unexpected table " + tableName);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }

    static class CreatingDelegate extends RoomOpenHelperDelegate {

        CreatingDelegate(DatabaseBundle databaseBundle) {
            super(databaseBundle);
        }

        @Override
        protected void createAllTables(SupportSQLiteDatabase database) {
            for (String query : mDatabaseBundle.buildCreateQueries()) {
                database.execSQL(query);
            }
        }

        @Override
        protected void validateMigration(SupportSQLiteDatabase db) {
            throw new UnsupportedOperationException("This open helper just creates the database but"
                    + " it received a migration request.");
        }
    }

    abstract static class RoomOpenHelperDelegate extends RoomOpenHelper.Delegate {
        final DatabaseBundle mDatabaseBundle;

        RoomOpenHelperDelegate(DatabaseBundle databaseBundle) {
            mDatabaseBundle = databaseBundle;
        }

        @Override
        protected void dropAllTables(SupportSQLiteDatabase database) {
            throw new UnsupportedOperationException("cannot drop all tables in the test");
        }

        @Override
        protected void onOpen(SupportSQLiteDatabase database) {

        }
    }
}
