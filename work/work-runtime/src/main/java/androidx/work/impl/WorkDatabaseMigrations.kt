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
import androidx.room.OnConflictStrategy
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.OverwritingInputMerger
import androidx.work.impl.WorkDatabaseVersions.VERSION_1
import androidx.work.impl.WorkDatabaseVersions.VERSION_10
import androidx.work.impl.WorkDatabaseVersions.VERSION_11
import androidx.work.impl.WorkDatabaseVersions.VERSION_12
import androidx.work.impl.WorkDatabaseVersions.VERSION_13
import androidx.work.impl.WorkDatabaseVersions.VERSION_15
import androidx.work.impl.WorkDatabaseVersions.VERSION_16
import androidx.work.impl.WorkDatabaseVersions.VERSION_17
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

/** Migration helpers for [androidx.work.impl.WorkDatabase]. */
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

    // adding generation column to WorkSpec and SystemIdInfo tables
    const val VERSION_16 = 16

    // made input_merger_class_name non null
    const val VERSION_17 = 17
    // next_schedule_time_override & next_schedule_time_override_generation were added
    @Suppress("unused") const val VERSION_18 = 18
    // stop_reason added
    const val VERSION_19 = 19
    // default value of last_enqueue_time changed to -1
    const val VERSION_20 = 20
    // added NetworkRequest to Constraints
    const val VERSION_21 = 21
    // need to reschedule jobs in workmanager's namespace,
    // but no actual schema changes.
    const val VERSION_22 = 22
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
 * Removes the `alarmInfo` table and substitutes it for a more general `SystemIdInfo` table. Adds
 * implicit work tags for all work (a tag with the worker class name).
 */
public object Migration_1_2 : Migration(VERSION_1, VERSION_2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_SYSTEM_ID_INFO)
        db.execSQL(MIGRATE_ALARM_INFO_TO_SYSTEM_ID_INFO)
        db.execSQL(REMOVE_ALARM_INFO)
        db.execSQL(
            """
                INSERT OR IGNORE INTO worktag(tag, work_spec_id)
                SELECT worker_class_name AS tag, id AS work_spec_id FROM workspec
                """
        )
    }
}

/** Marks `SCHEDULE_REQUESTED_AT` to something other than `SCHEDULE_NOT_REQUESTED_AT`. */
public object Migration_3_4 : Migration(VERSION_3, VERSION_4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(PERIODIC_WORK_SET_SCHEDULE_REQUESTED_AT)
    }
}

/** Adds the `ContentUri` delays to the WorkSpec table. */
public object Migration_4_5 : Migration(VERSION_4, VERSION_5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(WORKSPEC_ADD_TRIGGER_UPDATE_DELAY)
        db.execSQL(WORKSPEC_ADD_TRIGGER_MAX_CONTENT_DELAY)
    }
}

/** Adds [androidx.work.impl.model.WorkProgress]. */
public object Migration_6_7 : Migration(VERSION_6, VERSION_7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_WORK_PROGRESS)
    }
}

/** Adds an index on period_start_time in [WorkSpec]. */
public object Migration_7_8 : Migration(VERSION_7, VERSION_8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_INDEX_PERIOD_START_TIME)
    }
}

/** Adds a notification_provider to the [WorkSpec]. */
public object Migration_8_9 : Migration(VERSION_8, VERSION_9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_RUN_IN_FOREGROUND)
    }
}

/** Adds a notification_provider to the [WorkSpec]. */
public object Migration_11_12 : Migration(VERSION_11, VERSION_12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_OUT_OF_QUOTA_POLICY)
    }
}

public object Migration_12_13 : Migration(VERSION_12, VERSION_13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(SET_DEFAULT_NETWORK_TYPE)
        db.execSQL(SET_DEFAULT_CONTENT_URI_TRIGGERS)
    }
}

@RenameColumn(
    tableName = "WorkSpec",
    fromColumnName = "period_start_time",
    toColumnName = "last_enqueue_time",
)
public class AutoMigration_14_15 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL(INITIALIZE_PERIOD_COUNTER)
        val values = ContentValues(1)
        values.put("last_enqueue_time", System.currentTimeMillis())
        db.update(
            "WorkSpec",
            OnConflictStrategy.ABORT,
            values,
            "last_enqueue_time = 0 AND interval_duration <> 0 ",
            emptyArray(),
        )
    }
}

/** A [WorkDatabase] migration that reschedules all eligible Workers. */
public class RescheduleMigration(public val mContext: Context, startVersion: Int, endVersion: Int) :
    Migration(startVersion, endVersion) {
    override fun migrate(db: SupportSQLiteDatabase) {
        if (endVersion >= VERSION_10) {
            db.execSQL(
                PreferenceUtils.INSERT_PREFERENCE,
                arrayOf<Any>(PreferenceUtils.KEY_RESCHEDULE_NEEDED, 1),
            )
        } else {
            val preferences =
                mContext.getSharedPreferences(
                    PreferenceUtils.PREFERENCES_FILE_NAME,
                    Context.MODE_PRIVATE,
                )

            // Mutate the shared preferences directly, and eventually they will get
            // migrated to the data store post v10.
            preferences.edit().putBoolean(PreferenceUtils.KEY_RESCHEDULE_NEEDED, true).apply()
        }
    }
}

/** Adds the [androidx.work.impl.model.Preference] table. */
internal class WorkMigration9To10(private val context: Context) : Migration(VERSION_9, VERSION_10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(PreferenceUtils.CREATE_PREFERENCE)
        PreferenceUtils.migrateLegacyPreferences(context, db)
        migrateLegacyIdGenerator(context, db)
    }
}

