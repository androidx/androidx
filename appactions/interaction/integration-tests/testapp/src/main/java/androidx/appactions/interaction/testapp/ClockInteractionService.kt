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

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.ExecutionCallback
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.productivity.StartTimer
import androidx.appactions.interaction.service.AppInteractionService
import androidx.appactions.interaction.service.AppVerificationInfo
import java.time.Duration

class ClockInteractionService : AppInteractionService() {

    lateinit var mHandler: Handler
    override fun onCreate() {
        super.onCreate()
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

    private val capability =
        StartTimer.CapabilityBuilder()
            .setId("start_timer_oneshot")
            .setDurationProperty(Property<Duration>(isRequiredForExecution = true))
            .setExecutionCallback(ExecutionCallback<StartTimer.Arguments, StartTimer.Output> {
                val name = it.name ?: "Default title"
                val duration = it.duration!!

                // Do execution. ie. create the Timer called $name for $duration
                showToast(msg = "Name:$name Duration:$duration")

                ExecutionResult.Builder<StartTimer.Output>().build()
            })
            .build()

    override val registeredCapabilities: List<Capability> = listOf(capability)
    override val allowedApps: List<AppVerificationInfo> = listOf(
        AppVerificationInfo.Builder()
            .addSignature(hex2Byte(ASSISTANT_SIGNATURE))
            .setPackageName(ASSISTANT_PACKAGE)
            .build()
    )
}