/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.telecom.test

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build.VERSION_CODES
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.utils.EndpointUtils
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class EndpointUtilsTest {

    private val sessionId: Int = 123

    private fun createEndpoint(name: String, type: Int): CallEndpointCompat {
        return CallEndpointCompat(name, type, sessionId)
    }

    private class FakeContext(base: Context, private val hasPermission: Boolean) :
        ContextWrapper(base) {
        override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
            return if (hasPermission) PackageManager.PERMISSION_GRANTED
            else PackageManager.PERMISSION_DENIED
        }

        override fun checkSelfPermission(permission: String): Int {
            return if (hasPermission) PackageManager.PERMISSION_GRANTED
            else PackageManager.PERMISSION_DENIED
        }
    }

    @Test
    fun testIsNonWearableDeviceByHeuristic_emptyList_returnsFalse() {
        val endpoints = emptyList<CallEndpointCompat>()
        assertThat(EndpointUtils.isNonWearableDeviceByHeuristic(endpoints)).isFalse()
    }

    @Test
    fun testIsNonWearableDeviceByHeuristic_noBluetoothEndpoints_returnsFalse() {
        val endpoints =
            listOf(
                createEndpoint("Earpiece", CallEndpointCompat.TYPE_EARPIECE),
                createEndpoint("Speaker", CallEndpointCompat.TYPE_SPEAKER),
                createEndpoint("Wired Headset", CallEndpointCompat.TYPE_WIRED_HEADSET),
            )
        assertThat(EndpointUtils.isNonWearableDeviceByHeuristic(endpoints)).isFalse()
    }

    @Test
    fun testIsNonWearableDeviceByHeuristic_onlyWatchEndpoint_returnsFalse() {
        val endpoints = listOf(createEndpoint("Pixel Watch", CallEndpointCompat.TYPE_BLUETOOTH))
        assertThat(EndpointUtils.isNonWearableDeviceByHeuristic(endpoints)).isFalse()
    }

    @Test
    fun testIsNonWearableDeviceByHeuristic_watchAndSpeaker_returnsFalse() {
        val endpoints =
            listOf(
                createEndpoint("Pixel Watch", CallEndpointCompat.TYPE_BLUETOOTH),
                createEndpoint("Speaker", CallEndpointCompat.TYPE_SPEAKER),
            )
        assertThat(EndpointUtils.isNonWearableDeviceByHeuristic(endpoints)).isFalse()
    }

    @Test
    fun testIsNonWearableDeviceByHeuristic_onlyNonWatchBluetooth_returnsTrue() {
        val endpoints = listOf(createEndpoint("Pixel Buds Pro", CallEndpointCompat.TYPE_BLUETOOTH))
        assertThat(EndpointUtils.isNonWearableDeviceByHeuristic(endpoints)).isTrue()
    }

    @Test
    fun testIsNonWearableDeviceByHeuristic_watchAndNonWatchBluetooth_returnsTrue() {
        val endpoints =
            listOf(
                createEndpoint("Pixel Watch", CallEndpointCompat.TYPE_BLUETOOTH),
                createEndpoint("Sony Headphones", CallEndpointCompat.TYPE_BLUETOOTH),
            )
        assertThat(EndpointUtils.isNonWearableDeviceByHeuristic(endpoints)).isTrue()
    }

    @Test
    fun testIsNonWearableDeviceByHeuristic_multipleWatches_returnsFalse() {
        val endpoints =
            listOf(
                createEndpoint("Galaxy Watch 5", CallEndpointCompat.TYPE_BLUETOOTH),
                createEndpoint("Garmin Fenix", CallEndpointCompat.TYPE_BLUETOOTH),
            )
        assertThat(EndpointUtils.isNonWearableDeviceByHeuristic(endpoints)).isFalse()
    }

    @Test
    fun testIsNonWearableDeviceByHeuristic_caseInsensitiveMatch_returnsFalse() {
        val endpoints = listOf(createEndpoint("gAlAxY WaTcH", CallEndpointCompat.TYPE_BLUETOOTH))
        assertThat(EndpointUtils.isNonWearableDeviceByHeuristic(endpoints)).isFalse()
    }

    @Test
    fun testIsNonWearableDeviceByHeuristic_variousWatchKeywords_returnsFalse() {
        val watchNames =
            listOf(
                "My smartwatch",
                "Random wearABLE",
                "Suunto 9",
                "Fossil Gen 6",
                "TicWatch Pro",
                "Fitbit Versa 4",
                "Amazfit Bip",
                "Garmin Forerunner",
                "SmartBand 5",
            )
        for (name in watchNames) {
            val endpoints = listOf(createEndpoint(name, CallEndpointCompat.TYPE_BLUETOOTH))
            assertThat(EndpointUtils.isNonWearableDeviceByHeuristic(endpoints)).isFalse()
        }
    }

    @Test
    fun testIsNonWearableDeviceByHeuristic_similarNonWatchNames_returnsTrue() {
        // Names that don't contain any of the restricted keywords
        val nonWatchNames =
            listOf(
                "Galaxy Buds 2",
                "Sony WH-1000XM4",
                "Bose QuietComfort",
                "Jabra Elite",
                "AirPods Pro",
                "Car Audio",
            )
        for (name in nonWatchNames) {
            val endpoints = listOf(createEndpoint(name, CallEndpointCompat.TYPE_BLUETOOTH))
            assertThat(EndpointUtils.isNonWearableDeviceByHeuristic(endpoints)).isTrue()
        }
    }

    @Test
    fun testHasAvailableNonWearableDevice_permissionDenied_usesHeuristic_returnsFalse() {
        val context =
            FakeContext(ApplicationProvider.getApplicationContext(), hasPermission = false)
        val endpoints = listOf(createEndpoint("Pixel Watch", CallEndpointCompat.TYPE_BLUETOOTH))

        val result = EndpointUtils.hasAvailableNonWearableDevice(context, endpoints) { _ -> null }

        assertThat(result).isFalse() // Heuristic evaluates Pixel Watch to false
    }

    @Test
    fun testHasAvailableNonWearableDevice_permissionDenied_usesHeuristic_returnsTrue() {
        val context =
            FakeContext(ApplicationProvider.getApplicationContext(), hasPermission = false)
        val endpoints = listOf(createEndpoint("AirPods Pro", CallEndpointCompat.TYPE_BLUETOOTH))

        val result = EndpointUtils.hasAvailableNonWearableDevice(context, endpoints) { _ -> null }

        assertThat(result).isTrue() // Heuristic evaluates AirPods Pro to true
    }

    @Test
    fun testHasAvailableNonWearableDevice_securityException_usesHeuristic_returnsFalse() {
        val context = FakeContext(ApplicationProvider.getApplicationContext(), hasPermission = true)
        val endpoints = listOf(createEndpoint("Galaxy Watch 5", CallEndpointCompat.TYPE_BLUETOOTH))

        val result =
            EndpointUtils.hasAvailableNonWearableDevice(context, endpoints) { _ ->
                throw SecurityException("Mocked SecurityException")
            }

        assertThat(result).isFalse() // Heuristic evaluates Galaxy Watch to false
    }

    @Test
    fun testHasAvailableNonWearableDevice_securityException_usesHeuristic_returnsTrue() {
        val context = FakeContext(ApplicationProvider.getApplicationContext(), hasPermission = true)
        val endpoints = listOf(createEndpoint("Bose Headphones", CallEndpointCompat.TYPE_BLUETOOTH))

        val result =
            EndpointUtils.hasAvailableNonWearableDevice(context, endpoints) { _ ->
                throw SecurityException("Mocked SecurityException")
            }

        assertThat(result).isTrue() // Heuristic evaluates Bose Headphones to true
    }
}
