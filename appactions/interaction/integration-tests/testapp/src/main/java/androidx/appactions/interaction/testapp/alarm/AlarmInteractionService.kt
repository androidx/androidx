/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.testapp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appactions.builtintypes.types.Schedule
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.ExecutionCallback
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.productivity.CreateAlarm
import androidx.appactions.interaction.capabilities.productivity.DismissAlarm
import androidx.appactions.interaction.capabilities.productivity.SnoozeAlarm
import androidx.appactions.interaction.capabilities.productivity.UpdateAlarm
import androidx.appactions.interaction.service.AppInteractionService
import androidx.appactions.interaction.service.AppVerificationInfo
import androidx.appactions.interaction.testapp.Alarm.AlarmActivity
import java.util.Calendar

const val KEY_ALARM_SHARED_PREFERENCES = "alarm_shared_preferences"
const val KEY_ALARM_SCHEDULE = "alarm_schedule"
const val KEY_ALARM_IS_SET = "alarm_is_set"
const val KEY_ALARM_IS_SNOOZED = "alarm_is_snoozed"

class AlarmInteractionService : AppInteractionService() {

    private lateinit var mHandler: Handler
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var context: Context
    private lateinit var intent: Intent
    private lateinit var alarmIntent: PendingIntent
    private var schedule: Schedule? = null
    val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = applicationContext
            .getSharedPreferences(KEY_ALARM_SHARED_PREFERENCES, Context.MODE_PRIVATE)
        mHandler = Handler(Looper.myLooper()!!)
        context = applicationContext
        intent = Intent(context, AlarmActivity.AlarmReceiver::class.java)
        alarmIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    private fun showToast(msg: String) {
        mHandler.post(Runnable {
            Toast.makeText(
                this@AlarmInteractionService,
                msg,
                Toast.LENGTH_LONG
            ).show()
        })
    }

    private val createAlarmCapability =
        CreateAlarm.CapabilityBuilder()
            .setIdentifierProperty(Property(listOf(StringValue("create_alarm_oneshot"))))
            .setExecutionCallback(
                ExecutionCallback {
                    schedule = it.schedule
                    sharedPreferences.edit().putString(KEY_ALARM_SCHEDULE, schedule.toString())
                        .apply()
                    sharedPreferences.edit().putBoolean(KEY_ALARM_IS_SET, true).apply()
                    setAlarm()
                    ExecutionResult.Builder<CreateAlarm.Output>().build()
                }).build()

    fun setAlarm() {
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, schedule?.startTime?.asTime?.hour ?: 12)
            set(Calendar.MINUTE, schedule?.startTime?.asTime?.minute ?: 30)
        }
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            schedule?.repeatFrequency?.asDuration?.toMillis() ?: -1,
            alarmIntent,
        )
    }

    private val dismissAlarmCapability =
        DismissAlarm.CapabilityBuilder()
            .setExecutionCallback(
                ExecutionCallback {
                    sharedPreferences.edit().putBoolean(KEY_ALARM_IS_SET, false).apply()
                    alarmManager.cancel(alarmIntent)
                    ExecutionResult.Builder<DismissAlarm.Output>().build()
                }).build()

    private val snoozeAlarmCapability =
        SnoozeAlarm.CapabilityBuilder()
            .setExecutionCallback(
                ExecutionCallback {
                    sharedPreferences.edit().putBoolean(KEY_ALARM_IS_SNOOZED, true).apply()
                    ExecutionResult.Builder<SnoozeAlarm.Output>().build()
                }).build()

    private val updateAlarmCapability =
        UpdateAlarm.OverwriteAlarmSchedule.CapabilityBuilder()
            .setExecutionCallback(
                ExecutionCallback {
                    schedule = it.schedule
                    sharedPreferences.edit().putString(KEY_ALARM_SCHEDULE, schedule.toString())
                        .apply()
                    sharedPreferences.edit().putBoolean(KEY_ALARM_IS_SET, true).apply()
                    setAlarm()
                    ExecutionResult.Builder<UpdateAlarm.OverwriteAlarmSchedule.Output>().build()
                }).build()

    override val registeredCapabilities: List<Capability> = listOf(
        createAlarmCapability,
        dismissAlarmCapability,
        snoozeAlarmCapability,
        updateAlarmCapability,
    )

    override val allowedApps: List<AppVerificationInfo> = /* TODO */ listOf()
}
