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

/**
 * Class holding source registration parameters.
 *
 * @param registrationUri URI that the Attribution Reporting API sends a request to in order to
 *   obtain source registration parameters.
 * @param debugKeyAllowed Used by the browser to indicate whether the debug key obtained from the
 *   registration URI is allowed to be used.
 */
class WebSourceParams public constructor(val registrationUri: Uri, val debugKeyAllowed: Boolean) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WebSourceParams) return false
        return this.registrationUri == other.registrationUri &&
            this.debugKeyAllowed == other.debugKeyAllowed
    }

    override fun hashCode(): Int {
        var hash = registrationUri.hashCode()
        hash = 31 * hash + debugKeyAllowed.hashCode()
        return hash
    }

    override fun toString(): String {
        return "WebSourceParams { RegistrationUri=$registrationUri, " +
            "DebugKeyAllowed=$debugKeyAllowed }"
    }

    internal companion object {
        @SuppressLint("ClassVerificationFailure", "NewApi")
        @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
        @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
        internal fun convertWebSourceParams(
            request: List<WebSourceParams>
        ): List<android.adservices.measurement.WebSourceParams> {
            var result = mutableListOf<android.adservices.measurement.WebSourceParams>()
            for (param in request) {
                result.add(
                    android.adservices.measurement.WebSourceParams.Builder(param.registrationUri)
                        .setDebugKeyAllowed(param.debugKeyAllowed)
                        .build()
                )
            }
            return result
        }
    }
}
