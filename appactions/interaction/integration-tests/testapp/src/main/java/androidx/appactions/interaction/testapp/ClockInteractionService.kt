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

package androidx.appactions.interaction.testapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.ExecutionCallback
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.productivity.PauseTimer
import androidx.appactions.interaction.capabilities.productivity.ResumeTimer
import androidx.appactions.interaction.capabilities.productivity.StartTimer
import androidx.appactions.interaction.capabilities.productivity.StopTimer
import androidx.appactions.interaction.service.AppInteractionService
import androidx.appactions.interaction.service.AppVerificationInfo
import java.time.Duration

const val KEY_TIMER_SHARED_PREFERENCES = "timer_shared_preferences"
const val KEY_TIMER_STARTED_AT = "timer_started_at"
const val KEY_TIMER_NAME = "timer_name"
const val KEY_TIMER_DURATION = "timer_duration"
const val KEY_TIMER_PAUSED_AT = "timer_paused_at"

class ClockInteractionService : AppInteractionService() {

    private lateinit var mHandler: Handler
    private var duration: Long = 0L
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreate() {
        super.onCreate()
        sharedPreferences = applicationContext
            .getSharedPreferences(KEY_TIMER_SHARED_PREFERENCES, Context.MODE_PRIVATE)
        mHandler = Handler(Looper.myLooper()!!)
    }

    private fun showToast(msg: String) {
        mHandler.post(Runnable {
            Toast.makeText(
                this@ClockInteractionService,
                msg,
                Toast.LENGTH_LONG
            ).show()
        })
    }

    private val startTimerCapability =
        StartTimer.CapabilityBuilder()
            .setId("start_timer_oneshot")
            .setDurationProperty(Property<Duration>(isRequiredForExecution = true))
            .setExecutionCallback(ExecutionCallback<StartTimer.Arguments, StartTimer.Output> {
                val result = ExecutionResult.Builder<StartTimer.Output>().build()
                val name = it.name ?: "Default title"
                val duration = it.duration!!
                if (sharedPreferences.getBoolean(KEY_IS_TIMER_RUNNING, false)) {
                    showToast(msg = "Could not start a timer because one is already running")
                    return@ExecutionCallback result
                }
                sharedPreferences.edit().putLong(KEY_TIMER_STARTED_AT, System.currentTimeMillis())
                sharedPreferences.edit().putBoolean(KEY_IS_TIMER_RUNNING, true)
                sharedPreferences.edit().putString(KEY_TIMER_NAME, name)
                sharedPreferences.edit().putLong(KEY_TIMER_DURATION, duration.toMillis())
                showToast(msg = "Timer Name:$name Duration:$duration has been started")

                result
            })
            .build()

    private val pauseTimerCapability =
        PauseTimer.CapabilityBuilder()
            .setId("pause_timer_oneshot")
            .setExecutionCallback(ExecutionCallback {
                sharedPreferences.edit().putLong(KEY_TIMER_PAUSED_AT, System.currentTimeMillis())
                ExecutionResult.Builder<PauseTimer.Output>().build()
            })
            .build()

    private val resumeTimerCapability =
        ResumeTimer.CapabilityBuilder()
            .setId("resume_timer_oneshot")
            .setExecutionCallback(ExecutionCallback {
                val pausedTime = sharedPreferences.getLong(KEY_TIMER_PAUSED_AT, 0L)
                val startedTime = sharedPreferences.getLong(KEY_TIMER_STARTED_AT, 0L)
                val elapsedTime = pausedTime - startedTime
                val duration = duration - elapsedTime
                sharedPreferences.edit().putLong(KEY_TIMER_STARTED_AT, System.currentTimeMillis())
                sharedPreferences.edit().putLong(KEY_TIMER_DURATION, duration)
                ExecutionResult.Builder<ResumeTimer.Output>().build()
            })
            .build()

    private val stopTimerCapability =
        StopTimer.CapabilityBuilder()
            .setId("resume_timer_oneshot")
            .setExecutionCallback(ExecutionCallback {
                sharedPreferences.edit().putBoolean(KEY_IS_TIMER_RUNNING, false)
                ExecutionResult.Builder<StopTimer.Output>().build()
            })
            .build()

    override val registeredCapabilities: List<Capability> = listOf(
        startTimerCapability,
        pauseTimerCapability,
        resumeTimerCapability,
        stopTimerCapability,
    )
    override val allowedApps: List<AppVerificationInfo> = listOf(
        AppVerificationInfo.Builder()
            .addSignature(hex2Byte(ASSISTANT_SIGNATURE))
            .setPackageName(ASSISTANT_PACKAGE)
            .build()
    )
}
