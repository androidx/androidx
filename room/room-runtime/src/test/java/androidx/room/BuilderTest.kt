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
package androidx.room

import android.content.Context
import androidx.kruth.assertThat
import androidx.room.Room.databaseBuilder
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.io.File
import java.util.concurrent.Executor
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class BuilderTest {
    @Test
    fun nullName() {
        try {
            databaseBuilder(
                mock(), RoomDatabase::class.java, null
            ).build()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).isEqualTo(
                "Cannot build a database with null or empty name. If you are trying to create an " +
                    "in memory database, use Room.inMemoryDatabaseBuilder"
            )
        }
    }

    @Test
    fun emptyName() {
        try {
            databaseBuilder(
                mock(), RoomDatabase::class.java, "  "
            ).build()
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).isEqualTo(
                "Cannot build a database with null or empty name. If you are trying to create an " +
                    "in memory database, use Room.inMemoryDatabaseBuilder"
            )
        }
    }

    @Test
    fun executors_setQueryExecutor() {
        val executor: Executor = mock()
        val db = databaseBuilder(
            mock(), TestDatabase::class.java, "foo"
        ).setQueryExecutor(executor).build()

        assertThat(db.mDatabaseConfiguration.queryExecutor).isEqualTo(executor)
        assertThat(db.mDatabaseConfiguration.transactionExecutor).isEqualTo(executor)
    }

    @Test
    fun executors_setTransactionExecutor() {
        val executor: Executor = mock()
        val db = databaseBuilder(
            mock(), TestDatabase::class.java, "foo"
        ).setTransactionExecutor(executor).build()

        assertThat(db.mDatabaseConfiguration.queryExecutor).isEqualTo(executor)
        assertThat(db.mDatabaseConfiguration.transactionExecutor).isEqualTo(executor)
    }

    @Test
    fun executors_setBothExecutors() {
        val executor1: Executor = mock()
        val executor2: Executor = mock()
        val db = databaseBuilder(
            mock(), TestDatabase::class.java, "foo"
        ).setQueryExecutor(executor1).setTransactionExecutor(executor2).build()

        assertThat(db.mDatabaseConfiguration.queryExecutor).isEqualTo(executor1)
        assertThat(db.mDatabaseConfiguration.transactionExecutor).isEqualTo(executor2)
    }

    @Test
    fun migration() {
        val m1: Migration = EmptyMigration(0, 1)
        val m2: Migration = EmptyMigration(1, 2)
        val db = databaseBuilder(
            mock(), TestDatabase::class.java, "foo"
        ).addMigrations(m1, m2).build()

        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        val migrations = config.migrationContainer

        assertThat(migrations.findMigrationPath(0, 1)).containsExactlyElementsIn(listOf(m1))
        assertThat(migrations.findMigrationPath(1, 2)).containsExactlyElementsIn(listOf(m2))
        assertThat(migrations.findMigrationPath(0, 2))
            .containsExactlyElementsIn(listOf(m1, m2))
        assertThat(migrations.findMigrationPath(2, 0)).isNull()
        assertThat(migrations.findMigrationPath(0, 3)).isNull()
    }

    @Test
    fun migrationOverride() {
        val m1: Migration = EmptyMigration(0, 1)
        val m2: Migration = EmptyMigration(1, 2)
        val m3: Migration = EmptyMigration(0, 1)
        val db = databaseBuilder(
            mock(), TestDatabase::class.java, "foo"
        ).addMigrations(m1, m2, m3).build()

        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        val migrations = config.migrationContainer

        assertThat(migrations.findMigrationPath(0, 1)).containsExactlyElementsIn(listOf(m3))
        assertThat(migrations.findMigrationPath(1, 2)).containsExactlyElementsIn(listOf(m2))
        assertThat(migrations.findMigrationPath(0, 3)).isNull()
    }

    @Test
    fun migrationJump() {
        val m1: Migration = EmptyMigration(0, 1)
        val m2: Migration = EmptyMigration(1, 2)
        val m3: Migration = EmptyMigration(2, 3)
        val m4: Migration = EmptyMigration(0, 3)
        val db = databaseBuilder(
            mock(), TestDatabase::class.java, "foo"
        ).addMigrations(m1, m2, m3, m4).build()

        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        val migrations = config.migrationContainer

        assertThat(migrations.findMigrationPath(0, 3)).containsExactlyElementsIn(listOf(m4))
        assertThat(migrations.findMigrationPath(1, 3))
            .containsExactlyElementsIn(listOf(m2, m3))
    }

    @Test
    fun migrationDowngrade() {
        val m1_2: Migration = EmptyMigration(1, 2)
        val m2_3: Migration = EmptyMigration(2, 3)
        val m3_4: Migration = EmptyMigration(3, 4)
        val m3_2: Migration = EmptyMigration(3, 2)
        val m2_1: Migration = EmptyMigration(2, 1)
        val db = databaseBuilder(
            mock(), TestDatabase::class.java, "foo"
        )
            .addMigrations(m1_2, m2_3, m3_4, m3_2, m2_1).build()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        val migrations = config.migrationContainer
        assertThat(migrations.findMigrationPath(3, 2))
            .containsExactlyElementsIn(listOf(m3_2))
        assertThat(migrations.findMigrationPath(3, 1))
            .containsExactlyElementsIn(listOf(m3_2, m2_1))
    }

    @Test
    fun skipMigration() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java)
            .fallbackToDestructiveMigration()
            .build()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        assertThat(config.requireMigration).isFalse()
    }

    @Test
    fun fallbackToDestructiveMigrationFrom_calledOnce_migrationsNotRequiredForValues() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java)
            .fallbackToDestructiveMigrationFrom(1, 2).build()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        assertThat(config.isMigrationRequired(1, 2)).isFalse()
        assertThat(config.isMigrationRequired(2, 3)).isFalse()
    }

    @Test
    fun fallbackToDestructiveMigrationFrom_calledTwice_migrationsNotRequiredForValues() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java)
            .fallbackToDestructiveMigrationFrom(1, 2)
            .fallbackToDestructiveMigrationFrom(3, 4)
            .build()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        assertThat(config.isMigrationRequired(1, 2)).isFalse()
        assertThat(config.isMigrationRequired(2, 3)).isFalse()
        assertThat(config.isMigrationRequired(3, 4)).isFalse()
        assertThat(config.isMigrationRequired(4, 5)).isFalse()
    }

    @Test
    fun isMigrationRequiredFrom_fallBackToDestructiveCalled_alwaysReturnsFalse() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java)
            .fallbackToDestructiveMigration()
            .build()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig

        assertThat(config.isMigrationRequired(0, 1)).isFalse()
        assertThat(config.isMigrationRequired(1, 2)).isFalse()
        assertThat(config.isMigrationRequired(5, 6)).isFalse()
        assertThat(config.isMigrationRequired(7, 12)).isFalse()
        assertThat(config.isMigrationRequired(132, 150)).isFalse()

        assertThat(config.isMigrationRequired(1, 0)).isFalse()
        assertThat(config.isMigrationRequired(2, 1)).isFalse()
        assertThat(config.isMigrationRequired(6, 5)).isFalse()
        assertThat(config.isMigrationRequired(7, 12)).isFalse()
        assertThat(config.isMigrationRequired(150, 132)).isFalse()
    }

    // isMigrationRequiredFrom doesn't know about downgrade only so it always returns true
    @Test
    fun isMigrationRequired_destructiveMigrationOnDowngrade_returnTrueWhenUpgrading() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig

        // isMigrationRequiredFrom doesn't know about downgrade only so it always returns true
        assertThat(config.isMigrationRequired(0, 1)).isTrue()
        assertThat(config.isMigrationRequired(1, 2)).isTrue()
        assertThat(config.isMigrationRequired(5, 6)).isTrue()
        assertThat(config.isMigrationRequired(7, 12)).isTrue()
        assertThat(config.isMigrationRequired(132, 150)).isTrue()
    }

    // isMigrationRequiredFrom doesn't know about downgrade only so it always returns true
    @Test
    fun isMigrationRequired_destructiveMigrationOnDowngrade_returnFalseWhenDowngrading() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig

        // isMigrationRequiredFrom doesn't know about downgrade only so it always returns true
        assertThat(config.isMigrationRequired(1, 0)).isFalse()
        assertThat(config.isMigrationRequired(2, 1)).isFalse()
        assertThat(config.isMigrationRequired(6, 5)).isFalse()
        assertThat(config.isMigrationRequired(12, 7)).isFalse()
        assertThat(config.isMigrationRequired(150, 132)).isFalse()
    }

    @Test
    fun isMigrationRequiredFrom_byDefault_alwaysReturnsTrue() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java)
            .build()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig

        assertThat(config.isMigrationRequired(0, 1)).isTrue()
        assertThat(config.isMigrationRequired(1, 2)).isTrue()
        assertThat(config.isMigrationRequired(5, 6)).isTrue()
        assertThat(config.isMigrationRequired(7, 12)).isTrue()
        assertThat(config.isMigrationRequired(132, 150)).isTrue()

        assertThat(config.isMigrationRequired(1, 0)).isTrue()
        assertThat(config.isMigrationRequired(2, 1)).isTrue()
        assertThat(config.isMigrationRequired(6, 5)).isTrue()
        assertThat(config.isMigrationRequired(7, 12)).isTrue()
        assertThat(config.isMigrationRequired(150, 132)).isTrue()
    }

    @Test
    fun isMigrationRequiredFrom_fallBackToDestFromCalled_falseForProvidedValues() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java)
            .fallbackToDestructiveMigrationFrom(1, 4, 81)
            .build()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        assertThat(config.isMigrationRequired(1, 2)).isFalse()
        assertThat(config.isMigrationRequired(4, 8)).isFalse()
        assertThat(config.isMigrationRequired(81, 90)).isFalse()
    }

    @Test
    fun isMigrationRequiredFrom_fallBackToDestFromCalled_trueForNonProvidedValues() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java)
            .fallbackToDestructiveMigrationFrom(1, 4, 81)
            .build()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        assertThat(config.isMigrationRequired(2, 3)).isTrue()
        assertThat(config.isMigrationRequired(3, 4)).isTrue()
        assertThat(config.isMigrationRequired(73, 80)).isTrue()
    }

    @Test
    fun autoMigrationShouldBeAddedToMigrations_WhenManualDowngradeMigrationIsPresent() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(
            context,
            TestDatabase::class.java
        )
            .addMigrations(EmptyMigration(1, 0))
            .build() as BuilderTest_TestDatabase_Impl
        val config: DatabaseConfiguration = db.mDatabaseConfiguration
        assertThat(
            config.migrationContainer.findMigrationPath(1, 2)).isEqualTo((db.mAutoMigrations)
        )
    }

    @Test
    fun fallbackToDestructiveMigrationOnDowngrade_withProvidedValues_falseForDowngrades() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java)
            .fallbackToDestructiveMigrationOnDowngrade()
            .fallbackToDestructiveMigrationFrom(2, 4).build()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        assertThat(config.isMigrationRequired(1, 2)).isTrue()
        assertThat(config.isMigrationRequired(2, 3)).isFalse()
        assertThat(config.isMigrationRequired(3, 4)).isTrue()
        assertThat(config.isMigrationRequired(4, 5)).isFalse()
        assertThat(config.isMigrationRequired(5, 6)).isTrue()
        assertThat(config.isMigrationRequired(2, 1)).isFalse()
        assertThat(config.isMigrationRequired(3, 2)).isFalse()
        assertThat(config.isMigrationRequired(4, 3)).isFalse()
        assertThat(config.isMigrationRequired(5, 4)).isFalse()
        assertThat(config.isMigrationRequired(6, 5)).isFalse()
    }

    @Test
    fun createBasic() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java).build()
        assertThat(db).isInstanceOf<BuilderTest_TestDatabase_Impl>()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        assertThat(config).isNotNull()
        assertThat(config.context).isEqualTo(context)
        assertThat(config.name).isNull()
        assertThat(config.allowMainThreadQueries).isFalse()
        assertThat(config.journalMode).isEqualTo(RoomDatabase.JournalMode.TRUNCATE)
        assertThat(config.sqliteOpenHelperFactory)
            .isInstanceOf<FrameworkSQLiteOpenHelperFactory>()
    }

    @Test
    fun createAllowMainThread() {
        val context: Context = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        assertThat(config.allowMainThreadQueries).isTrue()
    }

    @Test
    fun createWriteAheadLogging() {
        val context: Context = mock()
        val db = databaseBuilder(context, TestDatabase::class.java, "foo")
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING).build()
        assertThat(db).isInstanceOf<BuilderTest_TestDatabase_Impl>()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        assertThat(config.journalMode).isEqualTo(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
    }

    @Test
    fun createWithFactoryAndVersion() {
        val context: Context = mock()
        val factory: SupportSQLiteOpenHelper.Factory = mock()
        val db = inMemoryDatabaseBuilder(context, TestDatabase::class.java)
            .openHelperFactory(factory)
            .build()
        assertThat(db).isInstanceOf<BuilderTest_TestDatabase_Impl>()
        val config: DatabaseConfiguration = (db as BuilderTest_TestDatabase_Impl).mConfig
        assertThat(config).isNotNull()
        assertThat(config.sqliteOpenHelperFactory).isEqualTo(factory)
    }

    @Test
    fun createFromAssetAndFromFile() {
        var exception: Exception? = null
        try {
            databaseBuilder(
                mock(),
                TestDatabase::class.java,
                "foo"
            )
                .createFromAsset("assets-path")
                .createFromFile(File("not-a--real-file"))
                .build()
            Assert.fail("Build should have thrown")
        } catch (e: Exception) {
            exception = e
        }
        assertThat(exception).isInstanceOf<IllegalArgumentException>()
        assertThat(exception).hasMessageThat().contains("More than one of createFromAsset(), " +
            "createFromInputStream(), and createFromFile() were called on this Builder")
    }

    @Test
    fun createInMemoryFromAsset() {
        var exception: Exception? = null
        try {
            inMemoryDatabaseBuilder(
                mock(),
                TestDatabase::class.java
            )
                .createFromAsset("assets-path")
                .build()
            Assert.fail("Build should have thrown")
        } catch (e: Exception) {
            exception = e
        }
        assertThat(exception).isInstanceOf<IllegalArgumentException>()
        assertThat(exception).hasMessageThat().contains(
            "Cannot create from asset or file for an in-memory"
        )
    }

    @Test
    fun createInMemoryFromFile() {
        var exception: Exception? = null
        try {
            inMemoryDatabaseBuilder(
                mock(),
                TestDatabase::class.java
            )
                .createFromFile(File("not-a--real-file"))
                .build()
            Assert.fail("Build should have thrown")
        } catch (e: Exception) {
            exception = e
        }
        assertThat(exception).isInstanceOf<IllegalArgumentException>()
        assertThat(exception).hasMessageThat().contains(
            "Cannot create from asset or file for an in-memory"
        )
    }

    internal abstract class TestDatabase : RoomDatabase() {
        lateinit var mDatabaseConfiguration: DatabaseConfiguration
        override fun init(configuration: DatabaseConfiguration) {
            super.init(configuration)
            mDatabaseConfiguration = configuration
        }
    }

    internal class EmptyMigration(start: Int, end: Int) : Migration(start, end) {
        override fun migrate(db: SupportSQLiteDatabase) {}
    }
}
