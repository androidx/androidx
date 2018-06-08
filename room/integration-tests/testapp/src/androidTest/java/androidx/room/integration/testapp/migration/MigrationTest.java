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
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.room.testing.MigrationTestHelper;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Test custom database migrations.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MigrationTest {
    private static final String TEST_DB = "migration-test";
    @Rule
    public MigrationTestHelper helper;

    public MigrationTest() {
        helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                MigrationDb.class.getCanonicalName());
    }

    @Test
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
        // trigger open
        db.beginTransaction();
        db.endTransaction();
        helper.closeWhenFinished(db);
        return db;
    }

    @Test
    public void addTableFailure() throws IOException {
        testFailure(1, 2);
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
        testFailure(4, 5);
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
        testFailure(5, 6);
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
                        public void migrate(SupportSQLiteDatabase database) {
                            database.execSQL("CREATE TABLE Entity4 (`id` INTEGER NOT NULL,"
                                    + " `name` TEXT, PRIMARY KEY(`id`))");
                        }
                    });
        } catch (Throwable t) {
            throwable = t;
        }
        assertThat(throwable, instanceOf(IllegalStateException.class));
        //noinspection ConstantConditions
        assertThat(throwable.getMessage(), containsString("Migration failed"));
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
    public void missingMigration() throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 1);
        database.close();
        try {
            Context targetContext = InstrumentationRegistry.getTargetContext();
            MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                    .build();
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

        Context targetContext = InstrumentationRegistry.getTargetContext();
        MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                .fallbackToDestructiveMigration()
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
        Context targetContext = InstrumentationRegistry.getTargetContext();

        MigrationDb db = Room.databaseBuilder(targetContext, MigrationDb.class, TEST_DB)
                .fallbackToDestructiveMigrationFrom(6)
                .build();

        assertThat(db.dao().loadAllEntity1s().size(), is(0));
    }

    @Test
    public void fallbackToDestructiveMigrationFrom_suppliedValueIsMigrationStartVersion_exception()
            throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 6);
        database.close();
        Context targetContext = InstrumentationRegistry.getTargetContext();

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
        Context targetContext = InstrumentationRegistry.getTargetContext();

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

    private void testFailure(int startVersion, int endVersion) throws IOException {
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
        assertThat(throwable.getMessage(), containsString("Migration failed"));
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `Entity2` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + " `name` TEXT)");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE " + MigrationDb.Entity2.TABLE_NAME
                    + " ADD COLUMN addedInV3 TEXT");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `Entity3` (`id` INTEGER NOT NULL,"
                    + " `removedInV5` TEXT, `name` TEXT, PRIMARY KEY(`id`))");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `Entity3_New` (`id` INTEGER NOT NULL,"
                    + " `name` TEXT, PRIMARY KEY(`id`))");
            database.execSQL("INSERT INTO Entity3_New(`id`, `name`) "
                    + "SELECT `id`, `name` FROM Entity3");
            database.execSQL("DROP TABLE Entity3");
            database.execSQL("ALTER TABLE Entity3_New RENAME TO Entity3");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE " + MigrationDb.Entity3.TABLE_NAME);
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS " + MigrationDb.Entity4.TABLE_NAME
                    + " (`id` INTEGER NOT NULL, `name` TEXT COLLATE NOCASE, PRIMARY KEY(`id`),"
                    + " FOREIGN KEY(`name`) REFERENCES `Entity1`(`name`)"
                    + " ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED)");
            database.execSQL("CREATE UNIQUE INDEX `index_entity1` ON "
                    + MigrationDb.Entity1.TABLE_NAME + " (`name`)");
        }
    };

    private static final Migration[] ALL_MIGRATIONS = new Migration[]{MIGRATION_1_2,
            MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7};

    static final class EmptyMigration extends Migration {
        EmptyMigration(int startVersion, int endVersion) {
            super(startVersion, endVersion);
        }

        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // do nothing
        }
    }
}
