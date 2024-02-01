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

package androidx.room.migration;

import static androidx.room.util.TableInfo.Column.defaultValueEquals;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import android.util.Pair;

import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        TableInfo expectedInfo = new TableInfo("foo",
                toMap(new TableInfo.Column("id", "INTEGER", false, 1, null,
                                TableInfo.CREATED_FROM_ENTITY),
                        new TableInfo.Column("name", "TEXT", false, 0, null,
                                TableInfo.CREATED_FROM_ENTITY)),
                Collections.<TableInfo.ForeignKey>emptySet());
        assertThat(expectedInfo, is(dbInfo));
    }

    @Test
    public void readSimple_toStringCheck() {
        mDb = createDatabase(
                "CREATE TABLE foo (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "name TEXT)");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        assertThat(
                dbInfo.toString(),
                // Leaves out indices as they are represented as null in versionn17 and empty
                // array for later versions
                containsString("TableInfo{name='foo', columns={id=Column{name='id'"
                        + ", type='INTEGER', "
                        + "affinity='3', notNull=false, primaryKeyPosition=1, "
                        + "defaultValue='undefined'}, name=Column{name='name', type='TEXT', "
                        + "affinity='2', notNull=false, primaryKeyPosition=0, "
                        + "defaultValue='undefined'}}, foreignKeys=[]")
        );
    }

    @Test
    public void multiplePrimaryKeys() {
        mDb = createDatabase(
                "CREATE TABLE foo (id INTEGER,"
                        + "name TEXT, PRIMARY KEY(name, id))");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        TableInfo expectedInfo = new TableInfo("foo",
                toMap(new TableInfo.Column("id", "INTEGER", false, 2, null,
                                TableInfo.CREATED_FROM_ENTITY),
                        new TableInfo.Column("name", "TEXT", false, 1, null,
                                TableInfo.CREATED_FROM_ENTITY)),
                Collections.<TableInfo.ForeignKey>emptySet());
        assertThat(expectedInfo, is(dbInfo));
    }

    @Test
    public void alteredTable() {
        mDb = createDatabase(
                "CREATE TABLE foo (id INTEGER,"
                        + "name TEXT, PRIMARY KEY(name))");
        mDb.execSQL("ALTER TABLE foo ADD COLUMN added REAL;");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        TableInfo expectedInfo = new TableInfo("foo",
                toMap(new TableInfo.Column("id", "INTEGER", false, 0, null,
                                TableInfo.CREATED_FROM_ENTITY),
                        new TableInfo.Column("name", "TEXT", false, 1, null,
                                TableInfo.CREATED_FROM_ENTITY),
                        new TableInfo.Column("added", "REAL", false, 0, null,
                                TableInfo.CREATED_FROM_ENTITY)),
                Collections.<TableInfo.ForeignKey>emptySet());
        assertThat(expectedInfo, is(dbInfo));
    }

    @Test
    public void nonNull() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT NOT NULL)");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        TableInfo expectedInfo = new TableInfo("foo",
                toMap(new TableInfo.Column("name", "TEXT", true, 0, null,
                        TableInfo.CREATED_FROM_ENTITY)),
                Collections.<TableInfo.ForeignKey>emptySet());
        assertThat(expectedInfo, is(dbInfo));
    }

    @Test
    public void defaultValue() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT DEFAULT blah)");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        TableInfo expectedInfo = new TableInfo(
                "foo",
                toMap(new TableInfo.Column("name", "TEXT", false, 0, "blah",
                        TableInfo.CREATED_FROM_ENTITY)),
                Collections.<TableInfo.ForeignKey>emptySet());
        assertThat(expectedInfo, is(dbInfo));
        assertThat(dbInfo, is(expectedInfo));
    }

    @Test
    public void defaultValue_missing() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT)");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        TableInfo expectedInfo = new TableInfo(
                "foo",
                toMap(new TableInfo.Column("name", "TEXT", false, 0, "blah",
                        TableInfo.CREATED_FROM_ENTITY)),
                Collections.<TableInfo.ForeignKey>emptySet());
        assertThat(expectedInfo, is(not(dbInfo)));
        assertThat(dbInfo, is(not(expectedInfo)));
    }

    @Test
    public void defaultValue_missing_should_print_undefined() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT)");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        TableInfo.Column columnInfo = dbInfo.columns.get("name");
        assertThat(columnInfo.toString()).isEqualTo(
                "Column{name='name', type='TEXT', affinity='2', notNull=false, "
                        + "primaryKeyPosition=0, defaultValue='undefined'}"
        );
    }

    @Test
    public void defaultValue_null_should_print_null() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT DEFAULT null)");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        TableInfo.Column columnInfo = dbInfo.columns.get("name");
        assertThat(columnInfo.toString()).isEqualTo(
                "Column{name='name', type='TEXT', affinity='2', notNull=false, "
                        + "primaryKeyPosition=0, defaultValue='null'}"
        );
    }

    @SuppressWarnings("deprecation")
    @Test
    public void defaultValue_oldConstructor() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT DEFAULT blah)");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        TableInfo expectedInfo = new TableInfo(
                "foo",
                toMap(new TableInfo.Column("name", "TEXT", false, 0)),
                Collections.<TableInfo.ForeignKey>emptySet());
        assertThat(expectedInfo, is(dbInfo));
        assertThat(dbInfo, is(expectedInfo));
    }

    @Test
    public void defaultValue_unaccounted() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT DEFAULT blah)");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        TableInfo expectedInfo = new TableInfo(
                "foo",
                toMap(new TableInfo.Column("name", "TEXT", false, 0, null,
                        TableInfo.CREATED_FROM_ENTITY)),
                Collections.<TableInfo.ForeignKey>emptySet());
        assertThat(expectedInfo, is(dbInfo));
        assertThat(dbInfo, is(expectedInfo));
    }

    @Test
    public void columnInfo_defaultValue_equality() {
        TableInfo.Column column1;
        TableInfo.Column column2;

        column1 = new TableInfo.Column("name", "TEXT", false, 0, null,
                TableInfo.CREATED_FROM_ENTITY);
        column2 = new TableInfo.Column("name", "TEXT", false, 0, null,
                TableInfo.CREATED_FROM_ENTITY);
        assertThat(column1, is(column2));
        assertThat(column2, is(column1));

        column1 = new TableInfo.Column("name", "TEXT", false, 0, "",
                TableInfo.CREATED_FROM_ENTITY);
        column2 = new TableInfo.Column("name", "TEXT", false, 0, "different",
                TableInfo.CREATED_FROM_ENTITY);
        assertThat(column1, is(not(column2)));
        assertThat(column2, is(not(column1)));

        column1 = new TableInfo.Column("name", "TEXT", false, 0, null,
                TableInfo.CREATED_FROM_ENTITY);
        column2 = new TableInfo.Column("name", "TEXT", false, 0, "different",
                TableInfo.CREATED_FROM_ENTITY);
        assertThat(column1, is(not(column2)));
        assertThat(column2, is(not(column1)));

        column1 = new TableInfo.Column("name", "TEXT", false, 0, null,
                TableInfo.CREATED_FROM_DATABASE);
        column2 = new TableInfo.Column("name", "TEXT", false, 0, null,
                TableInfo.CREATED_FROM_DATABASE);
        assertThat(column1, is(column2));
        assertThat(column2, is(column1));

        column1 = new TableInfo.Column("name", "TEXT", false, 0, null,
                TableInfo.CREATED_FROM_DATABASE);
        column2 = new TableInfo.Column("name", "TEXT", false, 0, "different",
                TableInfo.CREATED_FROM_DATABASE);
        assertThat(column1, is(not(column2)));
        assertThat(column2, is(not(column1)));

        column1 = new TableInfo.Column("name", "TEXT", false, 0, null,
                TableInfo.CREATED_FROM_UNKNOWN);
        column2 = new TableInfo.Column("name", "TEXT", false, 0, null,
                TableInfo.CREATED_FROM_DATABASE);
        assertThat(column1, is(column2));
        assertThat(column2, is(column1));

        column1 = new TableInfo.Column("name", "TEXT", false, 0, null,
                TableInfo.CREATED_FROM_UNKNOWN);
        column2 = new TableInfo.Column("name", "TEXT", false, 0, "different",
                TableInfo.CREATED_FROM_DATABASE);
        assertThat(column1, is(column2));
        assertThat(column2, is(column1));
    }

    @Test
    public void foreignKey() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT)",
                "CREATE TABLE bar(barName TEXT, FOREIGN KEY(barName) REFERENCES foo(name))"
        );
        TableInfo dbInfo = TableInfo.read(mDb, "bar");
        assertThat(dbInfo.foreignKeys.size(), is(1));
        final TableInfo.ForeignKey foreignKey = dbInfo.foreignKeys.iterator().next();
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
        TableInfo dbInfo = TableInfo.read(mDb, "bar");
        assertThat(dbInfo.foreignKeys.size(), is(2));
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
        assertThat(dbInfo.foreignKeys, equalTo(expected));
    }

    @Test
    public void compositeForeignKey() {
        mDb = createDatabase(
                "CREATE TABLE foo (name TEXT, lastName TEXT)",
                "CREATE TABLE bar(barName TEXT, barLastName TEXT, "
                        + " FOREIGN KEY(barName, barLastName) REFERENCES foo(name, lastName)"
                        + " ON UPDATE cascade ON DELETE RESTRICT)");
        TableInfo dbInfo = TableInfo.read(mDb, "bar");
        assertThat(dbInfo.foreignKeys.size(), is(1));
        TableInfo.ForeignKey expected = new TableInfo.ForeignKey(
                "foo", // table
                "RESTRICT", // on delete
                "CASCADE", // on update
                asList("barName", "barLastName"), // my columns
                asList("name", "lastName") // ref columns
        );
        assertThat(dbInfo.foreignKeys.iterator().next(), is(expected));
    }

    @Test
    public void caseInsensitiveTypeName() {
        mDb = createDatabase(
                "CREATE TABLE foo (n integer)");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        TableInfo expectedInfo = new TableInfo(
                "foo",
                toMap(new TableInfo.Column("n", "INTEGER", false, 0, null,
                        TableInfo.CREATED_FROM_ENTITY)),
                Collections.<TableInfo.ForeignKey>emptySet());
        assertThat(expectedInfo, is(dbInfo));
    }

    @Test
    public void readIndices() {
        mDb = createDatabase(
                "CREATE TABLE foo (n INTEGER, indexed TEXT, unique_indexed TEXT,"
                        + "a INTEGER, b INTEGER);",
                "CREATE INDEX foo_indexed ON foo(indexed);",
                "CREATE UNIQUE INDEX foo_unique_indexed ON foo(unique_indexed COLLATE NOCASE"
                        + " DESC);",
                "CREATE INDEX " + TableInfo.Index.DEFAULT_PREFIX + "foo_composite_indexed"
                        + " ON foo(a, b);"
        );
        TableInfo dbInfo = TableInfo.read(mDb, "foo");
        TableInfo expectedInfo = new TableInfo(
                "foo",
                toMap(new TableInfo.Column("n", "INTEGER", false, 0, null,
                                TableInfo.CREATED_FROM_ENTITY),
                        new TableInfo.Column("indexed", "TEXT", false, 0, null,
                                TableInfo.CREATED_FROM_ENTITY),
                        new TableInfo.Column("unique_indexed", "TEXT", false, 0, null,
                                TableInfo.CREATED_FROM_ENTITY),
                        new TableInfo.Column("a", "INTEGER", false, 0, null,
                                TableInfo.CREATED_FROM_ENTITY),
                        new TableInfo.Column("b", "INTEGER", false, 0, null,
                                TableInfo.CREATED_FROM_ENTITY)),
                Collections.<TableInfo.ForeignKey>emptySet(),
                toSet(new TableInfo.Index("index_foo_blahblah", false,
                        Arrays.asList("a", "b"), Collections.emptyList()),
                        new TableInfo.Index("foo_unique_indexed", true,
                                Arrays.asList("unique_indexed"), Arrays.asList("DESC")),
                        new TableInfo.Index("foo_indexed", false,
                                Arrays.asList("indexed"), Arrays.asList("ASC"))));
        assertThat(expectedInfo, is(dbInfo));
    }

    @Test
    public void compatColumnTypes() {
        // see:https://www.sqlite.org/datatype3.html 3.1
        List<Pair<String, String>> testCases = Arrays.asList(
                new Pair<>("TINYINT", "integer"),
                new Pair<>("VARCHAR", "text"),
                new Pair<>("DOUBLE", "real"),
                new Pair<>("BOOLEAN", "numeric"),
                new Pair<>("FLOATING POINT", "integer")
        );
        for (Pair<String, String> testCase : testCases) {
            mDb = createDatabase(
                    "CREATE TABLE foo (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "name " + testCase.first + ")");
            TableInfo info = TableInfo.read(mDb, "foo");
            assertThat(info, is(new TableInfo("foo",
                    toMap(new TableInfo.Column("id", "INTEGER", false, 1, null,
                                    TableInfo.CREATED_FROM_ENTITY),
                            new TableInfo.Column("name", testCase.second, false, 0, null,
                                    TableInfo.CREATED_FROM_ENTITY)),
                    Collections.<TableInfo.ForeignKey>emptySet())));
        }
    }

    private static Map<String, TableInfo.Column> toMap(TableInfo.Column... columns) {
        Map<String, TableInfo.Column> result = new HashMap<>();
        for (TableInfo.Column column : columns) {
            result.put(column.name, column);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> Set<T> toSet(T... ts) {
        final HashSet<T> result = new HashSet<T>();
        for (T t : ts) {
            result.add(t);
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
                        .builder(ApplicationProvider.getApplicationContext())
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

    @Test
    public void testSurroundingParenthesis() {
        mDb = createDatabase(
                "CREATE TABLE foo (name INTEGER NOT NULL DEFAULT ((0) + (1 + 2)))");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");

        assertThat("((0) + (1 + 2))")
                .isNotEqualTo(Objects.requireNonNull(dbInfo.columns.get("name")).defaultValue);

        assertThat(defaultValueEquals(
                "((0) + (1 + 2))",
                Objects.requireNonNull(dbInfo.columns.get("name")).defaultValue)).isTrue();
    }

    @Test
    public void testDoubleSurroundingParenthesis() {
        mDb = createDatabase(
                "CREATE TABLE foo (name INTEGER NOT NULL DEFAULT (((0) + (1 + 2))))");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");

        assertThat("(((0) + (1 + 2)))")
                .isNotEqualTo(Objects.requireNonNull(dbInfo.columns.get("name")).defaultValue);

        assertThat(defaultValueEquals(
                "(((0) + (1 + 2)))",
                Objects.requireNonNull(dbInfo.columns.get("name")).defaultValue));
    }

    @Test
    public void testMultipleParenthesisWithSurrounding() {
        mDb = createDatabase(
                "CREATE TABLE foo (name INTEGER NOT NULL DEFAULT (((3 + 5) + (2 + 1)) + (1 + 2)))");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");

        assertThat("(((3 + 5) + (2 + 1)) + (1 + 2))")
                .isNotEqualTo(Objects.requireNonNull(dbInfo.columns.get("name")).defaultValue);

        assertThat(defaultValueEquals(
                "(((3 + 5) + (2 + 1)) + (1 + 2))",
                Objects.requireNonNull(dbInfo.columns.get("name")).defaultValue));
    }

    @Test
    public void testSurroundingParenthesisWithSpacesBefore() {
        mDb = createDatabase(
                "CREATE TABLE foo (name INTEGER NOT NULL DEFAULT (    (0) + (1 + 2)))");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");

        assertThat("(    (0) + (1 + 2))")
                .isNotEqualTo(Objects.requireNonNull(dbInfo.columns.get("name")).defaultValue);

        assertThat(defaultValueEquals(
                "(    (0) + (1 + 2))",
                Objects.requireNonNull(dbInfo.columns.get("name")).defaultValue));
    }

    @Test
    public void testSurroundingParenthesisWithSpacesAfter() {
        mDb = createDatabase(
                "CREATE TABLE foo (name INTEGER NOT NULL DEFAULT ((0) + (1 + 2)    ))");
        TableInfo dbInfo = TableInfo.read(mDb, "foo");

        assertThat("((0) + (1 + 2)    )")
                .isNotEqualTo(Objects.requireNonNull(dbInfo.columns.get("name")).defaultValue);

        assertThat(defaultValueEquals(
                "((0) + (1 + 2)    )",
                Objects.requireNonNull(dbInfo.columns.get("name")).defaultValue)).isTrue();
    }
}
