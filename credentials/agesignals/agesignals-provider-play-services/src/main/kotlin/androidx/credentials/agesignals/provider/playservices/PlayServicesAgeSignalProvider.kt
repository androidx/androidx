/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.agesignals.provider.playservices

import android.content.Context
import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.agesignals.AgeSignalProvider
import androidx.credentials.agesignals.GetAgeRangeRequest
import androidx.credentials.agesignals.GetAgeRangeResponse
import androidx.credentials.agesignals.exceptions.GetAgeRangeException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import java.util.concurrent.Executor

/** Entry point of all age signals requests to the Google Play Services module. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PlayServicesAgeSignalProvider(private val context: Context) : AgeSignalProvider {

    @VisibleForTesting
    internal var googleApiAvailability: GoogleApiAvailability = GoogleApiAvailability.getInstance()

    override fun onGetAgeRange(
        context: Context,
        request: GetAgeRangeRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
    ) {
        // TODO(Wire GMS client call upon SDK release)
        throw UnsupportedOperationException("Not implemented")
    }

    override fun isAvailable(): Boolean {
        return isAvailableOnDevice(MIN_GMS_APK_VERSION)
    }

    // https://developers.google.com/android/reference/com/google/android/gms/common/ConnectionResult
    // There is one error code that supports retry API_DISABLED_FOR_CONNECTION but it would not
    // be useful to retry that one because our connection to GMSCore is a static variable
    // (see GoogleApiAvailability.getInstance()) so we cannot recreate the connection to retry.
    private fun isGooglePlayServicesAvailable(context: Context, minApkVersion: Int): Int {
        return googleApiAvailability.isGooglePlayServicesAvailable(
            context,
            /*minApkVersion=*/ minApkVersion,
        )
    }

    private fun isAvailableOnDevice(minApkVersion: Int): Boolean {
        val resultCode = isGooglePlayServicesAvailable(context, minApkVersion)
        return resultCode == ConnectionResult.SUCCESS
    }

    internal companion object {
        @VisibleForTesting
        internal const val MIN_GMS_APK_VERSION = 243100000 // TODO(Update with actual version)
    }
}
