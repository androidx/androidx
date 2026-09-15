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
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.agesignals.GetAgeRangeRequest
import androidx.credentials.agesignals.GetAgeRangeResponse
import androidx.credentials.agesignals.exceptions.GetAgeRangeException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PlayServicesAgeSignalProviderTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private lateinit var provider: PlayServicesAgeSignalProvider
    private lateinit var fakeGoogleApiAvailability: FakeGoogleApiAvailability

    private class FakeGoogleApiAvailability : GoogleApiAvailability() {
        var availabilityResult: Int = ConnectionResult.SUCCESS
        var capturedContext: Context? = null
        var capturedMinApkVersion: Int? = null

        override fun isGooglePlayServicesAvailable(context: Context, minApkVersion: Int): Int {
            capturedContext = context
            capturedMinApkVersion = minApkVersion
            return availabilityResult
        }
    }

    @Before
    fun setUp() {
        provider = PlayServicesAgeSignalProvider(context)
        fakeGoogleApiAvailability = FakeGoogleApiAvailability()
        provider.googleApiAvailability = fakeGoogleApiAvailability
    }

    @Test
    fun isAvailable_whenGooglePlayServicesSuccess_returnsTrue() {
        fakeGoogleApiAvailability.availabilityResult = ConnectionResult.SUCCESS

        assertThat(provider.isAvailable()).isTrue()
        assertThat(fakeGoogleApiAvailability.capturedMinApkVersion)
            .isEqualTo(PlayServicesAgeSignalProvider.MIN_GMS_APK_VERSION)
        assertThat(fakeGoogleApiAvailability.capturedContext).isSameInstanceAs(context)
    }

    @Test
    fun isAvailable_whenGooglePlayServicesNotSuccess_returnsFalse() {
        val failureCodes =
            listOf(
                ConnectionResult.SERVICE_MISSING,
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
                ConnectionResult.SERVICE_DISABLED,
                ConnectionResult.SERVICE_INVALID,
            )
        for (code in failureCodes) {
            fakeGoogleApiAvailability.availabilityResult = code

            assertThat(provider.isAvailable()).isFalse()
        }
    }

    @Test
    fun onGetAgeRange_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException::class.java) {
            provider.onGetAgeRange(
                context = context,
                request = GetAgeRangeRequest(),
                cancellationSignal = null,
                executor = { it.run() },
                callback =
                    object : OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException> {
                        override fun onResult(result: GetAgeRangeResponse) {}

                        override fun onError(error: GetAgeRangeException) {}
                    },
            )
        }
    }
}
