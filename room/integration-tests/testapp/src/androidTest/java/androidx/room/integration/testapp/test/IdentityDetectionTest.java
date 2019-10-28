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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.util.Log;

import androidx.room.Room;
import androidx.room.integration.testapp.TestDatabase;
import androidx.room.integration.testapp.vo.User;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class IdentityDetectionTest {
    static final String TAG = "IdentityDetectionTest";
    static final String DB_FILE_NAME = "identity_test_db";
    TestDatabase mTestDatabase;
    @Before
    public void createTestDatabase() {
        deleteDbFile();
    }

    @Test
    public void reOpenWithoutIssues() {
        openDb();
        mTestDatabase.getUserDao().insert(TestUtil.createUser(3));
        closeDb();
        openDb();
        User[] users = mTestDatabase.getUserDao().loadByIds(3);
        assertThat(users.length, is(1));
    }

    @Test
    public void reOpenChangedHash() {
        openDb();
        mTestDatabase.getUserDao().insert(TestUtil.createUser(3));
        // change the hash
        SupportSQLiteDatabase db = mTestDatabase.getOpenHelper().getWritableDatabase();
        db.execSQL("UPDATE " + Room.MASTER_TABLE_NAME + " SET `identity_hash` = ?"
                + " WHERE id = 42", new String[]{"bad hash"});
        closeDb();
        Throwable[] exceptions = new Throwable[1];
        try {
            openDb();
            mTestDatabase.getUserDao().loadByIds(3);
        } catch (Throwable t) {
            exceptions[0] = t;
            mTestDatabase = null;
        }
        assertThat(exceptions[0], instanceOf(IllegalStateException.class));
    }

    @Test
    public void reOpenMasterTableDropped() {
        openDb();
        mTestDatabase.getUserDao().insert(TestUtil.createUser(3));
        // drop the master table
        SupportSQLiteDatabase db = mTestDatabase.getOpenHelper().getWritableDatabase();
        db.execSQL("DROP TABLE " + Room.MASTER_TABLE_NAME);
        closeDb();

        openDb();
        mTestDatabase.getUserDao().loadByIds(3);
    }

    private void closeDb() {
        mTestDatabase.close();
    }

    private void openDb() {
        mTestDatabase = Room.databaseBuilder(ApplicationProvider.getApplicationContext(),
                TestDatabase.class, DB_FILE_NAME).build();
    }

    @After
    public void clear() {
        try {
            if (mTestDatabase != null) {
                closeDb();
            }
            deleteDbFile();
        } catch (Throwable t) {
            Log.e(TAG, "could not close test database", t);
            throw t;
        }
    }

    private void deleteDbFile() {
        File testDb = ApplicationProvider.getApplicationContext().getDatabasePath(DB_FILE_NAME);
        testDb.delete();
    }
}
