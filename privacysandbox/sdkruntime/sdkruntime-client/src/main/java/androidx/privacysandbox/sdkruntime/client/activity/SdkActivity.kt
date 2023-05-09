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

package androidx.privacysandbox.sdkruntime.client.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat

/**
 * Activity to start for SDKs running locally.
 * Not for App / SDK Usage.
 *
 * SDK should use [SdkSandboxControllerCompat.registerSdkSandboxActivityHandler] for handler
 * registration.
 *
 * App should use [SdkSandboxManagerCompat.startSdkSandboxActivity] for starting activity.
 */
class SdkActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notifySdkOnActivityCreation()
    }

    private fun notifySdkOnActivityCreation() {
        val token = LocalSdkActivityStarter.getTokenFromSdkActivityStartIntent(intent)
        if (token == null) {
            Log.e(
                LOG_TAG,
                "Token is missing in starting SdkActivity intent params"
            )
            finish()
            return
        }

        try {
            val activityHolder = ComponentActivityHolder(this)
            LocalSdkActivityHandlerRegistry.notifyOnActivityCreation(token, activityHolder)
        } catch (e: Exception) {
            Log.e(
                LOG_TAG,
                "Failed to start the SdkActivity and going to finish it: ",
                e
            )
            finish()
        }
    }

    private companion object {
        private const val LOG_TAG = "SdkActivity"
    }
}