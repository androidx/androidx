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

package androidx.room.integration.kotlintestapp.migration

import android.support.test.InstrumentationRegistry
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.room.util.TableInfo
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class MigrationKotlinTest {

    @get:Rule
    var helper: MigrationTestHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MigrationDbKotlin::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory())

    companion object {
        val TEST_DB = "migration-test"
    }

    @Test
    @Throws(IOException::class)
    fun giveBadResource() {
        val helper = MigrationTestHelper(
                InstrumentationRegistry.getInstrumentation(),
                "foo", FrameworkSQLiteOpenHelperFactory())
        try {
            helper.createDatabase(TEST_DB, 1)
            throw AssertionError("must have failed with missing file exception")
        } catch (exception: FileNotFoundException) {
            assertThat<String>(exception.message, containsString("Cannot find"))
        }
    }

    @Test
    @Throws(IOException::class)
    fun startInCurrentVersion() {
        val db = helper.createDatabase(TEST_DB,
                MigrationDbKotlin.LATEST_VERSION)
        val dao = MigrationDbKotlin.Dao_V1(db)
        dao.insertIntoEntity1(2, "x")
        db.close()
        val migrationDb = getLatestDb()
        val items = migrationDb.dao().loadAllEntity1s()
        helper.closeWhenFinished(migrationDb)
        assertThat<Int>(items.size, `is`<Int>(1))
    }

    @Test
    @Throws(IOException::class)
    fun addTable() {
        var db = helper.createDatabase(TEST_DB, 1)
        val dao = MigrationDbKotlin.Dao_V1(db)
        dao.insertIntoEntity1(2, "foo")
        dao.insertIntoEntity1(3, "bar")
        db.close()
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true,
                MIGRATION_1_2)
        MigrationDbKotlin.Dao_V2(db).insertIntoEntity2(3, "blah")
        db.close()
        val migrationDb = getLatestDb()
        val entity1s = migrationDb.dao().loadAllEntity1s()

        assertThat(entity1s.size, `is`(2))
        val entity2 = MigrationDbKotlin.Entity2(2, null, "bar")
        // assert no error happens
        migrationDb.dao().insert(entity2)
        val entity2s = migrationDb.dao().loadAllEntity2s()
        assertThat(entity2s.size, `is`(2))
    }

    private fun getLatestDb(): MigrationDbKotlin {
        val db = Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                MigrationDbKotlin::class.java, TEST_DB).addMigrations(*ALL_MIGRATIONS).build()
        // trigger open
        db.beginTransaction()
        db.endTransaction()
        helper.closeWhenFinished(db)
        return db
    }

    @Test
    @Throws(IOException::class)
    fun addTableFailure() {
        testFailure(1, 2)
    }

    @Test
    @Throws(IOException::class)
    fun addColumnFailure() {
        val db = helper.createDatabase(TEST_DB, 2)
        db.close()
        var caught: IllegalStateException? = null
        try {
            helper.runMigrationsAndValidate(TEST_DB, 3, true,
                    EmptyMigration(2, 3))
        } catch (ex: IllegalStateException) {
            caught = ex
        }

        assertThat<IllegalStateException>(caught,
                instanceOf<IllegalStateException>(IllegalStateException::class.java))
    }

    @Test
    @Throws(IOException::class)
    fun addColumn() {
        val db = helper.createDatabase(TEST_DB, 2)
        val v2Dao = MigrationDbKotlin.Dao_V2(db)
        v2Dao.insertIntoEntity2(7, "blah")
        db.close()
        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
        // trigger open.
        val migrationDb = getLatestDb()
        val entity2s = migrationDb.dao().loadAllEntity2s()
        assertThat(entity2s.size, `is`(1))
        assertThat<String>(entity2s[0].name, `is`("blah"))
        assertThat<String>(entity2s[0].addedInV3, `is`<Any>(nullValue()))

        val entity2Pojos = migrationDb.dao().loadAllEntity2sAsPojo()
        assertThat(entity2Pojos.size, `is`(1))
        assertThat<String>(entity2Pojos[0].name, `is`("blah"))
        assertThat<String>(entity2Pojos[0].addedInV3, `is`<Any>(nullValue()))
    }

    @Test
    @Throws(IOException::class)
    fun failedToRemoveColumn() {
        testFailure(4, 5)
    }

    @Test
    @Throws(IOException::class)
    fun removeColumn() {
        helper.createDatabase(TEST_DB, 4)
        val db = helper.runMigrationsAndValidate(TEST_DB,
                5, true, MIGRATION_4_5)
        val info = TableInfo.read(db, MigrationDbKotlin.Entity3.TABLE_NAME)
        assertThat(info.columns.size, `is`(2))
    }

    @Test
    @Throws(IOException::class)
    fun dropTable() {
        helper.createDatabase(TEST_DB, 5)
        val db = helper.runMigrationsAndValidate(TEST_DB,
                6, true, MIGRATION_5_6)
        val info = TableInfo.read(db, MigrationDbKotlin.Entity3.TABLE_NAME)
        assertThat(info.columns.size, `is`(0))
    }

    @Test
    @Throws(IOException::class)
    fun failedToDropTable() {
        testFailure(5, 6)
    }

    @Test
    @Throws(IOException::class)
    fun failedToDropTableDontVerify() {
        helper.createDatabase(TEST_DB, 5)
        val db = helper.runMigrationsAndValidate(TEST_DB,
                6, false, EmptyMigration(5, 6))
        val info = TableInfo.read(db, MigrationDbKotlin.Entity3.TABLE_NAME)
        assertThat(info.columns.size, `is`(2))
    }

    @Test
    @Throws(IOException::class)
    fun failedForeignKey() {
        val db = helper.createDatabase(TEST_DB, 6)
        db.close()
        var throwable: Throwable? = null
        try {
            helper.runMigrationsAndValidate(TEST_DB,
                    7, false, object : Migration(6, 7) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE Entity4 (`id` INTEGER, `name` TEXT,"
                            + " PRIMARY KEY(`id`))")
                }
            })
        } catch (t: Throwable) {
            throwable = t
        }

        assertThat<Throwable>(throwable, instanceOf<Throwable>(IllegalStateException::class.java))

        assertThat<String>(throwable!!.message, containsString("Migration failed"))
    }

    @Test
    @Throws(IOException::class)
    fun newTableWithForeignKey() {
        helper.createDatabase(TEST_DB, 6)
        val db = helper.runMigrationsAndValidate(TEST_DB,
                7, false, MIGRATION_6_7)
        val info = TableInfo.read(db, MigrationDbKotlin.Entity4.TABLE_NAME)
        assertThat(info.foreignKeys.size, `is`(1))
    }

    @Throws(IOException::class)
    private fun testFailure(startVersion: Int, endVersion: Int) {
        val db = helper.createDatabase(TEST_DB, startVersion)
        db.close()
        var throwable: Throwable? = null
        try {
            helper.runMigrationsAndValidate(TEST_DB, endVersion, true,
                    EmptyMigration(startVersion, endVersion))
        } catch (t: Throwable) {
            throwable = t
        }

        assertThat<Throwable>(throwable, instanceOf<Throwable>(IllegalStateException::class.java))
        assertThat<String>(throwable!!.message, containsString("Migration failed"))
    }

    internal val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `Entity2` (`id` INTEGER NOT NULL,"
                    + " `name` TEXT, PRIMARY KEY(`id`))")
        }
    }

    internal val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE " + MigrationDbKotlin.Entity2.TABLE_NAME
                    + " ADD COLUMN addedInV3 TEXT")
        }
    }

    internal val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `Entity3` (`id` INTEGER NOT NULL,"
                    + " `removedInV5` TEXT, `name` TEXT, PRIMARY KEY(`id`))")
        }
    }

    internal val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `Entity3_New` (`id` INTEGER NOT NULL,"
                    + " `name` TEXT, PRIMARY KEY(`id`))")
            database.execSQL("INSERT INTO Entity3_New(`id`, `name`) "
                    + "SELECT `id`, `name` FROM Entity3")
            database.execSQL("DROP TABLE Entity3")
            database.execSQL("ALTER TABLE Entity3_New RENAME TO Entity3")
        }
    }

    internal val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DROP TABLE " + MigrationDbKotlin.Entity3.TABLE_NAME)
        }
    }

    internal val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS "
                    + MigrationDbKotlin.Entity4.TABLE_NAME
                    + " (`id` INTEGER NOT NULL, `name` TEXT, PRIMARY KEY(`id`),"
                    + " FOREIGN KEY(`name`) REFERENCES `Entity1`(`name`)"
                    + " ON UPDATE NO ACTION ON DELETE NO ACTION DEFERRABLE INITIALLY DEFERRED)")
            database.execSQL("CREATE UNIQUE INDEX `index_entity1` ON "
                    + MigrationDbKotlin.Entity1.TABLE_NAME + " (`name`)")
        }
    }

    private val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
            MIGRATION_5_6, MIGRATION_6_7)

    internal class EmptyMigration(startVersion: Int, endVersion: Int)
        : Migration(startVersion, endVersion) {

        override fun migrate(database: SupportSQLiteDatabase) {
            // do nothing
        }
    }
}
