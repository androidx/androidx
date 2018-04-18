/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import static java.util.Arrays.asList;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
@RunWith(JUnit4.class)
public class BuilderTest {
    @Test(expected = IllegalArgumentException.class)
    public void nullContext() {
        //noinspection ConstantConditions
        Room.databaseBuilder(null, RoomDatabase.class, "bla").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullContext2() {
        //noinspection ConstantConditions
        Room.inMemoryDatabaseBuilder(null, RoomDatabase.class).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullName() {
        //noinspection ConstantConditions
        Room.databaseBuilder(mock(Context.class), RoomDatabase.class, null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyName() {
        Room.databaseBuilder(mock(Context.class), RoomDatabase.class, "  ").build();
    }

    @Test
    public void migration() {
        Migration m1 = new EmptyMigration(0, 1);
        Migration m2 = new EmptyMigration(1, 2);
        TestDatabase db = Room.databaseBuilder(mock(Context.class), TestDatabase.class, "foo")
                .addMigrations(m1, m2).build();
        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        RoomDatabase.MigrationContainer migrations = config.migrationContainer;
        assertThat(migrations.findMigrationPath(0, 1), is(asList(m1)));
        assertThat(migrations.findMigrationPath(1, 2), is(asList(m2)));
        assertThat(migrations.findMigrationPath(0, 2), is(asList(m1, m2)));
        assertThat(migrations.findMigrationPath(2, 0), CoreMatchers.<List<Migration>>nullValue());
        assertThat(migrations.findMigrationPath(0, 3), CoreMatchers.<List<Migration>>nullValue());
    }

    @Test
    public void migrationOverride() {
        Migration m1 = new EmptyMigration(0, 1);
        Migration m2 = new EmptyMigration(1, 2);
        Migration m3 = new EmptyMigration(0, 1);
        TestDatabase db = Room.databaseBuilder(mock(Context.class), TestDatabase.class, "foo")
                .addMigrations(m1, m2, m3).build();
        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        RoomDatabase.MigrationContainer migrations = config.migrationContainer;
        assertThat(migrations.findMigrationPath(0, 1), is(asList(m3)));
        assertThat(migrations.findMigrationPath(1, 2), is(asList(m2)));
        assertThat(migrations.findMigrationPath(0, 3), CoreMatchers.<List<Migration>>nullValue());
    }

    @Test
    public void migrationJump() {
        Migration m1 = new EmptyMigration(0, 1);
        Migration m2 = new EmptyMigration(1, 2);
        Migration m3 = new EmptyMigration(2, 3);
        Migration m4 = new EmptyMigration(0, 3);
        TestDatabase db = Room.databaseBuilder(mock(Context.class), TestDatabase.class, "foo")
                .addMigrations(m1, m2, m3, m4).build();
        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        RoomDatabase.MigrationContainer migrations = config.migrationContainer;
        assertThat(migrations.findMigrationPath(0, 3), is(asList(m4)));
        assertThat(migrations.findMigrationPath(1, 3), is(asList(m2, m3)));
    }

    @Test
    public void migrationDowngrade() {
        Migration m1_2 = new EmptyMigration(1, 2);
        Migration m2_3 = new EmptyMigration(2, 3);
        Migration m3_4 = new EmptyMigration(3, 4);
        Migration m3_2 = new EmptyMigration(3, 2);
        Migration m2_1 = new EmptyMigration(2, 1);
        TestDatabase db = Room.databaseBuilder(mock(Context.class), TestDatabase.class, "foo")
                .addMigrations(m1_2, m2_3, m3_4, m3_2, m2_1).build();
        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        RoomDatabase.MigrationContainer migrations = config.migrationContainer;
        assertThat(migrations.findMigrationPath(3, 2), is(asList(m3_2)));
        assertThat(migrations.findMigrationPath(3, 1), is(asList(m3_2, m2_1)));
    }

    @Test
    public void skipMigration() {
        Context context = mock(Context.class);

        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .fallbackToDestructiveMigration()
                .build();

        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        assertThat(config.requireMigration, is(false));
    }

    @Test
    public void fallbackToDestructiveMigrationFrom_calledOnce_migrationsNotRequiredForValues() {
        Context context = mock(Context.class);

        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .fallbackToDestructiveMigrationFrom(1, 2).build();

        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        assertThat(config.isMigrationRequiredFrom(1), is(false));
        assertThat(config.isMigrationRequiredFrom(2), is(false));
    }

    @Test
    public void fallbackToDestructiveMigrationFrom_calledTwice_migrationsNotRequiredForValues() {
        Context context = mock(Context.class);
        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .fallbackToDestructiveMigrationFrom(1, 2)
                .fallbackToDestructiveMigrationFrom(3, 4)
                .build();
        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;

        assertThat(config.isMigrationRequiredFrom(1), is(false));
        assertThat(config.isMigrationRequiredFrom(2), is(false));
        assertThat(config.isMigrationRequiredFrom(3), is(false));
        assertThat(config.isMigrationRequiredFrom(4), is(false));
    }

    @Test
    public void isMigrationRequiredFrom_fallBackToDestructiveCalled_alwaysReturnsFalse() {
        Context context = mock(Context.class);

        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .fallbackToDestructiveMigration()
                .build();

        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        assertThat(config.isMigrationRequiredFrom(0), is(false));
        assertThat(config.isMigrationRequiredFrom(1), is(false));
        assertThat(config.isMigrationRequiredFrom(5), is(false));
        assertThat(config.isMigrationRequiredFrom(12), is(false));
        assertThat(config.isMigrationRequiredFrom(132), is(false));
    }

    @Test
    public void isMigrationRequiredFrom_byDefault_alwaysReturnsTrue() {
        Context context = mock(Context.class);

        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .build();

        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        assertThat(config.isMigrationRequiredFrom(0), is(true));
        assertThat(config.isMigrationRequiredFrom(1), is(true));
        assertThat(config.isMigrationRequiredFrom(5), is(true));
        assertThat(config.isMigrationRequiredFrom(12), is(true));
        assertThat(config.isMigrationRequiredFrom(132), is(true));
    }

    @Test
    public void isMigrationRequiredFrom_fallBackToDestFromCalled_falseForProvidedValues() {
        Context context = mock(Context.class);

        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .fallbackToDestructiveMigrationFrom(1, 4, 81)
                .build();

        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        assertThat(config.isMigrationRequiredFrom(1), is(false));
        assertThat(config.isMigrationRequiredFrom(4), is(false));
        assertThat(config.isMigrationRequiredFrom(81), is(false));
    }

    @Test
    public void isMigrationRequiredFrom_fallBackToDestFromCalled_trueForNonProvidedValues() {
        Context context = mock(Context.class);

        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .fallbackToDestructiveMigrationFrom(1, 4, 81)
                .build();

        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        assertThat(config.isMigrationRequiredFrom(2), is(true));
        assertThat(config.isMigrationRequiredFrom(3), is(true));
        assertThat(config.isMigrationRequiredFrom(73), is(true));
    }

    @Test
    public void createBasic() {
        Context context = mock(Context.class);
        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class).build();
        assertThat(db, instanceOf(BuilderTest_TestDatabase_Impl.class));
        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        assertThat(config, notNullValue());
        assertThat(config.context, is(context));
        assertThat(config.name, is(nullValue()));
        assertThat(config.allowMainThreadQueries, is(false));
        assertThat(config.journalMode, is(RoomDatabase.JournalMode.TRUNCATE));
        assertThat(config.sqliteOpenHelperFactory,
                instanceOf(FrameworkSQLiteOpenHelperFactory.class));
    }

    @Test
    public void createAllowMainThread() {
        Context context = mock(Context.class);
        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .allowMainThreadQueries()
                .build();
        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        assertThat(config.allowMainThreadQueries, is(true));
    }

    @Test
    public void createWriteAheadLogging() {
        Context context = mock(Context.class);
        TestDatabase db = Room.databaseBuilder(context, TestDatabase.class, "foo")
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING).build();
        assertThat(db, instanceOf(BuilderTest_TestDatabase_Impl.class));
        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        assertThat(config.journalMode, is(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING));
    }

    @Test
    public void createWithFactoryAndVersion() {
        Context context = mock(Context.class);
        SupportSQLiteOpenHelper.Factory factory = mock(SupportSQLiteOpenHelper.Factory.class);

        TestDatabase db = Room.inMemoryDatabaseBuilder(context, TestDatabase.class)
                .openHelperFactory(factory)
                .build();
        assertThat(db, instanceOf(BuilderTest_TestDatabase_Impl.class));
        DatabaseConfiguration config = ((BuilderTest_TestDatabase_Impl) db).mConfig;
        assertThat(config, notNullValue());
        assertThat(config.sqliteOpenHelperFactory, is(factory));
    }

    abstract static class TestDatabase extends RoomDatabase {
    }

    static class EmptyMigration extends Migration {
        EmptyMigration(int start, int end) {
            super(start, end);
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
        }
    }

}
