/*
 * Copyright 2018 The Android Open Source Project
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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.FtsOptions;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
public class FtsMigrationTest {
    private static final String TEST_DB = "migration-test";
    @Rule
    public MigrationTestHelper helper;

    public FtsMigrationTest() {
        helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
                FtsMigrationDb.class.getCanonicalName());
    }

    @Database(entities = {Book.class, User.class, AddressFts.class, Mail.class}, version = 6)
    abstract static class FtsMigrationDb extends RoomDatabase {
        abstract BookDao getBookDao();
        abstract UserDao getUserDao();
    }

    @Dao
    interface BookDao {
        @Insert
        void insert(Book book);

        @Query("SELECT * FROM BOOK WHERE title MATCH :title")
        Book getBook(String title);

        @Query("SELECT * FROM BOOK")
        Book getAllBooks();
    }

    @Dao
    interface UserDao {
        @Query("SELECT * FROM AddressFts WHERE AddressFts MATCH :searchQuery")
        List<Address> searchAddress(String searchQuery);
    }

    @Entity
    @Fts4(matchInfo = FtsOptions.MatchInfo.FTS3)
    static class Book {
        public String title;
        public String author;
        public int numOfPages;
        public String text;
    }

    @Entity
    static class User {
        @PrimaryKey
        public long id;
        public String firstName;
        public String lastName;
        @Embedded
        public Address address;
    }

    @Entity
    @Fts4(contentEntity = User.class)
    static class AddressFts {
        @Embedded
        public Address address;
    }

    static class Address {
        public String line1;
        public String line2;
        public String state;
        public int zipcode;
    }

    @Entity
    @Fts4(languageId = "lid")
    static class Mail {
        @PrimaryKey
        @ColumnInfo(name = "rowid")
        public long mailId;
        public String content;
        public int lid;
    }

    @Test
    public void validMigration() throws Exception {
        SupportSQLiteDatabase db;

        db = helper.createDatabase(TEST_DB, 1);
        db.execSQL("INSERT INTO BOOK VALUES('Ready Player One', 'Ernest Cline', 402, "
                + "'Everyone my age remembers where they were and what they were doing...')");
        db.close();

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2);

        Book book = getLatestDb().getBookDao().getBook("Ready Player");
        assertThat(book.title, is("Ready Player One"));
        assertThat(book.author, is("Ernest Cline"));
        assertThat(book.numOfPages, is(402));
    }

    @Test
    public void invalidMigration_missingFtsOption() throws Exception {
        SupportSQLiteDatabase supportSQLiteDatabase = helper.createDatabase(TEST_DB, 1);
        supportSQLiteDatabase.close();

        try {
            Context targetContext = ApplicationProvider.getApplicationContext();
            FtsMigrationDb db = Room.databaseBuilder(targetContext, FtsMigrationDb.class, TEST_DB)
                    .addMigrations(BAD_MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                            MIGRATION_5_6)
                    .build();
            helper.closeWhenFinished(db);
            db.getBookDao().getAllBooks();
            fail("Should have failed migration.");
        } catch (IllegalStateException ex) {
            assertThat(ex.getMessage(), containsString("Migration didn't properly handle"));
        }
    }

    @Test
    public void validFtsContentMigration() throws Exception {
        SupportSQLiteDatabase db;

        db = helper.createDatabase(TEST_DB, 3);
        db.execSQL("INSERT INTO Person VALUES(1, 'Ernest', 'Cline', 'Ruth Ave', '', 'TX', 78757)");
        db.close();

        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4);

        List<Address> addresses = getLatestDb().getUserDao().searchAddress("Ruth");
        assertThat(addresses.size(), is(1));
        assertThat(addresses.get(0).line1, is("Ruth Ave"));
    }

    @Test
    public void validFtsWithNamedRowIdMigration() throws Exception {
        SupportSQLiteDatabase db;

        db = helper.createDatabase(TEST_DB, 4);
        db.close();

        helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5);
    }

    @Test
    public void validFtsWithLanguageIdMigration() throws Exception {
        SupportSQLiteDatabase db;

        db = helper.createDatabase(TEST_DB, 5);
        db.close();

        helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6);
    }

    @SuppressWarnings("deprecation")
    private FtsMigrationDb getLatestDb() {
        FtsMigrationDb db = Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                FtsMigrationDb.class, TEST_DB).addMigrations(ALL_MIGRATIONS).build();
        // trigger open
        db.beginTransaction();
        db.endTransaction();
        helper.closeWhenFinished(db);
        return db;
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Book RENAME TO Book_old");
            database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `Book` USING FTS4("
                    + "`title`, `author`, `numOfPages`, `text`, matchinfo=fts3)");
            database.execSQL("INSERT INTO Book SELECT * FROM Book_old");
            database.execSQL("DROP TABLE Book_old");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Person` (`id` INTEGER NOT NULL, "
                            + "`firstName` TEXT, `lastName` TEXT, `line1` TEXT, `line2` TEXT, "
                            + "`state` TEXT, `zipcode` INTEGER, PRIMARY KEY(`id`))");
            database.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `AddressFts` USING FTS4(`line1` TEXT, "
                            + "`line2` TEXT, `state` TEXT, `zipcode` INTEGER, content=`Person`)");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `Person` RENAME TO `User`");
            database.execSQL("DROP TABLE `AddressFts`");
            database.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `AddressFts` USING FTS4(`line1` TEXT, "
                            + "`line2` TEXT, `state` TEXT, `zipcode` INTEGER, content=`User`)");
            database.execSQL(
                    "INSERT INTO `AddressFts` (`docid`, `line1`, `line2`, `state`, `zipcode`) "
                            + "SELECT `rowid`, `line1`, `line2`, `state`, `zipcode` FROM `User`");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `Mail` USING FTS4("
                    + "`content` TEXT NOT NULL)");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE `Mail`");
            database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `Mail` USING FTS4("
                    + "`content` TEXT NOT NULL, languageid=`lid`)");
        }
    };

    private static final Migration BAD_MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE Book");
            database.execSQL("CREATE VIRTUAL TABLE `Book` USING FTS4("
                    + "`title`, `author`, `numOfPages`, `text`)");
        }
    };

    private static final Migration[] ALL_MIGRATIONS = new Migration[]{
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6
    };
}
