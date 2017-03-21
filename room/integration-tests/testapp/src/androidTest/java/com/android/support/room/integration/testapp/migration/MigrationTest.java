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

package com.android.support.room.integration.testapp.migration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.framework.FrameworkSQLiteOpenHelperFactory;
import com.android.support.room.Room;
import com.android.support.room.migration.Migration;
import com.android.support.room.testing.MigrationTestHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

/**
 * Test custom database migrations.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MigrationTest {
    @Rule
    public MigrationTestHelper helper;

    public MigrationTest() {
        helper = new MigrationTestHelper(InstrumentationRegistry.getContext(),
                MigrationDb.class.getCanonicalName(), new FrameworkSQLiteOpenHelperFactory());
    }

    @Test
    public void startInCurrentVersion() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase("migration-test",
                MigrationDb.LATEST_VERSION);
        final MigrationDb.Dao_V1 dao = new MigrationDb.Dao_V1(db);
        dao.insertIntoVo1(2, "x");
        db.close();
        MigrationDb migrationDb = Room.databaseBuilder(InstrumentationRegistry.getContext(),
                MigrationDb.class, "migration-test").build();
        List<MigrationDb.Vo1> items = migrationDb.dao().loadAllVo1s();
        assertThat(items.size(), is(1));
    }

    @Test
    public void addTable() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase("migration-test", 1);
        final MigrationDb.Dao_V1 dao = new MigrationDb.Dao_V1(db);
        dao.insertIntoVo1(2, "foo");
        dao.insertIntoVo1(3, "bar");
        db.close();

        MigrationDb migrationDb = Room.databaseBuilder(InstrumentationRegistry.getContext(),
                MigrationDb.class,
                "migration-test")
                .addMigrations(new Migration(1, 2) {
                    @Override
                    public void migrate(SupportSQLiteDatabase database) {
                        database.execSQL("CREATE TABLE IF NOT EXISTS `Vo2` (`id` INTEGER,"
                                + " `name` TEXT, PRIMARY KEY(`id`))");
                    }
                }).build();
        List<MigrationDb.Vo1> vo1s = migrationDb.dao().loadAllVo1s();
        assertThat(vo1s.size(), is(2));
        MigrationDb.Vo2 vo2 = new MigrationDb.Vo2();
        vo2.id = 2;
        vo2.name = "bar";
        // assert no error happens
        migrationDb.dao().insert(vo2);
        List<MigrationDb.Vo1> vo2s = migrationDb.dao().loadAllVo2s();
        assertThat(vo2s.size(), is(1));
    }
}
