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

package androidx.work

import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiNetworkSpecifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ConstraintsTest {

    @SdkSuppress(minSdkVersion = 31)
    @Test
    fun testThrowIfNetworkSpecifierUsed() {
        val request =
            NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(
                    WifiNetworkSpecifier.Builder().setBand(ScanResult.WIFI_BAND_5_GHZ).build()
                )
                .build()

        val builder = Constraints.Builder()
        try {
            builder.setRequiredNetworkRequest(request, NetworkType.NOT_REQUIRED)
            throw AssertionError(
                "NetworkSpecifier aren't supported on purpose." +
                    "JobScheduler doesn't actually support them due to bugs, see b/293507207." +
                    "Additionally, they would significantly internal design of WM, " +
                    "because we can't properly persist NetworkSpecifiers, due to" +
                    "missing getters/setters on different API levels."
            )
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun testEqualityWithNetworkRequest() {
        val request1 =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

        val constraints1 =
            Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiredNetworkRequest(request1, NetworkType.UNMETERED)
                .build()

        val request2 =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

        val constraints2 =
            Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiredNetworkRequest(request2, NetworkType.UNMETERED)
                .build()

        val constraints3 = Constraints(requiresCharging = true)

        Truth.assertThat(constraints1).isEqualTo(constraints2)
        Truth.assertThat(constraints1.hashCode()).isEqualTo(constraints2.hashCode())
        Truth.assertThat(constraints1).isNotEqualTo(constraints3)
    }
}
