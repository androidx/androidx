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

import android.content.Context;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.vo.User;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DatabaseCallbackTest {

    @Test
    @MediumTest
    public void createAndOpen() {
        Context context = InstrumentationRegistry.getTargetContext();
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
    @SmallTest
    public void writeOnCreate() {
        Context context = InstrumentationRegistry.getTargetContext();
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

    public static class TestDatabaseCallback extends RoomDatabase.Callback {

        boolean mCreated;
        boolean mOpened;

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            mCreated = true;
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            mOpened = true;
        }
    }
}
