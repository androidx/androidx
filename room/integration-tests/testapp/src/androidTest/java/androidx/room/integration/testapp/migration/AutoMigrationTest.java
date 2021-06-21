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

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.room.testing.MigrationTestHelper;
import androidx.room.util.TableInfo;
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

    public AutoMigrationTest() {
        helper = new MigrationTestHelper(
                InstrumentationRegistry.getInstrumentation(),
                AutoMigrationDb.class
        );
    }

    // Run this to create the very 1st version of the db.
    public void createFirstVersion() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);
        db.execSQL("INSERT INTO Entity1 (id, name) VALUES (1, 'row1')");
        db.close();
    }

    @Test
    public void goFromV1ToV2() throws IOException {
        createFirstVersion();
        SupportSQLiteDatabase db = helper.runMigrationsAndValidate(
                TEST_DB,
                2,
                true
        );
        final TableInfo info = TableInfo.read(db, AutoMigrationDb.Entity1.TABLE_NAME);
        assertThat(info.columns.size(), is(3));
    }

    /**
     * Verifies that the user defined migration is selected over using an autoMigration.
     */
    @Test
    public void testAutoMigrationsNotProcessedBeforeCustomMigrations() throws IOException {
        helper.runMigrationsAndValidate(
                TEST_DB,
                2,
                true,
                MIGRATION_1_2
        );
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `Entity0` ADD COLUMN `addedInV2` INTEGER NOT NULL "
                    + "DEFAULT 2");
        }
    };
}
