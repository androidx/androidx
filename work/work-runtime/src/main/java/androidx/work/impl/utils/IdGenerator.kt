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

package androidx.work.impl.utils;

import static android.content.Context.MODE_PRIVATE;

import static androidx.work.impl.utils.PreferenceUtils.INSERT_PREFERENCE;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.model.Preference;

/**
 * Generates unique IDs that are persisted in {@link SharedPreferences}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IdGenerator {

    /** The initial id used for JobInfos and Alarms. */
    public static final int INITIAL_ID = 0;
    public static final String PREFERENCE_FILE_KEY = "androidx.work.util.id";
    public static final String NEXT_JOB_SCHEDULER_ID_KEY = "next_job_scheduler_id";
    public static final String NEXT_ALARM_MANAGER_ID_KEY = "next_alarm_manager_id";

    private final WorkDatabase mWorkDatabase;

    /**
     * Constructs a {@link IdGenerator}.
     *
     * @param workDatabase The {@link WorkDatabase} where metadata is persisted.
     */
    public IdGenerator(@NonNull WorkDatabase workDatabase) {
        mWorkDatabase = workDatabase;
    }

    /**
     * Generates IDs for {@link android.app.job.JobInfo} jobs given a reserved range.
     */
    public int nextJobSchedulerIdWithRange(int minInclusive, int maxInclusive) {
        synchronized (IdGenerator.class) {
            int id = nextId(NEXT_JOB_SCHEDULER_ID_KEY);
            if (id < minInclusive || id > maxInclusive) {
                // outside the range, re-start at minInclusive.
                id = minInclusive;
                update(NEXT_JOB_SCHEDULER_ID_KEY, id + 1);
            }
            return id;
        }
    }

    /**
     * Generates IDs for {@link android.app.AlarmManager} work.
     */
    public int nextAlarmManagerId() {
        synchronized (IdGenerator.class) {
            return nextId(NEXT_ALARM_MANAGER_ID_KEY);
        }
    }

    /**
     * Returns the next available ID based on a persisted counter.
     * Resets ID counter to 0 when the ID exceeded {@link Integer#MAX_VALUE}.
     *
     * @param key String matching the particular counter to retrieve
     * @return next available id
     */
    private int nextId(String key) {
        mWorkDatabase.beginTransaction();
        try {
            Long value = mWorkDatabase.preferenceDao().getLongValue(key);
            int id = value != null ? value.intValue() : INITIAL_ID;
            int nextId = (id == Integer.MAX_VALUE) ? INITIAL_ID : id + 1;
            update(key, nextId);
            mWorkDatabase.setTransactionSuccessful();
            return id;
        } finally {
            mWorkDatabase.endTransaction();
        }

    }

    private void update(String key, int value) {
        mWorkDatabase.preferenceDao().insertPreference(new Preference(key, Long.valueOf(value)));
    }

    /**
     * Migrates {@link IdGenerator} from {@link android.content.SharedPreferences} to the
     * {@link WorkDatabase}.
     *
     * @param context The application {@link Context}
     */
    // TODO(b/141962522): Suppressed during upgrade to AGP 3.6.
    @SuppressWarnings("IdentityBinaryExpression")
    public static void migrateLegacyIdGenerator(
            @NonNull Context context,
            @NonNull SupportSQLiteDatabase sqLiteDatabase) {

        SharedPreferences sharedPreferences =
                context.getSharedPreferences(PREFERENCE_FILE_KEY, MODE_PRIVATE);

        // Check to see if we have not migrated already.
        if (sharedPreferences.contains(NEXT_JOB_SCHEDULER_ID_KEY)
                || sharedPreferences.contains(NEXT_JOB_SCHEDULER_ID_KEY)) {

            int nextJobId = sharedPreferences.getInt(NEXT_JOB_SCHEDULER_ID_KEY, INITIAL_ID);
            int nextAlarmId = sharedPreferences.getInt(NEXT_ALARM_MANAGER_ID_KEY, INITIAL_ID);

            sqLiteDatabase.beginTransaction();
            try {
                sqLiteDatabase.execSQL(INSERT_PREFERENCE,
                        new Object[]{NEXT_JOB_SCHEDULER_ID_KEY, nextJobId});

                sqLiteDatabase.execSQL(INSERT_PREFERENCE,
                        new Object[]{NEXT_ALARM_MANAGER_ID_KEY, nextAlarmId});
                // Cleanup
                sharedPreferences.edit()
                    .clear()
                    .apply();

                sqLiteDatabase.setTransactionSuccessful();
            } finally {
                sqLiteDatabase.endTransaction();
            }
        }

    }
}
