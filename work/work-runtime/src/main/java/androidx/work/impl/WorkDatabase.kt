/*
 * Copyright 2017 The Android Open Source Project
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
package androidx.work.impl

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.work.Data
import androidx.work.impl.WorkDatabaseVersions.VERSION_10
import androidx.work.impl.WorkDatabaseVersions.VERSION_11
import androidx.work.impl.WorkDatabaseVersions.VERSION_2
import androidx.work.impl.WorkDatabaseVersions.VERSION_3
import androidx.work.impl.WorkDatabaseVersions.VERSION_5
import androidx.work.impl.WorkDatabaseVersions.VERSION_6
import androidx.work.impl.model.Dependency
import androidx.work.impl.model.DependencyDao
import androidx.work.impl.model.Preference
import androidx.work.impl.model.PreferenceDao
import androidx.work.impl.model.RawWorkInfoDao
import androidx.work.impl.model.SystemIdInfo
import androidx.work.impl.model.SystemIdInfoDao
import androidx.work.impl.model.WorkName
import androidx.work.impl.model.WorkNameDao
import androidx.work.impl.model.WorkProgress
import androidx.work.impl.model.WorkProgressDao
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.model.WorkSpecDao
import androidx.work.impl.model.WorkTag
import androidx.work.impl.model.WorkTagDao
import androidx.work.impl.model.WorkTypeConverters
import androidx.work.impl.model.WorkTypeConverters.StateIds.COMPLETED_STATES
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * A Room database for keeping track of work states.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Database(
    entities = [Dependency::class, WorkSpec::class, WorkTag::class, SystemIdInfo::class,
        WorkName::class, WorkProgress::class, Preference::class],
    autoMigrations = [
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15, spec = AutoMigration_14_15::class)
    ],
    version = 15
)
@TypeConverters(value = [Data::class, WorkTypeConverters::class])
abstract class WorkDatabase : RoomDatabase() {
    /**
     * @return The Data Access Object for [WorkSpec]s.
     */
    abstract fun workSpecDao(): WorkSpecDao

    /**
     * @return The Data Access Object for [Dependency]s.
     */
    abstract fun dependencyDao(): DependencyDao

    /**
     * @return The Data Access Object for [WorkTag]s.
     */
    abstract fun workTagDao(): WorkTagDao

    /**
     * @return The Data Access Object for [SystemIdInfo]s.
     */
    abstract fun systemIdInfoDao(): SystemIdInfoDao

    /**
     * @return The Data Access Object for [WorkName]s.
     */
    abstract fun workNameDao(): WorkNameDao

    /**
     * @return The Data Access Object for [WorkProgress].
     */
    abstract fun workProgressDao(): WorkProgressDao

    /**
     * @return The Data Access Object for [Preference].
     */
    abstract fun preferenceDao(): PreferenceDao

    /**
     * @return The Data Access Object which can be used to execute raw queries.
     */
    abstract fun rawWorkInfoDao(): RawWorkInfoDao

    companion object {
        /**
         * Creates an instance of the WorkDatabase.
         *
         * @param context         A context (this method will use the application context from it)
         * @param queryExecutor   An [Executor] that will be used to execute all async Room
         * queries.
         * @param useTestDatabase `true` to generate an in-memory database that allows main thread
         * access
         * @return The created WorkDatabase
         */
        @JvmStatic
        fun create(
            context: Context,
            queryExecutor: Executor,
            useTestDatabase: Boolean
        ): WorkDatabase {
            val builder = if (useTestDatabase) {
                Room.inMemoryDatabaseBuilder(context, WorkDatabase::class.java)
                    .allowMainThreadQueries()
            } else {
                Room.databaseBuilder(context, WorkDatabase::class.java, WORK_DATABASE_NAME)
                    .openHelperFactory { configuration ->
                        val configBuilder = SupportSQLiteOpenHelper.Configuration.builder(context)
                        configBuilder.name(configuration.name)
                            .callback(configuration.callback)
                            .noBackupDirectory(true)
                        FrameworkSQLiteOpenHelperFactory().create(configBuilder.build())
                    }
            }
            return builder.setQueryExecutor(queryExecutor)
                .addCallback(CleanupCallback)
                .addMigrations(Migration_1_2)
                .addMigrations(RescheduleMigration(context, VERSION_2, VERSION_3))
                .addMigrations(Migration_3_4)
                .addMigrations(Migration_4_5)
                .addMigrations(RescheduleMigration(context, VERSION_5, VERSION_6))
                .addMigrations(Migration_6_7)
                .addMigrations(Migration_7_8)
                .addMigrations(Migration_8_9)
                .addMigrations(WorkMigration9To10(context))
                .addMigrations(RescheduleMigration(context, VERSION_10, VERSION_11))
                .addMigrations(Migration_11_12)
                .addMigrations(Migration_12_13)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

// Delete rows in the workspec table that...
private const val PRUNE_SQL_FORMAT_PREFIX =
    // are completed...
    "DELETE FROM workspec WHERE state IN $COMPLETED_STATES AND " +
        // and the minimum retention time has expired...
        "(last_enqueue_time + minimum_retention_duration) < "

// and all dependents are completed.
private const val PRUNE_SQL_FORMAT_SUFFIX = " AND " +
    "(SELECT COUNT(*)=0 FROM dependency WHERE " +
    "    prerequisite_id=id AND " +
    "    work_spec_id NOT IN " +
    "        (SELECT id FROM workspec WHERE state IN $COMPLETED_STATES))"
private val PRUNE_THRESHOLD_MILLIS = TimeUnit.DAYS.toMillis(1)

internal object CleanupCallback : RoomDatabase.Callback() {
    private val pruneSQL: String
        get() = "$PRUNE_SQL_FORMAT_PREFIX$pruneDate$PRUNE_SQL_FORMAT_SUFFIX"

    val pruneDate: Long
        get() = System.currentTimeMillis() - PRUNE_THRESHOLD_MILLIS

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        db.beginTransaction()
        try {
            // Prune everything that is completed, has an expired retention time, and has no
            // active dependents:
            db.execSQL(pruneSQL)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