public object Migration_15_16 : Migration(VERSION_15, VERSION_16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // b/239543214: unclear how data got corrupted,
        // but foreign key check on SystemIdInfo fails,
        // meaning SystemIdInfo has work_spec_id that doesn't exist in WorkSpec table.
        db.execSQL(
            "DELETE FROM SystemIdInfo WHERE work_spec_id IN " +
                "(SELECT work_spec_id FROM SystemIdInfo " +
                "LEFT JOIN WorkSpec ON work_spec_id = id WHERE WorkSpec.id IS NULL)"
        )

        db.execSQL("ALTER TABLE `WorkSpec` ADD COLUMN `generation` " + "INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `_new_SystemIdInfo` (
            `work_spec_id` TEXT NOT NULL, 
            `generation` INTEGER NOT NULL DEFAULT 0, 
            `system_id` INTEGER NOT NULL, 
            PRIMARY KEY(`work_spec_id`, `generation`), 
            FOREIGN KEY(`work_spec_id`) REFERENCES `WorkSpec`(`id`) 
                ON UPDATE CASCADE ON DELETE CASCADE )
               """
                .trimIndent()
        )
        db.execSQL(
            "INSERT INTO `_new_SystemIdInfo` (`work_spec_id`,`system_id`) " +
                "SELECT `work_spec_id`,`system_id` FROM `SystemIdInfo`"
        )
        db.execSQL("DROP TABLE `SystemIdInfo`")
        db.execSQL("ALTER TABLE `_new_SystemIdInfo` RENAME TO `SystemIdInfo`")
    }
}

public object Migration_16_17 : Migration(VERSION_16, VERSION_17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // b/261721822: unclear how the content of input_merger_class_name could have been,
        // null such that it fails to migrate to a table with a NOT NULL constrain, therefore
        // set the current default value to avoid dropping the worker.
        db.execSQL(
            """UPDATE WorkSpec
                SET input_merger_class_name = '${OverwritingInputMerger::class.java.name}'
                WHERE input_merger_class_name IS NULL
                """
                .trimIndent()
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `_new_WorkSpec` (
                `id` TEXT NOT NULL,
                `state` INTEGER NOT NULL,
                `worker_class_name` TEXT NOT NULL,
                `input_merger_class_name` TEXT NOT NULL,
                `input` BLOB NOT NULL,
                `output` BLOB NOT NULL,
                `initial_delay` INTEGER NOT NULL,
                `interval_duration` INTEGER NOT NULL,
                `flex_duration` INTEGER NOT NULL,
                `run_attempt_count` INTEGER NOT NULL,
                `backoff_policy` INTEGER NOT NULL,
                `backoff_delay_duration` INTEGER NOT NULL,
                `last_enqueue_time` INTEGER NOT NULL,
                `minimum_retention_duration` INTEGER NOT NULL,
                `schedule_requested_at` INTEGER NOT NULL,
                `run_in_foreground` INTEGER NOT NULL,
                `out_of_quota_policy` INTEGER NOT NULL,
                `period_count` INTEGER NOT NULL DEFAULT 0,
                `generation` INTEGER NOT NULL DEFAULT 0,
                `required_network_type` INTEGER NOT NULL,
                `requires_charging` INTEGER NOT NULL,
                `requires_device_idle` INTEGER NOT NULL,
                `requires_battery_not_low` INTEGER NOT NULL,
                `requires_storage_not_low` INTEGER NOT NULL,
                `trigger_content_update_delay` INTEGER NOT NULL,
                `trigger_max_content_delay` INTEGER NOT NULL,
                `content_uri_triggers` BLOB NOT NULL,
                PRIMARY KEY(`id`)
                )"""
                .trimIndent()
        )
        db.execSQL(
            """INSERT INTO `_new_WorkSpec` (
            `id`,
            `state`,
            `worker_class_name`,
            `input_merger_class_name`,
            `input`,
            `output`,
            `initial_delay`,
            `interval_duration`,
            `flex_duration`,
            `run_attempt_count`,
            `backoff_policy`,
            `backoff_delay_duration`,
            `last_enqueue_time`,
            `minimum_retention_duration`,
            `schedule_requested_at`,
            `run_in_foreground`,
            `out_of_quota_policy`,
            `period_count`,
            `generation`,
            `required_network_type`,
            `requires_charging`,
            `requires_device_idle`,
            `requires_battery_not_low`,
            `requires_storage_not_low`,
            `trigger_content_update_delay`,
            `trigger_max_content_delay`,
            `content_uri_triggers`
            ) SELECT
            `id`,
            `state`,
            `worker_class_name`,
            `input_merger_class_name`,
            `input`,
            `output`,
            `initial_delay`,
            `interval_duration`,
            `flex_duration`,
            `run_attempt_count`,
            `backoff_policy`,
            `backoff_delay_duration`,
            `last_enqueue_time`,
            `minimum_retention_duration`,
            `schedule_requested_at`,
            `run_in_foreground`,
            `out_of_quota_policy`,
            `period_count`,
            `generation`,
            `required_network_type`,
            `requires_charging`,
            `requires_device_idle`,
            `requires_battery_not_low`,
            `requires_storage_not_low`,
            `trigger_content_update_delay`,
            `trigger_max_content_delay`,
            `content_uri_triggers`
            FROM `WorkSpec`"""
                .trimIndent()
        )
        db.execSQL("DROP TABLE `WorkSpec`")
        db.execSQL("ALTER TABLE `_new_WorkSpec` RENAME TO `WorkSpec`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_WorkSpec_schedule_requested_at`" +
                "ON `WorkSpec` (`schedule_requested_at`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_WorkSpec_last_enqueue_time` ON" +
                "`WorkSpec` (`last_enqueue_time`)"
        )
    }
}

public class AutoMigration_19_20 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE WorkSpec SET `last_enqueue_time` = -1 WHERE `last_enqueue_time` = 0")
    }
}
