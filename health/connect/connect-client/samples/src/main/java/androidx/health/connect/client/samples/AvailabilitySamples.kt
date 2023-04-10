/*
 * Copyright 2022 The Android Open Source Project
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

@file:Suppress("UNUSED_VARIABLE")

package androidx.health.connect.client.samples

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.Sampled
import androidx.health.connect.client.HealthConnectClient

@Sampled
suspend fun AvailabilityCheckSamples(context: Context, providerPackageName: String) {
    val availabilityStatus = HealthConnectClient.getSdkStatus(context, providerPackageName)
    if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
        return // early return as there is no viable integration
    }
    if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
        // Optionally redirect to package installer to find a provider, for example:
        val uriString =
            "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.android.vending")
                data = Uri.parse(uriString)
                putExtra("overlay", true)
                putExtra("callerId", context.packageName)
            }
        )
        return
    }
    val healthConnectClient = HealthConnectClient.getOrCreate(context)
    // Issue operations with healthConnectClient
}
