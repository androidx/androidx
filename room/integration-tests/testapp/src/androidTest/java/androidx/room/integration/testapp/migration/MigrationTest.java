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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.room.migration.bundle.SchemaBundle;
import androidx.room.testing.MigrationTestHelper;
import androidx.room.util.TableInfo;
import androidx.room.util.ViewInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.truth.Truth;

import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test custom database migrations.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MigrationTest {
    private static final String TEST_DB = "migration-test";
    @Rule
    public MigrationTestHelper helper;

    public MigrationTest() {
        helper = new MigrationTestHelper(
                InstrumentationRegistry.getInstrumentation(),
                MigrationDb.class.getCanonicalName()
        );
    }

    @Test
    @SuppressWarnings("deprecation") // This test is for verifying the old deprecated constructor
    public void giveBadResource() throws IOException {
        MigrationTestHelper helper = new MigrationTestHelper(
                InstrumentationRegistry.getInstrumentation(),
                "foo", new FrameworkSQLiteOpenHelperFactory());
        try {
            helper.createDatabase(TEST_DB, 1);
            throw new AssertionError("must have failed with missing file exception");
        } catch (FileNotFoundException exception) {
            assertThat(exception.getMessage(), containsString("Cannot find"));
        }
    }

    @Test
    public void startInCurrentVersion() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB,
                MigrationDb.LATEST_VERSION);
        final MigrationDb.Dao_V1 dao = new MigrationDb.Dao_V1(db);
        dao.insertIntoEntity1(2, "x");
        db.close();
        MigrationDb migrationDb = getLatestDb();
        List<MigrationDb.Entity1> items = migrationDb.dao().loadAllEntity1s();
        helper.closeWhenFinished(migrationDb);
        assertThat(items.size(), is(1));
    }

    @Test
    public void addTable() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);
        final MigrationDb.Dao_V1 dao = new MigrationDb.Dao_V1(db);
        dao.insertIntoEntity1(2, "foo");
        dao.insertIntoEntity1(3, "bar");
        db.close();
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2);
        new MigrationDb.Dao_V2(db).insertIntoEntity2(3, "blah");
        db.close();
        MigrationDb migrationDb = getLatestDb();
        List<MigrationDb.Entity1> entity1s = migrationDb.dao().loadAllEntity1s();

        assertThat(entity1s.size(), is(2));
        MigrationDb.Entity2 entity2 = new MigrationDb.Entity2();
        entity2.id = 2;
        entity2.name = "bar";
        // assert no error happens
        migrationDb.dao().insert(entity2);
        List<MigrationDb.Entity2> entity2s = migrationDb.dao().loadAllEntity2s();
        assertThat(entity2s.size(), is(2));
    }

    private MigrationDb getLatestDb() {
        MigrationDb db = Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                MigrationDb.class, TEST_DB).addMigrations(ALL_MIGRATIONS).build();
        db.getOpenHelper().getWritableDatabase(); // trigger open
        helper.closeWhenFinished(db);
        return db;
    }

    @Test
    public void addTableFailure() throws IOException {
        String errorMsg = """
                Migration didn't properly handle:  Entity2

                Expected:

                TableInfo {
                    name = 'Entity2',
                    columns = {   \s
                        Column {
                           name = 'id',
                           type = 'INTEGER',
                           affinity = '3',
                           notNull = 'true',
                           primaryKeyPosition = '1',
                           defaultValue = 'undefined'
                        },
                        Column {
                           name = 'name',
                           type = 'TEXT',
                           affinity = '2',
                           notNull = 'false',
                           primaryKeyPosition = '0',
                           defaultValue = 'undefined'
                        }
                    },
                    foreignKeys = { }
                    indices = { }
                }

                Found:

                TableInfo {
                    name = 'Entity2',
                    columns = { }
                    foreignKeys = { }
                    indices = { }
                }""";
        testFailure(1, 2, errorMsg);
    }

    @Test
    public void addColumnFailure() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);
        db.close();
        IllegalStateException caught = null;
        try {
            helper.runMigrationsAndValidate(TEST_DB, 3, true, new EmptyMigration(2, 3));
        } catch (IllegalStateException ex) {
            caught = ex;
        }
        assertThat(caught, instanceOf(IllegalStateException.class));
    }

    @Test
    public void addColumn() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);
        MigrationDb.Dao_V2 v2Dao = new MigrationDb.Dao_V2(db);
        v2Dao.insertIntoEntity2(7, "blah");
        db.close();
        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3);
        // trigger open.
        MigrationDb migrationDb = getLatestDb();
        List<MigrationDb.Entity2> entity2s = migrationDb.dao().loadAllEntity2s();
        assertThat(entity2s.size(), is(1));
        assertThat(entity2s.get(0).name, is("blah"));
        assertThat(entity2s.get(0).addedInV3, is(nullValue()));

        List<MigrationDb.Entity2Pojo> entity2Pojos = migrationDb.dao().loadAllEntity2sAsPojo();
        assertThat(entity2Pojos.size(), is(1));
        assertThat(entity2Pojos.get(0).name, is("blah"));
        assertThat(entity2Pojos.get(0).addedInV3, is(nullValue()));
    }

    @Test
    public void failedToRemoveColumn() throws IOException {
        String errorMsg = """
                Migration didn't properly handle:  Entity3

                Expected:

                TableInfo {
                    name = 'Entity3',
                    columns = {   \s
                        Column {
                           name = 'id',
                           type = 'INTEGER',
                           affinity = '3',
                           notNull = 'true',
                           primaryKeyPosition = '1',
                           defaultValue = 'undefined'
                        },
                        Column {
                           name = 'name',
                           type = 'TEXT',
                           affinity = '2',
                           notNull = 'false',
                           primaryKeyPosition = '0',
                           defaultValue = 'undefined'
                        }
                    },
                    foreignKeys = { }
                    indices = { }
                }

                Found:

                TableInfo {
                    name = 'Entity3',
                    columns = {   \s
                        Column {
                           name = 'id',
                           type = 'INTEGER',
                           affinity = '3',
                           notNull = 'true',
                           primaryKeyPosition = '1',
                           defaultValue = 'undefined'
                        },
                        Column {
                           name = 'name',
                           type = 'TEXT',
                           affinity = '2',
                           notNull = 'false',
                           primaryKeyPosition = '0',
                           defaultValue = 'undefined'
                        },
                        Column {
                           name = 'removedInV5',
                           type = 'TEXT',
                           affinity = '2',
                           notNull = 'false',
                           primaryKeyPosition = '0',
                           defaultValue = 'undefined'
                        }
                    },
                    foreignKeys = { }
                    indices = { }
                }""";
        testFailure(4, 5, errorMsg);
    }

    @Test
    public void removeColumn() throws IOException {
        helper.createDatabase(TEST_DB, 4);
        final SupportSQLiteDatabase db = helper.runMigrationsAndValidate(TEST_DB,
                5, true, MIGRATION_4_5);
        final TableInfo info = TableInfo.read(db, MigrationDb.Entity3.TABLE_NAME);
        assertThat(info.columns.size(), is(2));
    }

    @Test
    public void dropTable() throws IOException {
        helper.createDatabase(TEST_DB, 5);
        final SupportSQLiteDatabase db = helper.runMigrationsAndValidate(TEST_DB,
                6, true, MIGRATION_5_6);
        final TableInfo info = TableInfo.read(db, MigrationDb.Entity3.TABLE_NAME);
        assertThat(info.columns.size(), is(0));
    }

    @Test
    public void failedToDropTable() throws IOException {
        String errorMsg = "Migration didn't properly handle: Unexpected table Entity3";
        testFailure(5, 6, errorMsg);
    }

    @Test
    public void failedToDropTableDontVerify() throws IOException {
        helper.createDatabase(TEST_DB, 5);
        final SupportSQLiteDatabase db = helper.runMigrationsAndValidate(TEST_DB,
                6, false, new EmptyMigration(5, 6));
        final TableInfo info = TableInfo.read(db, MigrationDb.Entity3.TABLE_NAME);
        assertThat(info.columns.size(), is(2));
    }

    @Test
    public void failedForeignKey() throws IOException {
        final SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 6);
        db.close();
        Throwable throwable = null;
        try {
            helper.runMigrationsAndValidate(TEST_DB,
                    7, false, new Migration(6, 7) {
                        @Override
                        public void migrate(@NonNull SupportSQLiteDatabase db) {
                            db.execSQL("CREATE TABLE Entity4 (`id` INTEGER NOT NULL,"
                                    + " `name` TEXT, PRIMARY KEY(`id`))");
                        }
                    });
        } catch (Throwable t) {
            throwable = t;
        }
        assertThat(throwable, instanceOf(IllegalStateException.class));
        //noinspection ConstantConditions
        assertThat(throwable.getMessage(), containsString("Migration didn't properly handle"));
    }

    @Test
    public void newTableWithForeignKey() throws IOException {
        helper.createDatabase(TEST_DB, 6);
        final SupportSQLiteDatabase db = helper.runMigrationsAndValidate(TEST_DB,
                7, false, MIGRATION_6_7);
        final TableInfo info = TableInfo.read(db, MigrationDb.Entity4.TABLE_NAME);
        assertThat(info.foreignKeys.size(), is(1));
    }

    @Test
    public void addView() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 7);
        assertThat(ViewInfo.read(db, MigrationDb.View1.VIEW_NAME).sql, is(nullValue()));
        db.close();
        db = helper.runMigrationsAndValidate(TEST_DB,
                8, false, MIGRATION_7_8);
        final ViewInfo info = ViewInfo.read(db, MigrationDb.View1.VIEW_NAME);
        assertThat(info.name, is(equalTo(MigrationDb.View1.VIEW_NAME)));
        assertThat(info.sql, is(equalTo("CREATE VIEW `" + MigrationDb.View1.VIEW_NAME + "` AS "
                + "SELECT Entity4.id, Entity4.name, Entity1.id AS entity1Id "
                + "FROM Entity4 INNER JOIN Entity1 ON Entity4.name = Entity1.name")));
    }

    @Test
    public void addViewFailure() throws IOException {
        String sql = "CREATE VIEW `View1` AS SELECT Entity4.id, Entity4.name, "
                     + "Entity1.id AS entity1Id FROM Entity4 INNER JOIN Entity1 "
                     + "ON Entity4.name = Entity1.name";
        String errorMsg = ("""
                Migration didn't properly handle:  View1

                Expected: ViewInfo {
                   name = 'View1',
                   sql = '$sql'
                }

                Found: ViewInfo {
                   name = 'View1',
                   sql = 'null'
                }""").replace("$sql", sql);
        testFailure(7, 8, errorMsg);
    }

    @Test
    public void addDefaultValue_unaccounted() throws IOException {
        // Verify a migration test pre room 2.2.0 that adds a default value does not suddenly fails.
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 8);
        db.execSQL("INSERT INTO Entity2 (id, addedInV3, name) VALUES (1, '', '')");
        db.close();
        db = helper.runMigrationsAndValidate(TEST_DB,
                9, false, MIGRATION_8_9);
        Cursor c = db.query("SELECT id, addedInV9 FROM Entity2");
        //noinspection TryFinallyCanBeTryWithResources
        try {
            assertThat(c.moveToNext(), is(true));
            assertThat(c.getInt(0), is(1));
            assertThat(c.getString(1), is(""));
        } finally {
            c.close();
        }
    }

    @Test
    public void addDefaultValue() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 10);
        final TableInfo oldTable = TableInfo.read(db, MigrationDb.Entity2.TABLE_NAME);
        final TableInfo.Column oldColumn = oldTable.columns.get("name");
        assertThat(oldColumn, is(notNullValue()));
        assertThat(oldColumn.defaultValue, is(nullValue()));
        db.close();
        db = helper.runMigrationsAndValidate(TEST_DB, 11, false, MIGRATION_10_11);
        final TableInfo table = TableInfo.read(db, MigrationDb.Entity2.TABLE_NAME);
        final TableInfo.Column column = table.columns.get("name");
        assertThat(column, is(notNullValue()));
        assertThat(column.defaultValue, is(equalTo("'Unknown'")));
    }

    @Test
    public void addDefaultValueFailure() throws IOException {
        String errorMsg = """
                Migration didn't properly handle:  Entity2

                Expected:

                TableInfo {
                    name = 'Entity2',
                    columns = {   \s
                        Column {
                           name = 'addedInV3',
                           type = 'TEXT',
                           affinity = '2',
                           notNull = 'false',
                           primaryKeyPosition = '0',
                           defaultValue = 'undefined'
                        },
                        Column {
                           name = 'addedInV9',
                           type = 'TEXT',
                           affinity = '2',
                           notNull = 'false',
                           primaryKeyPosition = '0',
                           defaultValue = 'undefined'
                        },
                        Column {
                           name = 'id',
                           type = 'INTEGER',
                           affinity = '3',
                           notNull = 'true',
                           primaryKeyPosition = '1',
                           defaultValue = 'undefined'
                        },
                        Column {
                           name = 'name',
                           type = 'TEXT',
                           affinity = '2',
                           notNull = 'false',
                           primaryKeyPosition = '0',
                           defaultValue = ''Unknown''
                        }
                    },
                    foreignKeys = { }
                    indices = { }
                }

                Found:

                TableInfo {
                    name = 'Entity2',
                    columns = {   \s
                        Column {
                           name = 'addedInV3',
                           type = 'TEXT',
                           affinity = '2',
                           notNull = 'false',
                           primaryKeyPosition = '0',
                           defaultValue = 'undefined'
                        },
                        Column {
                           name = 'addedInV9',
                           type = 'TEXT',
                           affinity = '2',
                           notNull = 'false',
                           primaryKeyPosition = '0',
                           defaultValue = 'undefined'
                        },
                        Column {
                           name = 'id',
                           type = 'INTEGER',
                           affinity = '3',
                           notNull = 'true',
                           primaryKeyPosition = '1',
                           defaultValue = 'undefined'
                        },
                        Column {
                           name = 'name',
                           type = 'TEXT',
                           affinity = '2',
                           notNull = 'false',
                           primaryKeyPosition = '0',
                           defaultValue = 'undefined'
                        }
                    },
                    foreignKeys = { }
                    indices = { }
                }""";
        testFailure(10, 11, errorMsg);
    }

    @Test
    public void validateDefaultValueWithSurroundingParenthesis() throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 12);
        database.close();
        Context targetContext = ApplicationProvider.getApplicationContext();
        MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                .addMigrations(MIGRATION_12_13)
                .build();

        assertThat(db.dao().loadAllEntity1s(), is(notNullValue()));
        helper.closeWhenFinished(db);

        // Confirm if this is also works with the Migration Test Helper.
        Throwable throwable = null;
        try {
            helper.runMigrationsAndValidate(TEST_DB, 13, true,
                    MIGRATION_12_13);
        } catch (Throwable t) {
            throwable = t;
        }
        assertThat(throwable, is(nullValue()));
    }

    @Test
    public void missingMigration_onUpgrade() throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 1);
        database.close();
        try {
            Context targetContext = ApplicationProvider.getApplicationContext();
            MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                    .build();
            helper.closeWhenFinished(db);
            db.dao().loadAllEntity1s();
            throw new AssertionError("Should've failed :/");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void missingMigration_onDowngrade() throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, MigrationDb.MAX_VERSION);
        database.close();
        try {
            Context targetContext = ApplicationProvider.getApplicationContext();
            MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                    .build();
            helper.closeWhenFinished(db);
            db.dao().loadAllEntity1s();
            throw new AssertionError("Should've failed :/");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void missingMigration_onUpgrade_fallbackToDestructiveMigrationOnDowngrade()
            throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 1);
        database.close();
        try {
            Context targetContext = ApplicationProvider.getApplicationContext();
            MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                    .fallbackToDestructiveMigrationOnDowngrade(false)
                    .build();
            helper.closeWhenFinished(db);
            db.dao().loadAllEntity1s();
            throw new AssertionError("Should've failed :/");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void missingMigrationNuke() throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 1);
        final MigrationDb.Dao_V1 dao = new MigrationDb.Dao_V1(database);
        dao.insertIntoEntity1(2, "foo");
        dao.insertIntoEntity1(3, "bar");
        database.close();

        Context targetContext = ApplicationProvider.getApplicationContext();
        MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                .fallbackToDestructiveMigration(false)
                .build();
        assertThat(db.dao().loadAllEntity1s().size(), is(0));
        db.close();
    }

    @Test
    public void failWithIdentityCheck() throws IOException {
        for (int i = 1; i < MigrationDb.LATEST_VERSION; i++) {
            String name = "test_" + i;
            helper.createDatabase(name, i).close();
            IllegalStateException exception = null;
            try {
                MigrationDb db = Room.databaseBuilder(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        MigrationDb.class, name).build();
                helper.closeWhenFinished(db);
                db.runInTransaction(new Runnable() {
                    @Override
                    public void run() {
                        // do nothing
                    }
                });
            } catch (IllegalStateException ex) {
                exception = ex;
            }
            MatcherAssert.assertThat("identity detection should've failed",
                    exception, notNullValue());
        }
    }

    @Test
    public void fallbackToDestructiveMigrationFrom_destructiveMigrationOccursForSuppliedVersion()
            throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 6);
        final MigrationDb.Dao_V1 dao = new MigrationDb.Dao_V1(database);
        dao.insertIntoEntity1(2, "foo");
        dao.insertIntoEntity1(3, "bar");
        database.close();
        Context targetContext = ApplicationProvider.getApplicationContext();

        MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                .fallbackToDestructiveMigrationFrom(6)
                .build();
        helper.closeWhenFinished(db);
        assertThat(db.dao().loadAllEntity1s().size(), is(0));
    }

    @Test
    public void fallbackToDestructiveMigrationFrom_suppliedValueIsMigrationStartVersion_exception()
            throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 6);
        database.close();
        Context targetContext = ApplicationProvider.getApplicationContext();

        Throwable throwable = null;
        try {
            Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigrationFrom(6)
                    .build();
        } catch (Throwable t) {
            throwable = t;
        }

        assertThat(throwable, is(not(nullValue())));
        //noinspection ConstantConditions
        assertThat(throwable.getMessage(),
                startsWith("Inconsistency detected. A Migration was supplied to"));
        assertThat(throwable.getMessage(), endsWith("6"));
    }

    @Test
    public void fallbackToDestructiveMigrationFrom_suppliedValueIsMigrationEndVersion_exception()
            throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 5);
        database.close();
        Context targetContext = ApplicationProvider.getApplicationContext();

        Throwable throwable = null;
        try {
            Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigrationFrom(6)
                    .build();
        } catch (Throwable t) {
            throwable = t;
        }

        assertThat(throwable, is(not(nullValue())));
        //noinspection ConstantConditions
        assertThat(throwable.getMessage(),
                startsWith("Inconsistency detected. A Migration was supplied to"));
        assertThat(throwable.getMessage(), endsWith("6"));
    }

    @Test
    public void fallbackToDestructiveMigrationOnDowngrade_onDowngrade()
            throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, MigrationDb.MAX_VERSION);
        final MigrationDb.Dao_V1 dao = new MigrationDb.Dao_V1(database);
        dao.insertIntoEntity1(2, "foo");
        dao.insertIntoEntity1(3, "bar");
        database.close();

        Context targetContext = ApplicationProvider.getApplicationContext();
        MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                .fallbackToDestructiveMigrationOnDowngrade(false)
                .build();
        assertThat(db.dao().loadAllEntity1s().size(), is(0));
        db.close();
    }

    @Test
    public void fallbackToDestructiveMigration_onDowngrade()
            throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, MigrationDb.MAX_VERSION);
        final MigrationDb.Dao_V1 dao = new MigrationDb.Dao_V1(database);
        dao.insertIntoEntity1(2, "foo");
        dao.insertIntoEntity1(3, "bar");
        database.close();

        Context targetContext = ApplicationProvider.getApplicationContext();
        MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                .fallbackToDestructiveMigration(false)
                .build();
        assertThat(db.dao().loadAllEntity1s().size(), is(0));
        db.close();
    }

    @Test
    public void fallbackToDestructiveMigrationOnDowngrade_suppliedMigration()
            throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, MigrationDb.MAX_VERSION);
        final MigrationDb.Dao_V1 dao = new MigrationDb.Dao_V1(database);
        dao.insertIntoEntity1(2, "foo");
        dao.insertIntoEntity1(3, "bar");
        database.close();

        Context targetContext = ApplicationProvider.getApplicationContext();
        MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                .fallbackToDestructiveMigrationOnDowngrade(false)
                .addMigrations(MIGRATION_MAX_LATEST)
                .build();
        // Check that two values are present, confirming the database migration was successful
        long rowsCount = db.getOpenHelper().getReadableDatabase().compileStatement(
                "SELECT count(*) FROM NoOp").simpleQueryForLong();
        assertThat(rowsCount, is(2L));
        db.close();
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    public void missingAddedIndex() throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 11);
        database.close();
        Context targetContext = ApplicationProvider.getApplicationContext();
        MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                .addMigrations(new EmptyMigration(11, MIGRATION_MAX_LATEST.endVersion))
                .build();
        try {
            db.dao().loadAllEntity1s();
            Assert.fail("expected a missing migration exception");
        } catch (IllegalStateException ex) {
            Truth.assertThat(ex).hasMessageThat().contains(
                    "Migration didn't properly handle"
            );
        } finally {
            db.close();
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    public void missingAddedIndex_viaMigrationTesting() throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 11);
        database.close();
        try {
            helper.runMigrationsAndValidate(TEST_DB, 12, false, new EmptyMigration(11, 12));
            Assert.fail("expected a missing migration exception");
        } catch (IllegalStateException ex) {
            Truth.assertThat(ex).hasMessageThat().contains(
                    "Migration didn't properly handle"
            );
        }
    }

    // Verifies that even with allowDataLossOnRecovery, bad migrations are propagated and the DB
    // is not silently deleted.
    @Test
    public void badMigration_allowDataLossOnRecovery() throws IOException {
        // Create DB at version 1
        helper.createDatabase(TEST_DB, 1).close();

        // Create DB at latest version, but no migrations and with allowDataLossOnRecovery, it
        // should fail to open.
        Context targetContext = ApplicationProvider.getApplicationContext();
        MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                .openHelperFactory(configuration -> {
                    SupportSQLiteOpenHelper.Configuration config =
                            SupportSQLiteOpenHelper.Configuration.builder(targetContext)
                                    .name(configuration.name)
                                    .callback(configuration.callback)
                                    .allowDataLossOnRecovery(true)
                                    .build();
                    return new FrameworkSQLiteOpenHelperFactory().create(config);
                })
                .build();
        try {
            db.getOpenHelper().getWritableDatabase();
            Assert.fail("Expected a missing migration exception");
        } catch (IllegalStateException ex) {
            // Verifies exception is not wrapped
            Truth.assertThat(ex).hasMessageThat()
                    .containsMatch("A migration from \\d+ to \\d+ was required but not found.");
        }
    }

    @Test
    public void invalid_hash() throws IOException {
        // Create DB at version 1
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);
        // Set its version to 2
        db.setVersion(2);
        db.close();

        // Open the database again at version 2 and it should fail identity, replicating the
        // scenario of 1. bumping version code without schema changes, 2. making schema changes and
        // 3. opening the DB again.
        try {
            helper.runMigrationsAndValidate(TEST_DB, 2, false);
            Assert.fail("Expected a room cannot verify the data integrity exception");
        } catch (IllegalStateException ex) {
            Truth.assertThat(ex).hasMessageThat().contains(
                    "Room cannot verify the data integrity"
            );
            Truth.assertThat(ex).hasMessageThat().contains(
                    "Expected identity hash: aee9a6eed720c059df0f2ee0d6e96d89, "
                            + "found: 2f3557e56d7f665363f3e20d14787a59"
            );
        }
    }

    @Test
    public void dropAllTablesDuringDestructiveMigrations() throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, MigrationDb.MAX_VERSION);
        database.close();

        final boolean[] onDestructiveMigrationInvoked = { false };
        Context targetContext = ApplicationProvider.getApplicationContext();
        MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                .fallbackToDestructiveMigration(true)
                .addCallback(new RoomDatabase.Callback() {
                    @Override
                    public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                        super.onDestructiveMigration(db);
                        onDestructiveMigrationInvoked[0] = true;
                    }
                })
                .build();
        Set<String> tableNames = new HashSet<>();
        Cursor c = db.query("SELECT name FROM sqlite_master WHERE type = 'table'", new Object[0]);
        while (c.moveToNext()) {
            tableNames.add(c.getString(0));
        }
        c.close();
        db.close();
        // Extra table is no longer present
        assertThat(tableNames.contains("Extra"), is(false));
        // Android special table is present
        assertThat(tableNames.contains("android_metadata"), is(true));

        assertThat(onDestructiveMigrationInvoked[0], is(true));
    }

    private void testFailure(int startVersion, int endVersion, String errorMsg) throws IOException {
        final SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, startVersion);
        db.close();
        Throwable throwable = null;
        try {
            helper.runMigrationsAndValidate(TEST_DB, endVersion, true,
                    new EmptyMigration(startVersion, endVersion));
        } catch (Throwable t) {
            throwable = t;
        }
        assertThat(throwable, instanceOf(IllegalStateException.class));
        //noinspection ConstantConditions
        assertThat(throwable.getMessage(), is(errorMsg));
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `Entity2` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + " `name` TEXT)");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE " + MigrationDb.Entity2.TABLE_NAME
                    + " ADD COLUMN addedInV3 TEXT");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `Entity3` (`id` INTEGER NOT NULL,"
                    + " `removedInV5` TEXT, `name` TEXT, PRIMARY KEY(`id`))");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `Entity3_New` (`id` INTEGER NOT NULL,"
                    + " `name` TEXT, PRIMARY KEY(`id`))");
            db.execSQL("INSERT INTO Entity3_New(`id`, `name`) "
                    + "SELECT `id`, `name` FROM Entity3");
            db.execSQL("DROP TABLE Entity3");
            db.execSQL("ALTER TABLE Entity3_New RENAME TO Entity3");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("DROP TABLE " + MigrationDb.Entity3.TABLE_NAME);
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + MigrationDb.Entity4.TABLE_NAME
                    + " (`id` INTEGER NOT NULL, `name` TEXT COLLATE NOCASE, PRIMARY KEY(`id`),"
                    + " FOREIGN KEY(`name`) REFERENCES `Entity1`(`name`)"
                    + " ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED)");
            db.execSQL("CREATE UNIQUE INDEX `index_entity1` ON "
                    + MigrationDb.Entity1.TABLE_NAME + " (`name`)");
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE VIEW IF NOT EXISTS `" + MigrationDb.View1.VIEW_NAME
                    + "` AS SELECT Entity4.id, Entity4.name, Entity1.id AS entity1Id"
                    + " FROM Entity4 INNER JOIN Entity1 ON Entity4.name = Entity1.name");
        }
    };

    private static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Add a column along with a DEFAULT unknown to Room
            db.execSQL("ALTER TABLE Entity2 ADD COLUMN `addedInV9` TEXT DEFAULT ''");
        }
    };

    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Entity1 "
                    + "ADD COLUMN addedInV10 INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Add DEFAULT constraint to Entity2.name.
            db.execSQL("ALTER TABLE Entity2 RENAME TO save_Entity2");
            db.execSQL("CREATE TABLE IF NOT EXISTS Entity2 "
                    + "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`addedInV3` TEXT, `name` TEXT DEFAULT 'Unknown', `addedInV9` TEXT)");
            db.execSQL("INSERT INTO Entity2 (id, addedInV3, name, addedInV9) "
                    + "SELECT id, addedInV3, name, addedInV9 FROM save_Entity2");
            db.execSQL("DROP TABLE save_Entity2");
        }
    };

    private static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_Entity1_addedInV10` "
                    + "ON `Entity1` (`addedInV10`)");
        }
    };

    private static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE Entity1 "
                    + "ADD COLUMN added1InV13 INTEGER NOT NULL DEFAULT (0)");
        }
    };

    /**
     * Downgrade migration from {@link MigrationDb#MAX_VERSION} to
     * {@link MigrationDb#LATEST_VERSION} that uses the schema file and re-creates the tables such
     * that the post-migration validation passes.
     *
     * Additionally, it adds a table named NoOp with two rows to be able to distinguish this
     * migration from a destructive migration.
     *
     * This migration allows us to keep creating new schemas for newer tests without updating the
     * downgrade tests.
     */
    private static final Migration MIGRATION_MAX_LATEST = new Migration(
            MigrationDb.MAX_VERSION, MigrationDb.LATEST_VERSION) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Drop Entity1 since its possible that LATEST_VERSION schema defines it differently.
            db.execSQL("DROP TABLE IF EXISTS " + MigrationDb.Entity1.TABLE_NAME);

            try {
                Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
                InputStream input = testContext.getAssets().open(MigrationDb.class.getCanonicalName()
                        + "/" + MigrationDb.LATEST_VERSION + ".json");
                SchemaBundle schemaBundle = SchemaBundle.deserialize(input);
                for (String query : schemaBundle.getDatabase().buildCreateQueries()) {
                    db.execSQL(query);
                }
            } catch (IOException e) {
                // Re-throw as a runtime exception so test fails if something goes wrong.
                throw new RuntimeException(e);
            }

            db.execSQL("CREATE TABLE IF NOT EXISTS `NoOp` (`id` INTEGER NOT NULL,"
                    + " PRIMARY KEY(`id`))");
            db.execSQL("INSERT INTO `NoOp` (`id`) VALUES (1)");
            db.execSQL("INSERT INTO `NoOp` (`id`) VALUES (2)");
        }
    };

    private static final Migration[] ALL_MIGRATIONS = new Migration[]{MIGRATION_1_2,
            MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12,
            MIGRATION_12_13};

    static final class EmptyMigration extends Migration {
        EmptyMigration(int startVersion, int endVersion) {
            super(startVersion, endVersion);
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // do nothing
        }
    }
}
