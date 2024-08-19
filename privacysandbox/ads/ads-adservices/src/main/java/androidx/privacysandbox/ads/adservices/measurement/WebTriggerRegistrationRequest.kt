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

package androidx.privacysandbox.ads.adservices.measurement

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ads.adservices.measurement.WebTriggerParams.Companion.convertWebTriggerParams

/**
 * Class to hold input to measurement trigger registration calls from web context.
 *
 * @param webTriggerParams Registration info to fetch sources.
 * @param destination Destination [Uri].
 */
class WebTriggerRegistrationRequest
public constructor(val webTriggerParams: List<WebTriggerParams>, val destination: Uri) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebTriggerRegistrationRequest) return false
        return this.webTriggerParams == other.webTriggerParams &&
            this.destination == other.destination
    }

    override fun hashCode(): Int {
        var hash = webTriggerParams.hashCode()
        hash = 31 * hash + destination.hashCode()
        return hash
    }

    override fun toString(): String {
        return "WebTriggerRegistrationRequest { WebTriggerParams=$webTriggerParams, " +
            "Destination=$destination"
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertToAdServices():
        android.adservices.measurement.WebTriggerRegistrationRequest {
        return android.adservices.measurement.WebTriggerRegistrationRequest.Builder(
                convertWebTriggerParams(webTriggerParams),
                destination
            )
            .build()
    }
}
