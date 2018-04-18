/*
 * Copyright 2018 The Android Open Source Project
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Insert;
import androidx.room.InvalidationTracker;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
@RunWith(AndroidJUnit4.class)
public class ClearAllTablesTest {

    @Database(version = 1, entities = {Parent.class, Child.class}, exportSchema = false)
    public abstract static class ClearAllTablesDatabase extends RoomDatabase {
        abstract ClearAllTablesDao dao();
    }

    @Entity
    public static class Parent {
        @PrimaryKey
        public long id;
        public String name;

        public Parent(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Entity(foreignKeys = {
            @ForeignKey(entity = Parent.class, parentColumns = "id", childColumns = "parentId")})
    public static class Child {
        @PrimaryKey
        public long id;
        public String name;
        public long parentId;

        public Child(long id, String name, long parentId) {
            this.id = id;
            this.name = name;
            this.parentId = parentId;
        }
    }

    @Dao
    public interface ClearAllTablesDao {
        @Insert
        void insertParent(Parent parent);

        @Insert
        void insertChild(Child child);

        @Query("SELECT COUNT(*) FROM Parent")
        int countParent();

        @Query("SELECT COUNT(*) FROM Child")
        int countChild();
    }

    private ClearAllTablesDatabase mDatabase;
    private ClearAllTablesDao mDao;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getTargetContext();
        mDatabase = Room.inMemoryDatabaseBuilder(context, ClearAllTablesDatabase.class).build();
        mDao = mDatabase.dao();
    }

    @After
    public void closeDatabase() {
        mDatabase.close();
    }

    @Test
    @SmallTest
    public void simple() {
        mDao.insertParent(new Parent(1, "A"));
        assertThat(mDao.countParent(), is(1));
        mDatabase.clearAllTables();
        assertThat(mDao.countParent(), is(0));
    }

    @Test
    @SmallTest
    public void inTransaction() {
        mDao.insertParent(new Parent(1, "A"));
        assertThat(mDao.countParent(), is(1));
        // Running clearAllTables in a transaction is not recommended, but we should not crash.
        mDatabase.runInTransaction(() -> mDatabase.clearAllTables());
        assertThat(mDao.countParent(), is(0));
    }

    @Test
    @SmallTest
    public void inMainThread() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                mDatabase.clearAllTables();
                fail("Was expecting an exception");
            } catch (IllegalStateException e) {
                assertThat(e.getMessage(), containsString("main thread"));
            }
        });
    }

    @Test
    @SmallTest
    public void foreignKey() {
        mDao.insertParent(new Parent(1, "A"));
        mDao.insertChild(new Child(1, "a", 1));
        assertThat(mDao.countParent(), is(1));
        assertThat(mDao.countChild(), is(1));
        mDatabase.clearAllTables();
        assertThat(mDao.countParent(), is(0));
        assertThat(mDao.countChild(), is(0));
    }

    @Test
    @SmallTest
    public void observer() throws InterruptedException {
        mDao.insertParent(new Parent(1, "A"));
        assertThat(mDao.countParent(), is(1));
        final CountDownLatch latch = new CountDownLatch(1);
        mDatabase.getInvalidationTracker().addObserver(new InvalidationTracker.Observer("Parent") {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                assertThat(tables, hasSize(1));
                assertThat(tables, hasItem("Parent"));
                assertThat(mDao.countParent(), is(0));
                latch.countDown();
            }
        });
        mDatabase.clearAllTables();
        assertThat(latch.await(3000, TimeUnit.MILLISECONDS), is(true));
    }

    @Test
    @MediumTest
    public void clearsDataFromDiskTruncate() throws IOException {
        clearsDataFromDisk(RoomDatabase.JournalMode.TRUNCATE);
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 16)
    public void clearsDataFromDiskWal() throws IOException {
        clearsDataFromDisk(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING);
    }

    private void clearsDataFromDisk(RoomDatabase.JournalMode journalMode) throws IOException {
        final Context context = InstrumentationRegistry.getTargetContext();
        final String dbName = "clear.db";
        context.deleteDatabase(dbName);
        ClearAllTablesDatabase db;
        db = Room.databaseBuilder(context, ClearAllTablesDatabase.class, dbName)
                .setJournalMode(journalMode).build();
        final File file = context.getDatabasePath(dbName);
        final String uuid = UUID.randomUUID().toString();
        db.dao().insertParent(new Parent(1, uuid));
        assertThat(queryEncoding(db), is(equalTo("UTF-8")));
        db.close();
        assertThat(fileContainsString(file, uuid), is(true));
        db = Room.databaseBuilder(context, ClearAllTablesDatabase.class, dbName)
                .setJournalMode(journalMode).build();
        db.clearAllTables();
        db.close();
        assertThat(fileContainsString(file, uuid), is(false));
    }

    private String queryEncoding(RoomDatabase db) {
        Cursor c = null;
        try {
            c = db.query("PRAGMA encoding", null);
            c.moveToFirst();
            return c.getString(0);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private boolean fileContainsString(File file, String s) throws IOException {
        final byte[] content = new byte[(int) file.length()];
        final FileInputStream stream = new FileInputStream(file);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            assertThat(stream.read(content), is(content.length));
            return new String(content, Charset.forName("UTF-8")).indexOf(s) > 0;
        } finally {
            stream.close();
        }
    }
}
