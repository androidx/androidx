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

package androidx.core.telecom.test

import android.media.AudioDeviceInfo
import android.os.Build.VERSION_CODES
import android.telecom.CallAudioState
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallEndpointCompat.Companion.TYPE_BLUETOOTH
import androidx.core.telecom.CallEndpointCompat.Companion.TYPE_EARPIECE
import androidx.core.telecom.CallEndpointCompat.Companion.TYPE_SPEAKER
import androidx.core.telecom.CallEndpointCompat.Companion.TYPE_STREAMING
import androidx.core.telecom.CallEndpointCompat.Companion.TYPE_UNKNOWN
import androidx.core.telecom.CallEndpointCompat.Companion.TYPE_WIRED_HEADSET
import androidx.core.telecom.internal.CallEndpointUuidTracker
import androidx.core.telecom.internal.utils.EndpointUtils
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.remapAudioDeviceTypeToCallEndpointType
import androidx.core.telecom.test.utils.TestUtils
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@RequiresApi(VERSION_CODES.O)
class CallEndpointCompatTest {
    val mSessionId: Int = 222

    @After
    fun tearDown() {
        CallEndpointUuidTracker.endSession(mSessionId)
    }

    @Test
    fun testCallEndpointConstructor() {
        val name = "Endpoint"
        val type = TYPE_EARPIECE
        val identifier = TestUtils.generateRandomUuid()
        val endpoint = CallEndpointCompat(name, type, identifier)
        assertEquals(name, endpoint.name)
        assertEquals(type, endpoint.type)
        assertEquals(identifier, endpoint.identifier)
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @Test
    fun testWrappingAudioStateIntoAEndpoint() {
        val state = CallAudioState(false, CallAudioState.ROUTE_EARPIECE, 0)
        val endpoint = EndpointUtils.toCallEndpointCompat(state, mSessionId)
        assertEquals("EARPIECE", endpoint.name)
        assertEquals(TYPE_EARPIECE, endpoint.type)
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @Test
    fun testSupportedMask() {
        val supportedRouteMask =
            CallAudioState.ROUTE_EARPIECE or
                CallAudioState.ROUTE_SPEAKER or
                CallAudioState.ROUTE_WIRED_HEADSET
        val state = CallAudioState(false, CallAudioState.ROUTE_EARPIECE, supportedRouteMask)
        val endpoints = EndpointUtils.toCallEndpointsCompat(state, mSessionId)
        assertEquals(3, endpoints.size)
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @Test
    fun testCallAudioRouteToEndpointTypeMapping() {
        assertEquals(TYPE_EARPIECE, EndpointUtils.mapRouteToType(CallAudioState.ROUTE_EARPIECE))
        assertEquals(TYPE_SPEAKER, EndpointUtils.mapRouteToType(CallAudioState.ROUTE_SPEAKER))
        assertEquals(
            TYPE_WIRED_HEADSET,
            EndpointUtils.mapRouteToType(CallAudioState.ROUTE_WIRED_HEADSET)
        )
        assertEquals(TYPE_BLUETOOTH, EndpointUtils.mapRouteToType(CallAudioState.ROUTE_BLUETOOTH))
        assertEquals(TYPE_STREAMING, EndpointUtils.mapRouteToType(CallAudioState.ROUTE_STREAMING))
        assertEquals(TYPE_UNKNOWN, EndpointUtils.mapRouteToType(-1))
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @Test
    fun testTypeToRouteMapping() {
        assertEquals(CallAudioState.ROUTE_EARPIECE, EndpointUtils.mapTypeToRoute(TYPE_EARPIECE))
        assertEquals(CallAudioState.ROUTE_SPEAKER, EndpointUtils.mapTypeToRoute(TYPE_SPEAKER))
        assertEquals(CallAudioState.ROUTE_BLUETOOTH, EndpointUtils.mapTypeToRoute(TYPE_BLUETOOTH))
        assertEquals(
            CallAudioState.ROUTE_WIRED_HEADSET,
            EndpointUtils.mapTypeToRoute(TYPE_WIRED_HEADSET)
        )
        assertEquals(CallAudioState.ROUTE_STREAMING, EndpointUtils.mapTypeToRoute(TYPE_STREAMING))
        assertEquals(CallAudioState.ROUTE_EARPIECE, EndpointUtils.mapTypeToRoute(-1))
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @Test
    fun testAudioDeviceInfoTypeToCallEndpointTypeRemapping() {
        assertEquals(
            TYPE_EARPIECE,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
        )
        assertEquals(
            TYPE_SPEAKER,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
        )
        // Wired Headset Devices
        assertEquals(
            TYPE_WIRED_HEADSET,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_WIRED_HEADSET)
        )
        assertEquals(
            TYPE_WIRED_HEADSET,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
        )
        assertEquals(
            TYPE_WIRED_HEADSET,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_USB_DEVICE)
        )
        assertEquals(
            TYPE_WIRED_HEADSET,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_USB_ACCESSORY)
        )
        assertEquals(
            TYPE_WIRED_HEADSET,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_USB_HEADSET)
        )
        // Bluetooth Devices
        assertEquals(
            TYPE_BLUETOOTH,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        )
        assertEquals(
            TYPE_BLUETOOTH,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_HEARING_AID)
        )
        assertEquals(
            TYPE_BLUETOOTH,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_BLE_HEADSET)
        )
        assertEquals(
            TYPE_BLUETOOTH,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_BLE_SPEAKER)
        )
        assertEquals(
            TYPE_BLUETOOTH,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_BLE_BROADCAST)
        )
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @Test
    fun testDefaultSort() {
        val highestPriorityEndpoint = CallEndpointCompat("F", TYPE_WIRED_HEADSET, mSessionId)
        val second = CallEndpointCompat("E", TYPE_BLUETOOTH, mSessionId)
        val third = CallEndpointCompat("D", TYPE_SPEAKER, mSessionId)
        val fourth = CallEndpointCompat("C", TYPE_EARPIECE, mSessionId)
        val fifth = CallEndpointCompat("B", TYPE_STREAMING, mSessionId)
        val lowestPriorityEndpoint = CallEndpointCompat("A", TYPE_UNKNOWN, mSessionId)

        val endpoints =
            mutableListOf(
                lowestPriorityEndpoint,
                fourth,
                second,
                fifth,
                third,
                highestPriorityEndpoint
            )

        endpoints.sort()

        assertEquals(highestPriorityEndpoint, endpoints[0])
        assertEquals(second, endpoints[1])
        assertEquals(third, endpoints[2])
        assertEquals(fourth, endpoints[3])
        assertEquals(fifth, endpoints[4])
        assertEquals(lowestPriorityEndpoint, endpoints[5])
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @Test
    fun testDefaultSortWithDuplicateTypes() {
        val highestPriorityEndpoint = CallEndpointCompat("A", TYPE_BLUETOOTH, mSessionId)
        val second = CallEndpointCompat("B", TYPE_BLUETOOTH, mSessionId)
        val third = CallEndpointCompat("C", TYPE_BLUETOOTH, mSessionId)
        val fourth = CallEndpointCompat("D", TYPE_BLUETOOTH, mSessionId)
        val fifth = CallEndpointCompat("E", TYPE_BLUETOOTH, mSessionId)
        val lowestPriorityEndpoint = CallEndpointCompat("F", TYPE_BLUETOOTH, mSessionId)

        val endpoints =
            mutableListOf(
                lowestPriorityEndpoint,
                fourth,
                second,
                fifth,
                third,
                highestPriorityEndpoint
            )

        endpoints.sort()

        assertEquals(highestPriorityEndpoint, endpoints[0])
        assertEquals(second, endpoints[1])
        assertEquals(third, endpoints[2])
        assertEquals(fourth, endpoints[3])
        assertEquals(fifth, endpoints[4])
        assertEquals(lowestPriorityEndpoint, endpoints[5])
    }
}
