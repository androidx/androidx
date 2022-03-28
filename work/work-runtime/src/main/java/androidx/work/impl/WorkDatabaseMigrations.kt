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
package androidx.work.impl

import android.content.ContentValues
import android.content.Context
import android.os.Build
import androidx.room.OnConflictStrategy
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.impl.WorkDatabaseVersions.VERSION_1
import androidx.work.impl.WorkDatabaseVersions.VERSION_10
import androidx.work.impl.WorkDatabaseVersions.VERSION_11
import androidx.work.impl.WorkDatabaseVersions.VERSION_12
import androidx.work.impl.WorkDatabaseVersions.VERSION_13
import androidx.work.impl.WorkDatabaseVersions.VERSION_2
import androidx.work.impl.WorkDatabaseVersions.VERSION_3
import androidx.work.impl.WorkDatabaseVersions.VERSION_4
import androidx.work.impl.WorkDatabaseVersions.VERSION_5
import androidx.work.impl.WorkDatabaseVersions.VERSION_6
import androidx.work.impl.WorkDatabaseVersions.VERSION_7
import androidx.work.impl.WorkDatabaseVersions.VERSION_8
import androidx.work.impl.WorkDatabaseVersions.VERSION_9
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.model.WorkTypeConverters.StateIds.COMPLETED_STATES
import androidx.work.impl.utils.PreferenceUtils
import androidx.work.impl.utils.migrateLegacyIdGenerator

/**
 * Migration helpers for [androidx.work.impl.WorkDatabase].
 */
internal object WorkDatabaseVersions {
    // Known WorkDatabase versions
    const val VERSION_1 = 1
    const val VERSION_2 = 2
    const val VERSION_3 = 3
    const val VERSION_4 = 4
    const val VERSION_5 = 5
    const val VERSION_6 = 6
    const val VERSION_7 = 7
    const val VERSION_8 = 8
    const val VERSION_9 = 9
    const val VERSION_10 = 10
    const val VERSION_11 = 11
    const val VERSION_12 = 12
    const val VERSION_13 = 13

    // (as well as version_13): 2.8.0-alpha01, making required_network_type, content_uri_triggers
    // non null
    const val VERSION_14 = 14

    // renaming period_start_time to last_enqueue_time and adding period_count
    const val VERSION_15 = 15
}

private const val CREATE_SYSTEM_ID_INFO =
    """
    CREATE TABLE IF NOT EXISTS `SystemIdInfo` (`work_spec_id` TEXT NOT NULL, `system_id`
    INTEGER NOT NULL, PRIMARY KEY(`work_spec_id`), FOREIGN KEY(`work_spec_id`)
    REFERENCES `WorkSpec`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )
    """
private const val MIGRATE_ALARM_INFO_TO_SYSTEM_ID_INFO =
    """
    INSERT INTO SystemIdInfo(work_spec_id, system_id)
    SELECT work_spec_id, alarm_id AS system_id FROM alarmInfo
    """
private const val PERIODIC_WORK_SET_SCHEDULE_REQUESTED_AT =
    """
    UPDATE workspec SET schedule_requested_at = 0
    WHERE state NOT IN $COMPLETED_STATES
        AND schedule_requested_at = ${WorkSpec.SCHEDULE_NOT_REQUESTED_YET}
        AND interval_duration <> 0
    """
private const val REMOVE_ALARM_INFO = "DROP TABLE IF EXISTS alarmInfo"
private const val WORKSPEC_ADD_TRIGGER_UPDATE_DELAY =
    "ALTER TABLE workspec ADD COLUMN `trigger_content_update_delay` INTEGER NOT NULL DEFAULT -1"
private const val WORKSPEC_ADD_TRIGGER_MAX_CONTENT_DELAY =
    "ALTER TABLE workspec ADD COLUMN `trigger_max_content_delay` INTEGER NOT NULL DEFAULT -1"
private const val CREATE_WORK_PROGRESS =
    """
    CREATE TABLE IF NOT EXISTS `WorkProgress` (`work_spec_id` TEXT NOT NULL, `progress`
    BLOB NOT NULL, PRIMARY KEY(`work_spec_id`), FOREIGN KEY(`work_spec_id`)
    REFERENCES `WorkSpec`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )
    """
private const val CREATE_INDEX_PERIOD_START_TIME =
    """
    CREATE INDEX IF NOT EXISTS `index_WorkSpec_period_start_time` ON `workspec`(`period_start_time`)
    """
private const val CREATE_RUN_IN_FOREGROUND =
    "ALTER TABLE workspec ADD COLUMN `run_in_foreground` INTEGER NOT NULL DEFAULT 0"
private const val CREATE_OUT_OF_QUOTA_POLICY =
    "ALTER TABLE workspec ADD COLUMN `out_of_quota_policy` INTEGER NOT NULL DEFAULT 0"

