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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.dao.ProductDao;
import androidx.room.integration.testapp.vo.Product;
import androidx.room.integration.testapp.vo.User;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class DatabaseCallbackTest {

    @Test
    public void createAndOpen() {
        Context context = ApplicationProvider.getApplicationContext();
        TestDatabaseCallback callback1 = new TestDatabaseCallback();
        TestDatabase db1 = null;
        TestDatabase db2 = null;
        try {
            db1 = Room.databaseBuilder(context, TestDatabase.class, "test")
                    .addCallback(callback1)
                    .build();
            assertFalse(callback1.mCreated);
            assertFalse(callback1.mOpened);
            User user1 = TestUtil.createUser(3);
            user1.setName("george");
            db1.getUserDao().insert(user1);
            assertTrue(callback1.mCreated);
            assertTrue(callback1.mOpened);
            TestDatabaseCallback callback2 = new TestDatabaseCallback();
            db2 = Room.databaseBuilder(context, TestDatabase.class, "test")
                    .addCallback(callback2)
                    .build();
            assertFalse(callback2.mCreated);
            assertFalse(callback2.mOpened);
            User user2 = db2.getUserDao().load(3);
            assertThat(user2.getName(), is("george"));
            assertFalse(callback2.mCreated); // Not called; already created by db1
            assertTrue(callback2.mOpened);
        } finally {
            if (db1 != null) {
                db1.close();
            }
            if (db2 != null) {
                db2.close();
            }
            assertTrue(context.deleteDatabase("test"));
        }
    }

    @Test
    public void writeOnCreate() {
        Context context = ApplicationProvider.getApplicationContext();
        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .addCallback(new RoomDatabase.Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        Cursor cursor = null;
                        try {
                            cursor = db.query(
                                    "SELECT name FROM sqlite_master WHERE type = 'table'");
                            ArrayList<String> names = new ArrayList<>();
                            while (cursor.moveToNext()) {
                                names.add(cursor.getString(0));
                            }
                            assertThat(names, hasItem("User"));
                        } finally {
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    }
                })
                .build();
        List<Integer> ids = db.getUserDao().loadIds();
        assertThat(ids, is(empty()));
    }

    @Test
    public void exceptionOnCreate() {
        Context context = ApplicationProvider.getApplicationContext();
        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .addCallback(new RoomDatabase.Callback() {
                    boolean mIsBadInsertDone;

                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        if (!mIsBadInsertDone) {
                            mIsBadInsertDone = true;
                            db.insert("fake_table",
                                    SQLiteDatabase.CONFLICT_NONE,
                                    new ContentValues());
                        }
                    }
                })
                .build();

        try {
            db.getUserDao().loadIds();
        } catch (SQLiteException e) {
            // Simulate user catching DB exceptions.
        }

        // Should not throw an "IllegalStateException: attempt to re-open an already-closed"
        List<Integer> ids = db.getUserDao().loadIds();
        assertThat(ids, is(empty()));
    }

    @Test
    public void corruptExceptionOnCreate() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();

        TestDatabaseCallback callback = new TestDatabaseCallback();

        // Create fake DB files that will cause a SQLiteDatabaseCorruptException: SQLITE_NOTADB.
        String[] dbFiles = new String[]{"corrupted", "corrupted-shm", "corrupted-wal"};
        for (String fileName : dbFiles) {
            File dbFile = context.getDatabasePath(fileName);
            try (FileWriter fileWriter = new FileWriter(dbFile)) {
                fileWriter.write(new char[]{'p', 'o', 'i', 's', 'o', 'n'});
            }
        }

        TestDatabase db = Room.databaseBuilder(context, TestDatabase.class, "corrupted")
                .addCallback(callback)
                .build();

        assertFalse(callback.mCreated);
        assertFalse(callback.mOpened);

        // Should not throw a SQLiteDatabaseCorruptException, i.e. default onCorruption() was
        // executed and DB file was re-created.
        List<Integer> ids = db.getUserDao().loadIds();
        db.close();
        assertThat(ids, is(empty()));

        assertTrue(callback.mCreated);
        assertTrue(callback.mOpened);
    }

    @Test
    public void onDestructiveMigration_calledOnUpgrade() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("products.db");
        TestDatabaseCallback callback = new TestDatabaseCallback();
        ProductsDatabase_v2 database = Room.databaseBuilder(
                context, ProductsDatabase_v2.class, "products.db")
                .createFromAsset("databases/products_v1.db")
                .addCallback(callback)
                .fallbackToDestructiveMigration(false)
                .build();

        assertFalse(callback.mDestructivelyMigrated);

        // Use the database to trigger the opening and migration of the database
        ProductDao dao = database.getProductDao();
        dao.countProducts();

        assertTrue(callback.mDestructivelyMigrated);
        database.close();
    }

    @Database(entities = Product.class, version = 2, exportSchema = false)
    abstract static class ProductsDatabase_v2 extends RoomDatabase {
        abstract ProductDao getProductDao();
    }

    public static class TestDatabaseCallback extends RoomDatabase.Callback {

        boolean mCreated;
        boolean mOpened;
        boolean mDestructivelyMigrated;

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            mCreated = true;
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            mOpened = true;
        }

        @Override
        public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
            mDestructivelyMigrated = true;
        }
    }
}
