/*
 * Copyright (C) 2018 The Android Open Source Project
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
package androidx.room.integration.testapp.migration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;

import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * reproduces b/78359448
 */
@SdkSuppress(minSdkVersion = 24)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class JournalDbPostMigrationTest {
    @Rule
    public CountingTaskExecutorRule executorRule = new CountingTaskExecutorRule();
    private static final String DB_NAME = "journal-db";
    private AtomicInteger mOnOpenCount = new AtomicInteger(0);
    private AtomicInteger mOnCreateCount = new AtomicInteger(0);

    @Entity
    public static class User {
        @PrimaryKey(autoGenerate = true)
        public int uid;

        @ColumnInfo(name = "first_name")
        public String firstName;

        @ColumnInfo(name = "last_name")
        public String lastName;

        @ColumnInfo(name = "address")
        public String address;

        @ColumnInfo(name = "age")
        public int age;
    }

    @Dao
    public interface UserDao {
        @Query("SELECT * FROM user")
        List<User> getAll();

        @Query("SELECT * FROM user WHERE uid = :uid LIMIT 1")
        LiveData<User> getUser(int uid);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(User user);
    }

    @Database(entities = {User.class}, version = 3, exportSchema = false)
    public abstract static class AppDatabase extends RoomDatabase {
        public abstract UserDao userDao();
    }

    private static Migration sMigrationV1toV2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE user ADD COLUMN `address` TEXT");
            Cursor cursor = database.query("SELECT * FROM user");
            while (cursor.moveToNext()) {
                String authCode = cursor.getString(cursor.getColumnIndex("address"));
                if (TextUtils.isEmpty(authCode)) {
                    int id = cursor.getInt(cursor.getColumnIndex("uid"));
                    ContentValues values = new ContentValues();
                    values.put("address", UUID.randomUUID().toString());
                    database.update(
                            "user", SQLiteDatabase.CONFLICT_IGNORE, values,
                            "uid = " + id, null);
                }
            }
        }
    };
    private static Migration sMigrationV2toV3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE user ADD COLUMN `age` INTEGER NOT NULL DEFAULT 0");
        }
    };

    private AppDatabase getDb() {
        return Room.databaseBuilder(InstrumentationRegistry.getTargetContext(),
                AppDatabase.class, "journal-db")
                .addMigrations(sMigrationV1toV2, sMigrationV2toV3)
                .addCallback(new RoomDatabase.Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        mOnCreateCount.incrementAndGet();
                    }

                    @Override
                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        mOnOpenCount.incrementAndGet();
                    }
                })
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE).build();
    }

    private void copyAsset(String path, File outFile) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        InputStream fis = InstrumentationRegistry.getContext().getAssets().open(path);
        OutputStream out = new FileOutputStream(outFile, false);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            while ((length = fis.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } finally {
            fis.close();
            out.close();
        }
    }

    @After
    public void deleteDb() {
        InstrumentationRegistry.getTargetContext().deleteDatabase(DB_NAME);
    }

    @Before
    public void copyDbFromAssets() throws IOException {
        Context testContext = InstrumentationRegistry.getContext();
        Context targetContext = InstrumentationRegistry.getTargetContext();
        String[] walModificationDbs = testContext.getAssets().list(DB_NAME);
        File databasePath = targetContext.getDatabasePath(DB_NAME);
        for (String file : walModificationDbs) {
            copyAsset(DB_NAME + "/" + file,
                    new File(databasePath.getParentFile(), file));
        }
    }

    @Test
    public void migrateAndRead() {
        List<User> users = getDb().userDao().getAll();
        assertThat(users.size(), is(10));
    }

    @Test
    public void checkCallbacks() {
        // trigger db open
        getDb().userDao().getAll();
        assertThat(mOnOpenCount.get(), is(1));
        assertThat(mOnCreateCount.get(), is(0));
    }

    @Test
    public void liveDataPostMigrations() throws TimeoutException, InterruptedException {
        UserDao dao = getDb().userDao();
        LiveData<User> liveData = dao.getUser(3);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                liveData.observeForever(user -> {
                })
        );
        executorRule.drainTasks(1, TimeUnit.MINUTES);
        assertThat(liveData.getValue(), notNullValue());
        // update user
        User user = new User();
        user.uid = 3;
        user.firstName = "foo-bar";
        dao.insert(user);
        executorRule.drainTasks(1, TimeUnit.MINUTES);
        //noinspection ConstantConditions
        assertThat(liveData.getValue().firstName, is("foo-bar"));
    }
}