private const val SET_DEFAULT_NETWORK_TYPE =
    "UPDATE workspec SET required_network_type = 0 WHERE required_network_type IS NULL "

private const val SET_DEFAULT_CONTENT_URI_TRIGGERS =
    "UPDATE workspec SET content_uri_triggers = x'' WHERE content_uri_triggers is NULL"

private const val INITIALIZE_PERIOD_COUNTER =
    "UPDATE workspec SET period_count = 1 WHERE last_enqueue_time <> 0 AND interval_duration <> 0"

/**
 * Removes the `alarmInfo` table and substitutes it for a more general
 * `SystemIdInfo` table.
 * Adds implicit work tags for all work (a tag with the worker class name).
 */
object Migration_1_2 : Migration(VERSION_1, VERSION_2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(CREATE_SYSTEM_ID_INFO)
        database.execSQL(MIGRATE_ALARM_INFO_TO_SYSTEM_ID_INFO)
        database.execSQL(REMOVE_ALARM_INFO)
        database.execSQL(
            """
                INSERT OR IGNORE INTO worktag(tag, work_spec_id)
                SELECT worker_class_name AS tag, id AS work_spec_id FROM workspec
                """
        )
    }
}

/**
 * Marks `SCHEDULE_REQUESTED_AT` to something other than
 * `SCHEDULE_NOT_REQUESTED_AT`.
 */
object Migration_3_4 : Migration(VERSION_3, VERSION_4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        if (Build.VERSION.SDK_INT >= WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
            database.execSQL(PERIODIC_WORK_SET_SCHEDULE_REQUESTED_AT)
        }
    }
}

/**
 * Adds the `ContentUri` delays to the WorkSpec table.
 */
object Migration_4_5 : Migration(VERSION_4, VERSION_5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(WORKSPEC_ADD_TRIGGER_UPDATE_DELAY)
        database.execSQL(WORKSPEC_ADD_TRIGGER_MAX_CONTENT_DELAY)
    }
}

/**
 * Adds [androidx.work.impl.model.WorkProgress].
 */
object Migration_6_7 : Migration(VERSION_6, VERSION_7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(CREATE_WORK_PROGRESS)
    }
}

/**
 * Adds an index on period_start_time in [WorkSpec].
 */
object Migration_7_8 : Migration(VERSION_7, VERSION_8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(CREATE_INDEX_PERIOD_START_TIME)
    }
}

/**
 * Adds a notification_provider to the [WorkSpec].
 */
object Migration_8_9 : Migration(VERSION_8, VERSION_9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(CREATE_RUN_IN_FOREGROUND)
    }
}

/**
 * Adds a notification_provider to the [WorkSpec].
 */
object Migration_11_12 : Migration(VERSION_11, VERSION_12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(CREATE_OUT_OF_QUOTA_POLICY)
    }
}

object Migration_12_13 : Migration(VERSION_12, VERSION_13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(SET_DEFAULT_NETWORK_TYPE)
        database.execSQL(SET_DEFAULT_CONTENT_URI_TRIGGERS)
    }
}

@RenameColumn(
    tableName = "WorkSpec",
    fromColumnName = "period_start_time",
    toColumnName = "last_enqueue_time"
)
class AutoMigration_14_15 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL(INITIALIZE_PERIOD_COUNTER)
        val values = ContentValues(1)
        values.put("last_enqueue_time", System.currentTimeMillis())
        db.update(
            "WorkSpec", OnConflictStrategy.ABORT, values,
            "last_enqueue_time = 0 AND interval_duration <> 0 ", emptyArray()
        )
    }
}
/**
 * A [WorkDatabase] migration that reschedules all eligible Workers.
 */
class RescheduleMigration(val mContext: Context, startVersion: Int, endVersion: Int) :
    Migration(startVersion, endVersion) {
    override fun migrate(database: SupportSQLiteDatabase) {
        if (endVersion >= VERSION_10) {
            database.execSQL(
                PreferenceUtils.INSERT_PREFERENCE,
                arrayOf<Any>(PreferenceUtils.KEY_RESCHEDULE_NEEDED, 1)
            )
        } else {
            val preferences = mContext.getSharedPreferences(
                PreferenceUtils.PREFERENCES_FILE_NAME,
                Context.MODE_PRIVATE
            )

            // Mutate the shared preferences directly, and eventually they will get
            // migrated to the data store post v10.
            preferences.edit()
                .putBoolean(PreferenceUtils.KEY_RESCHEDULE_NEEDED, true)
                .apply()
        }
    }
}

/**
 * Adds the [androidx.work.impl.model.Preference] table.
 */
internal class WorkMigration9To10(private val context: Context) : Migration(VERSION_9, VERSION_10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(PreferenceUtils.CREATE_PREFERENCE)
        PreferenceUtils.migrateLegacyPreferences(context, database)
        migrateLegacyIdGenerator(context, database)
    }
}