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

package android.arch.background.workmanager.impl.utils;

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
    static final String PREFERENCE_FILE_KEY = "android.arch.background.workmanager.util.id";
    static final String NEXT_JOB_SCHEDULER_ID_KEY = "next_job_scheduler_id";
    static final String NEXT_FIREBASE_ALARM_ID_KEY = "next_firebase_alarm_id";
    static final String NEXT_ALARM_MANAGER_ID_KEY = "next_alarm_manager_id";
    static final int INITIAL_ID = 0;

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
     * Generates IDs for {@link android.app.job.JobScheduler} jobs.
     */
    public int nextJobSchedulerId() {
        synchronized (IdGenerator.class) {
            return nextId(NEXT_JOB_SCHEDULER_ID_KEY);
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
        mSharedPrefs.edit().putInt(key, nextId).apply();
        return id;
    }
}
