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

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.RestrictTo;

/**
 * Generates unique IDs that are persisted in {@link SharedPreferences}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IdGenerator {

    /** The initial id used for JobInfos, Firebase Jobs & and Alarms. */
    public static final int INITIAL_ID = 0;

    static final String PREFERENCE_FILE_KEY = "androidx.work.util.id";
    static final String NEXT_JOB_SCHEDULER_ID_KEY = "next_job_scheduler_id";
    static final String NEXT_FIREBASE_ALARM_ID_KEY = "next_firebase_alarm_id";
    static final String NEXT_ALARM_MANAGER_ID_KEY = "next_alarm_manager_id";

    private SharedPreferences mSharedPrefs;

    /**
     * Constructs a {@link IdGenerator}.
     *
     * @param context {@link Context} to get the {@link SharedPreferences} from.
     */
    public IdGenerator(Context context) {
        mSharedPrefs = context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
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
     * Generates IDs for Firebase Delayed Alarm Receiver jobs.
     */
    public int nextFirebaseAlarmId() {
        synchronized (IdGenerator.class) {
            return nextId(NEXT_FIREBASE_ALARM_ID_KEY);
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
     * @param key String matching the particular counter to retrieve
     * @return next available id
     */
    private int nextId(String key) {
        int id = mSharedPrefs.getInt(key, INITIAL_ID);
        int nextId = (id == Integer.MAX_VALUE) ? INITIAL_ID : id + 1;
        update(key, nextId);
        return id;
    }

    private void update(String key, int value) {
        mSharedPrefs.edit().putInt(key, value).apply();
    }
}
