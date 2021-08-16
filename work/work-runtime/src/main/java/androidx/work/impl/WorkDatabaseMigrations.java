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

package androidx.work.impl;

import static android.content.Context.MODE_PRIVATE;

import static androidx.work.impl.utils.PreferenceUtils.KEY_RESCHEDULE_NEEDED;
import static androidx.work.impl.utils.PreferenceUtils.PREFERENCES_FILE_NAME;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.work.impl.model.Preference;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkTypeConverters;
import androidx.work.impl.utils.IdGenerator;
import androidx.work.impl.utils.PreferenceUtils;

/**
 * Migration helpers for {@link androidx.work.impl.WorkDatabase}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkDatabaseMigrations {

    private WorkDatabaseMigrations() {
        // does nothing
    }

    // Known WorkDatabase versions
    public static final int VERSION_1 = 1;
    public static final int VERSION_2 = 2;
    public static final int VERSION_3 = 3;
    public static final int VERSION_4 = 4;
    public static final int VERSION_5 = 5;
    public static final int VERSION_6 = 6;
    public static final int VERSION_7 = 7;
    public static final int VERSION_8 = 8;
    public static final int VERSION_9 = 9;
    public static final int VERSION_10 = 10;
    public static final int VERSION_11 = 11;
    public static final int VERSION_12 = 12;

    private static final String CREATE_SYSTEM_ID_INFO =
            "CREATE TABLE IF NOT EXISTS `SystemIdInfo` (`work_spec_id` TEXT NOT NULL, `system_id`"
                    + " INTEGER NOT NULL, PRIMARY KEY(`work_spec_id`), FOREIGN KEY(`work_spec_id`)"
                    + " REFERENCES `WorkSpec`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )";

    private static final String MIGRATE_ALARM_INFO_TO_SYSTEM_ID_INFO =
            "INSERT INTO SystemIdInfo(work_spec_id, system_id) "
                    + "SELECT work_spec_id, alarm_id AS system_id FROM alarmInfo";

    private static final String PERIODIC_WORK_SET_SCHEDULE_REQUESTED_AT =
            "UPDATE workspec SET schedule_requested_at=0"
                    + " WHERE state NOT IN " + WorkTypeConverters.StateIds.COMPLETED_STATES
                    + " AND schedule_requested_at=" + WorkSpec.SCHEDULE_NOT_REQUESTED_YET
                    + " AND interval_duration<>0";

    private static final String REMOVE_ALARM_INFO = "DROP TABLE IF EXISTS alarmInfo";

    private static final String WORKSPEC_ADD_TRIGGER_UPDATE_DELAY =
            "ALTER TABLE workspec ADD COLUMN `trigger_content_update_delay` INTEGER NOT NULL "
                    + "DEFAULT -1";

    private static final String WORKSPEC_ADD_TRIGGER_MAX_CONTENT_DELAY =
            "ALTER TABLE workspec ADD COLUMN `trigger_max_content_delay` INTEGER NOT NULL DEFAULT"
                    + " -1";

    private static final String CREATE_WORK_PROGRESS =
            "CREATE TABLE IF NOT EXISTS `WorkProgress` (`work_spec_id` TEXT NOT NULL, `progress`"
                    + " BLOB NOT NULL, PRIMARY KEY(`work_spec_id`), FOREIGN KEY(`work_spec_id`) "
                    + "REFERENCES `WorkSpec`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )";

    private static final String CREATE_INDEX_PERIOD_START_TIME =
            "CREATE INDEX IF NOT EXISTS `index_WorkSpec_period_start_time` ON `workspec` "
                    + "(`period_start_time`)";

    private static final String CREATE_RUN_IN_FOREGROUND =
            "ALTER TABLE workspec ADD COLUMN `run_in_foreground` INTEGER NOT NULL DEFAULT 0";

    public static final String INSERT_PREFERENCE =
            "INSERT OR REPLACE INTO `Preference`"
                    + " (`key`, `long_value`) VALUES"
                    + " (@key, @long_value)";

    private static final String CREATE_PREFERENCE =
            "CREATE TABLE IF NOT EXISTS `Preference` (`key` TEXT NOT NULL, `long_value` INTEGER, "
                    + "PRIMARY KEY(`key`))";

    private static final String CREATE_OUT_OF_QUOTA_POLICY =
            "ALTER TABLE workspec ADD COLUMN `out_of_quota_policy` INTEGER NOT NULL DEFAULT 0";

    /**
     * Removes the {@code alarmInfo} table and substitutes it for a more general
     * {@code SystemIdInfo} table.
     * Adds implicit work tags for all work (a tag with the worker class name).
     */
    @NonNull
    public static Migration MIGRATION_1_2 = new Migration(VERSION_1, VERSION_2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(CREATE_SYSTEM_ID_INFO);
            database.execSQL(MIGRATE_ALARM_INFO_TO_SYSTEM_ID_INFO);
            database.execSQL(REMOVE_ALARM_INFO);
            database.execSQL("INSERT OR IGNORE INTO worktag(tag, work_spec_id) "
                    + "SELECT worker_class_name AS tag, id AS work_spec_id FROM workspec");
        }
    };

    /**
     * A {@link WorkDatabase} migration that reschedules all eligible Workers.
     */
    public static class RescheduleMigration extends Migration {
        final Context mContext;

        public RescheduleMigration(@NonNull Context context, int startVersion, int endVersion) {
            super(startVersion, endVersion);
            mContext = context;
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            if (endVersion >= VERSION_10) {
                database.execSQL(INSERT_PREFERENCE, new Object[]{KEY_RESCHEDULE_NEEDED, 1});
            } else {
                SharedPreferences preferences =
                        mContext.getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);

                // Mutate the shared preferences directly, and eventually they will get
                // migrated to the data store post v10.
                preferences.edit()
                        .putBoolean(KEY_RESCHEDULE_NEEDED, true)
                        .apply();
            }
        }
    }

    /**
     * Marks {@code SCHEDULE_REQUESTED_AT} to something other than
     * {@code SCHEDULE_NOT_REQUESTED_AT}.
     */
    @NonNull
    public static Migration MIGRATION_3_4 = new Migration(VERSION_3, VERSION_4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            if (Build.VERSION.SDK_INT >= WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL) {
                database.execSQL(PERIODIC_WORK_SET_SCHEDULE_REQUESTED_AT);
            }
        }
    };

    /**
     * Adds the {@code ContentUri} delays to the WorkSpec table.
     */
    @NonNull
    public static Migration MIGRATION_4_5 = new Migration(VERSION_4, VERSION_5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(WORKSPEC_ADD_TRIGGER_UPDATE_DELAY);
            database.execSQL(WORKSPEC_ADD_TRIGGER_MAX_CONTENT_DELAY);
        }
    };

    /**
     * Adds {@link androidx.work.impl.model.WorkProgress}.
     */
    @NonNull
    public static Migration MIGRATION_6_7 = new Migration(VERSION_6, VERSION_7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(CREATE_WORK_PROGRESS);
        }
    };

    /**
     * Adds an index on period_start_time in {@link WorkSpec}.
     */
    @NonNull
    public static Migration MIGRATION_7_8 = new Migration(VERSION_7, VERSION_8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(CREATE_INDEX_PERIOD_START_TIME);
        }
    };

    /**
     * Adds a notification_provider to the {@link WorkSpec}.
     */
    @NonNull
    public static Migration MIGRATION_8_9 = new Migration(VERSION_8, VERSION_9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(CREATE_RUN_IN_FOREGROUND);
        }
    };

    /**
     * Adds the {@link Preference} table.
     */
    public static class WorkMigration9To10 extends Migration {
        final Context mContext;

        public WorkMigration9To10(@NonNull Context context) {
            super(VERSION_9, VERSION_10);
            mContext = context;
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(CREATE_PREFERENCE);
            PreferenceUtils.migrateLegacyPreferences(mContext, database);
            IdGenerator.migrateLegacyIdGenerator(mContext, database);
        }
    }

    /**
     * Adds a notification_provider to the {@link WorkSpec}.
     */
    @NonNull
    public static Migration MIGRATION_11_12 = new Migration(VERSION_11, VERSION_12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(CREATE_OUT_OF_QUOTA_POLICY);
        }
    };
}
