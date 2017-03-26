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

package com.android.support.room.migration;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.android.support.db.SupportSQLiteDatabase;
import com.android.support.db.SupportSQLiteOpenHelper;
import com.android.support.db.framework.FrameworkSQLiteOpenHelperFactory;
import com.android.support.room.util.TableInfo;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(AndroidJUnit4.class)
@SmallTest
public class TableInfoTest {
    private SupportSQLiteDatabase mDb;

    @Test
    public void readSimple() {
        mDb = createDatabase(
                "CREATE TABLE foo (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "name TEXT)");
        TableInfo info = TableInfo.read(mDb, "foo");
        assertThat(info, is(new TableInfo("foo",
                toMap(new TableInfo.Column("id", "INTEGER", 1),
                        new TableInfo.Column("name", "TEXT", 0)))));
    }

    @Test
    public void multiplePrimaryKeys() {
        mDb = createDatabase(
                "CREATE TABLE foo (id INTEGER,"
                        + "name TEXT, PRIMARY KEY(name, id))");
        TableInfo info = TableInfo.read(mDb, "foo");
        assertThat(info, is(new TableInfo("foo",
                toMap(new TableInfo.Column("id", "INTEGER", 2),
                        new TableInfo.Column("name", "TEXT", 1))
        )));
    }

    @Test
    public void alteredTable() {
        mDb = createDatabase(
                "CREATE TABLE foo (id INTEGER,"
                        + "name TEXT, PRIMARY KEY(name))");
        mDb.execSQL("ALTER TABLE foo ADD COLUMN added REAL;");
        TableInfo info = TableInfo.read(mDb, "foo");
        assertThat(info, is(new TableInfo("foo",
                toMap(new TableInfo.Column("id", "INTEGER", 0),
                        new TableInfo.Column("name", "TEXT", 1),
                        new TableInfo.Column("added", "REAL", 0))
        )));
    }

    @Test
    public void nonNull() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT NOT NULL)");
        TableInfo info = TableInfo.read(mDb, "foo");
        assertThat(info, is(new TableInfo("foo",
                toMap(new TableInfo.Column("name", "TEXT", 0))
        )));
    }

    @Test
    public void defaultValue() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT DEFAULT blah)");
        TableInfo info = TableInfo.read(mDb, "foo");
        assertThat(info, is(new TableInfo(
                "foo",
                toMap(new TableInfo.Column("name", "TEXT", 0))
        )));
    }

    private static Map<String, TableInfo.Column> toMap(TableInfo.Column... columns) {
        Map<String, TableInfo.Column> result = new HashMap<>();
        for (TableInfo.Column column : columns) {
            result.put(column.name, column);
        }
        return result;
    }

    @After
    public void closeDb() throws IOException {
        if (mDb != null && mDb.isOpen()) {
            mDb.close();
        }
    }

    private static SupportSQLiteDatabase createDatabase(final String... queries) {
        return new FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration
                        .builder(InstrumentationRegistry.getTargetContext())
                        .name(null)
                        .version(1)
                        .callback(new SupportSQLiteOpenHelper.Callback() {
                            @Override
                            public void onCreate(SupportSQLiteDatabase db) {
                                for (String query : queries) {
                                    db.execSQL(query);
                                }
                            }

                            @Override
                            public void onUpgrade(SupportSQLiteDatabase db, int oldVersion,
                                    int newVersion) {
                                throw new IllegalStateException("should not be upgrading");
                            }
                        }).build()
        ).getWritableDatabase();
    }
}
