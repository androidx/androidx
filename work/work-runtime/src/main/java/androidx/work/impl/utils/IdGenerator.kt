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
package androidx.work.impl.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.impl.WorkDatabase
import androidx.work.impl.model.Preference
import androidx.work.impl.utils.PreferenceUtils.INSERT_PREFERENCE
import java.util.concurrent.Callable

/**
 * Generates unique IDs that are persisted in [SharedPreferences].
 * @param workDatabase The [WorkDatabase] where metadata is persisted.
 */
class IdGenerator(private val workDatabase: WorkDatabase) {
    /**
     * Generates IDs for [android.app.job.JobInfo] jobs given a reserved range.
     */
    fun nextJobSchedulerIdWithRange(minInclusive: Int, maxInclusive: Int): Int {
        return workDatabase.runInTransaction(Callable {
            var id = workDatabase.nextId(NEXT_JOB_SCHEDULER_ID_KEY)
            if (id !in minInclusive..maxInclusive) {
                // outside the range, re-start at minInclusive.
                id = minInclusive
                workDatabase.updatePreference(NEXT_JOB_SCHEDULER_ID_KEY, id + 1)
            }
            id
        })
    }

    /**
     * Generates IDs for [android.app.AlarmManager] work.
     */
    fun nextAlarmManagerId(): Int {
        return workDatabase.runInTransaction(Callable {
            workDatabase.nextId(NEXT_ALARM_MANAGER_ID_KEY)
        })
    }
}

private fun WorkDatabase.nextId(key: String): Int {
    val value = preferenceDao().getLongValue(key)
    val id = value?.toInt() ?: INITIAL_ID
    val nextId = if (id == Int.MAX_VALUE) INITIAL_ID else id + 1
    updatePreference(key, nextId)
    return id
}

private fun WorkDatabase.updatePreference(key: String, value: Int) =
    this.preferenceDao().insertPreference(Preference(key, value.toLong()))

/** The initial id used for JobInfos and Alarms.  */
const val INITIAL_ID = 0
const val NEXT_JOB_SCHEDULER_ID_KEY = "next_job_scheduler_id"
const val NEXT_ALARM_MANAGER_ID_KEY = "next_alarm_manager_id"
const val PREFERENCE_FILE_KEY = "androidx.work.util.id"

/**
 * Migrates [IdGenerator] from [android.content.SharedPreferences] to the
 * [WorkDatabase].
 *
 * @param context The application [Context]
 */
internal fun migrateLegacyIdGenerator(
    context: Context,
    sqLiteDatabase: SupportSQLiteDatabase
) {
    val sharedPreferences = context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)

    // Check to see if we have not migrated already.
    if (sharedPreferences.contains(NEXT_JOB_SCHEDULER_ID_KEY) ||
        sharedPreferences.contains(NEXT_JOB_SCHEDULER_ID_KEY)
    ) {
        val nextJobId = sharedPreferences.getInt(NEXT_JOB_SCHEDULER_ID_KEY, INITIAL_ID)
        val nextAlarmId = sharedPreferences.getInt(NEXT_ALARM_MANAGER_ID_KEY, INITIAL_ID)
        sqLiteDatabase.beginTransaction()
        try {
            sqLiteDatabase.execSQL(INSERT_PREFERENCE, arrayOf(NEXT_JOB_SCHEDULER_ID_KEY, nextJobId))
            sqLiteDatabase.execSQL(
                INSERT_PREFERENCE, arrayOf(NEXT_ALARM_MANAGER_ID_KEY, nextAlarmId)
            )
            // Cleanup
            sharedPreferences.edit().clear().apply()
            sqLiteDatabase.setTransactionSuccessful()
        } finally {
            sqLiteDatabase.endTransaction()
        }
    }
}
