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

package androidx.work.impl.utils;

import static android.content.Context.MODE_PRIVATE;

import static androidx.work.impl.WorkDatabaseMigrations.INSERT_PREFERENCE;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.model.Preference;

/**
 * Preference Utils for WorkManager.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PreferenceUtils {

    // For migration
    public static final String PREFERENCES_FILE_NAME = "androidx.work.util.preferences";
    public static final String KEY_LAST_CANCEL_ALL_TIME_MS = "last_cancel_all_time_ms";
    public static final String KEY_RESCHEDULE_NEEDED = "reschedule_needed";

    private final WorkDatabase mWorkDatabase;

    public PreferenceUtils(@NonNull WorkDatabase workDatabase) {
        mWorkDatabase = workDatabase;
    }

    /**
     * @return The last time (in milliseconds) a {@code cancelAll} method was called
     */
    public long getLastCancelAllTimeMillis() {
        Long value =
                mWorkDatabase.preferenceDao().getLongValue(KEY_LAST_CANCEL_ALL_TIME_MS);

        return value != null ? value : 0L;
    }

    /**
     * @return A {@link LiveData} of the last time (in milliseconds) a {@code cancelAll} method was
     *         called
     */
    @NonNull
    public LiveData<Long> getLastCancelAllTimeMillisLiveData() {
        LiveData<Long> observableValue =
                mWorkDatabase.preferenceDao().getObservableLongValue(KEY_LAST_CANCEL_ALL_TIME_MS);

        return Transformations.map(observableValue, new Function<Long, Long>() {
            @Override
            public Long apply(Long value) {
                return value != null ? value : 0L;
            }
        });
    }

    /**
     * Sets the last time a {@code cancelAll} method was called
     *
     * @param timeMillis The time a {@code cancelAll} method was called (in milliseconds)
     */
    public void setLastCancelAllTimeMillis(final long timeMillis) {
        Preference preference = new Preference(KEY_LAST_CANCEL_ALL_TIME_MS, timeMillis);
        mWorkDatabase.preferenceDao().insertPreference(preference);
    }

    /**
     * @return {@code true} When we should reschedule workers.
     */
    public boolean getNeedsReschedule() {
        // This preference is being set by a Room Migration.
        Long value = mWorkDatabase.preferenceDao().getLongValue(KEY_RESCHEDULE_NEEDED);
        return value != null && value == 1L;
    }

    /**
     * Updates the key which indicates that we have rescheduled jobs.
     */
    public void setNeedsReschedule(boolean needsReschedule) {
        Preference preference = new Preference(KEY_RESCHEDULE_NEEDED, needsReschedule);
        mWorkDatabase.preferenceDao().insertPreference(preference);
    }

    /**
     * Migrates preferences from {@link android.content.SharedPreferences} to the
     * {@link WorkDatabase}.
     *
     * @param context The application {@link Context}
     */
    public static void migrateLegacyPreferences(
            @NonNull Context context,
            @NonNull SupportSQLiteDatabase sqLiteDatabase) {

        SharedPreferences sharedPreferences =
                context.getSharedPreferences(PREFERENCES_FILE_NAME, MODE_PRIVATE);

        // Check to see if we have not migrated already.
        if (sharedPreferences.contains(KEY_RESCHEDULE_NEEDED)
                || sharedPreferences.contains(KEY_LAST_CANCEL_ALL_TIME_MS)) {

            long lastCancelTimeMillis = sharedPreferences.getLong(KEY_LAST_CANCEL_ALL_TIME_MS, 0L);
            boolean needsReschedule = sharedPreferences.getBoolean(KEY_RESCHEDULE_NEEDED, false);
            long reschedule = needsReschedule ? 1L : 0L;
            sqLiteDatabase.beginTransaction();
            try {

                sqLiteDatabase.execSQL(INSERT_PREFERENCE,
                        new Object[] {KEY_LAST_CANCEL_ALL_TIME_MS, lastCancelTimeMillis});

                sqLiteDatabase.execSQL(INSERT_PREFERENCE,
                        new Object[] {KEY_RESCHEDULE_NEEDED, reschedule});

                // Delete
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
