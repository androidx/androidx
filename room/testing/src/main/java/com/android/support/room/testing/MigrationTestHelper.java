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
import android.util.Log;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.room.migration.bundle.DatabaseBundle;
import com.android.support.room.migration.bundle.SchemaBundle;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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
 *
 */
public class MigrationTestHelper extends TestWatcher {
    private static final String TAG = "MigrationTestHelper";
    private final Context mContext;
    private final String mAssetsFolder;
    private final SupportSQLiteOpenHelper.Factory mOpenFactory;
    private List<WeakReference<SupportSQLiteDatabase>> mCreatedDatabases = new ArrayList<>();

    /**
     * Creates a new migration helper with an asset folder and the context.
     *
     * @param context The context to read assets and create the database.
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

    /**
     * Creates the database in the given version.
     * If the database file already exists, it tries to delete it first. If delete fails, throws
     * an exception.
     *
     * @param name The name of the database.
     * @param version The version in which the database should be created.
     *
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
        SupportSQLiteOpenHelper.Configuration config =
                SupportSQLiteOpenHelper.Configuration
                        .builder(mContext)
                        .callback(new SchemaOpenCallback(schemaBundle.getDatabase()))
                        .name(name)
                        .version(version)
                        .build();
        SupportSQLiteDatabase db = mOpenFactory.create(config).getWritableDatabase();
        mCreatedDatabases.add(new WeakReference<>(db));
        return db;
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        for (WeakReference<SupportSQLiteDatabase> dbRef : mCreatedDatabases) {
            SupportSQLiteDatabase db = dbRef.get();
            if (db != null && db.isOpen()) {
                try {
                    db.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private SchemaBundle loadSchema(int version) throws IOException {
        InputStream input = mContext.getAssets().open(mAssetsFolder + "/" + version + ".json");
        return SchemaBundle.deserialize(input);
    }

    static class SchemaOpenCallback extends SupportSQLiteOpenHelper.Callback {
        private final DatabaseBundle mDatabaseBundle;
        SchemaOpenCallback(DatabaseBundle databaseBundle) {
            mDatabaseBundle = databaseBundle;
        }

        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            for (String query : mDatabaseBundle.buildCreateQueries()) {
                db.execSQL(query);
            }
        }

        @Override
        public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
            throw new UnsupportedOperationException("cannot upgrade when creating database");
        }
    }
}
