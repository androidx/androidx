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

package android.arch.persistence.room.migration;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory;
import android.arch.persistence.room.util.TableInfo;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
                toMap(new TableInfo.Column("id", "INTEGER", false, 1),
                        new TableInfo.Column("name", "TEXT", false, 0)),
                Collections.<TableInfo.ForeignKey>emptySet())));
    }

    @Test
    public void multiplePrimaryKeys() {
        mDb = createDatabase(
                "CREATE TABLE foo (id INTEGER,"
                        + "name TEXT, PRIMARY KEY(name, id))");
        TableInfo info = TableInfo.read(mDb, "foo");
        assertThat(info, is(new TableInfo("foo",
                toMap(new TableInfo.Column("id", "INTEGER", false, 2),
                        new TableInfo.Column("name", "TEXT", false, 1)),
                Collections.<TableInfo.ForeignKey>emptySet())));
    }

    @Test
    public void alteredTable() {
        mDb = createDatabase(
                "CREATE TABLE foo (id INTEGER,"
                        + "name TEXT, PRIMARY KEY(name))");
        mDb.execSQL("ALTER TABLE foo ADD COLUMN added REAL;");
        TableInfo info = TableInfo.read(mDb, "foo");
        assertThat(info, is(new TableInfo("foo",
                toMap(new TableInfo.Column("id", "INTEGER", false, 0),
                        new TableInfo.Column("name", "TEXT", false, 1),
                        new TableInfo.Column("added", "REAL", false, 0)),
                Collections.<TableInfo.ForeignKey>emptySet())));
    }

    @Test
    public void nonNull() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT NOT NULL)");
        TableInfo info = TableInfo.read(mDb, "foo");
        assertThat(info, is(new TableInfo("foo",
                toMap(new TableInfo.Column("name", "TEXT", true, 0)),
                Collections.<TableInfo.ForeignKey>emptySet())));
    }

    @Test
    public void defaultValue() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT DEFAULT blah)");
        TableInfo info = TableInfo.read(mDb, "foo");
        assertThat(info, is(new TableInfo(
                "foo",
                toMap(new TableInfo.Column("name", "TEXT", false, 0)),
                Collections.<TableInfo.ForeignKey>emptySet())));
    }

    @Test
    public void foreignKey() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT)",
                "CREATE TABLE bar(barName TEXT, FOREIGN KEY(barName) REFERENCES foo(name))"
        );
        TableInfo info = TableInfo.read(mDb, "bar");
        assertThat(info.foreignKeys.size(), is(1));
        final TableInfo.ForeignKey foreignKey = info.foreignKeys.iterator().next();
        assertThat(foreignKey.columnNames, is(singletonList("barName")));
        assertThat(foreignKey.referenceColumnNames, is(singletonList("name")));
        assertThat(foreignKey.onDelete, is("NO ACTION"));
        assertThat(foreignKey.onUpdate, is("NO ACTION"));
        assertThat(foreignKey.referenceTable, is("foo"));
    }

    @Test
    public void multipleForeignKeys() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT, lastName TEXT)",
                "CREATE TABLE foo2 (name TEXT, lastName TEXT)",
                "CREATE TABLE bar(barName TEXT, barLastName TEXT, "
                        + " FOREIGN KEY(barName) REFERENCES foo(name) ON UPDATE SET NULL,"
                        + " FOREIGN KEY(barLastName) REFERENCES foo2(lastName) ON DELETE CASCADE)");
        TableInfo info = TableInfo.read(mDb, "bar");
        assertThat(info.foreignKeys.size(), is(2));
        Set<TableInfo.ForeignKey> expected = new HashSet<>();
        expected.add(new TableInfo.ForeignKey("foo2", // table
                "CASCADE", // on delete
                "NO ACTION", // on update
                singletonList("barLastName"), // my
                singletonList("lastName")) // ref
        );
        expected.add(new TableInfo.ForeignKey("foo", // table
                "NO ACTION", // on delete
                "SET NULL", // on update
                singletonList("barName"), // mine
                singletonList("name")/*ref*/));
        assertThat(info.foreignKeys, equalTo(expected));
    }

    @Test
    public void compositeForeignKey() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT, lastName TEXT)",
                "CREATE TABLE bar(barName TEXT, barLastName TEXT, "
                        + " FOREIGN KEY(barName, barLastName) REFERENCES foo(name, lastName)"
                        + " ON UPDATE cascade ON DELETE RESTRICT)");
        TableInfo info = TableInfo.read(mDb, "bar");
        assertThat(info.foreignKeys.size(), is(1));
        TableInfo.ForeignKey expected = new TableInfo.ForeignKey(
                "foo", // table
                "RESTRICT", // on delete
                "CASCADE", // on update
                asList("barName", "barLastName"), // my columns
                asList("name", "lastName") // ref columns
        );
        assertThat(info.foreignKeys.iterator().next(), is(expected));
    }

    @Test
    public void caseInsensitiveTypeName() {
        mDb = createDatabase(
                "CREATE TABLE foo (n integer)");
        TableInfo info = TableInfo.read(mDb, "foo");
        assertThat(info, is(new TableInfo(
                "foo",
                toMap(new TableInfo.Column("n", "INTEGER", false, 0)),
                Collections.<TableInfo.ForeignKey>emptySet())));
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
                        .callback(new SupportSQLiteOpenHelper.Callback(1) {
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
