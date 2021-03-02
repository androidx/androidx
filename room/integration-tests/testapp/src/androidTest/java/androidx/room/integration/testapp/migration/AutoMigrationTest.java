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

package androidx.room.integration.testapp.migration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * Test custom database migrations.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AutoMigrationTest {
    private static final String TEST_DB = "auto-migration-test";
    @Rule
    public MigrationTestHelper helper;

    // TODO: (b/181985265) Implement running AutoMigrations and validate.
    public AutoMigrationTest() {
        helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                AutoMigrationDb.class.getCanonicalName());
    }


    // Run this to create the very 1st version of the db.
    public void createFirstVersion() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);
        db.close();
    }

    @Test
    public void addColumnToDatabaseWithOneTable() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1)) {
            db.execSQL("INSERT INTO Entity1 (id, name) VALUES (1, 'row1')");
        }
        AutoMigrationDb autoMigrationDbV2 = getLatestDb();

        assertThat(autoMigrationDbV2.dao().getAllEntity1s().size(), is(1));
    }

    private AutoMigrationDb getLatestDb() {
        AutoMigrationDb db = Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AutoMigrationDb.class, TEST_DB).build();
        db.getOpenHelper().getWritableDatabase(); // trigger open
        helper.closeWhenFinished(db);
        return db;
    }
}
