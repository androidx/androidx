/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.Context;

import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.RoomMasterTable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class LegacyIdentityHashTest {

    private Context mTargetContext =
            InstrumentationRegistry.getInstrumentation().getTargetContext();
    private String mDatabaseName = "legacy-test.db";

    @Before
    public void setup() {
        mTargetContext.deleteDatabase(mDatabaseName);
    }

    @After
    public void teardown() {
        mTargetContext.deleteDatabase(mDatabaseName);
    }

    @Test
    public void openDatabaseWithLegacyHash() {
        RoomDatabase.Builder<LegacyDatabase> dbBuilder = Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                LegacyDatabase.class,
                "legacy-test.db"
        );

        LegacyDatabase newDb = dbBuilder.build();
        String insertQuery =
                RoomMasterTable.createInsertQuery("d5249b2a35eb34d6c54d25ca1b7b9b74");
        newDb.getOpenHelper().getWritableDatabase().execSQL(insertQuery);
        newDb.close();

        LegacyDatabase legacyDb = dbBuilder.build();
        legacyDb.getOpenHelper().getWritableDatabase(); // force open db
        legacyDb.close();
    }

    @Database(entities = TestDataEntity.class, version = 1, exportSchema = false)
    abstract static class LegacyDatabase extends RoomDatabase {

    }

    @Entity
    static class TestDataEntity {
        @PrimaryKey @SuppressWarnings("unused") Long mId;
    }
}
