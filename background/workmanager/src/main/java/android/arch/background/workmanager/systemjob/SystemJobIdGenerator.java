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

package android.arch.background.workmanager.systemjob;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.RestrictTo;

/**
 * Generates IDs for {@link android.app.job.JobScheduler} jobs. The value for the next ID is
 * persisted in {@link SharedPreferences}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SystemJobIdGenerator {
    static final String PREFERENCE_FILE_KEY = "android.arch.background.workmanager.util.id";
    static final String NEXT_ID_KEY = "next_id";
    static final int INITIAL_ID = 0;

    private SharedPreferences mSharedPrefs;

    /**
     * Constructs a {@link SystemJobIdGenerator}.
     *
     * @param context {@link Context} to get the {@link SharedPreferences} from.
     */
    public SystemJobIdGenerator(Context context) {
        mSharedPrefs = context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
    }

    /**
     * Returns the next available ID based on a persisted counter.
     * Resets ID counter to 0 when the ID exceeded {@link Integer#MAX_VALUE}.
     *
     * @return The next available ID.
     */
    public synchronized int nextId() {
        int id = mSharedPrefs.getInt(NEXT_ID_KEY, INITIAL_ID);
        int nextId = (id == Integer.MAX_VALUE) ? INITIAL_ID : id + 1;
        mSharedPrefs.edit().putInt(NEXT_ID_KEY, nextId).apply();
        return id;
    }
}
