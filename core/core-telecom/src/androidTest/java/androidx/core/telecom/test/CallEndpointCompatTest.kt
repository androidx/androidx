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
import androidx.core.telecom.internal.utils.EndpointUtils
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.remapAudioDeviceTypeToCallEndpointType
import androidx.core.telecom.test.utils.TestUtils
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Test

@RequiresApi(VERSION_CODES.O)
class CallEndpointCompatTest {

    @Test
    fun testCallEndpointConstructor() {
        val name = "Endpoint"
        val type = CallEndpointCompat.TYPE_EARPIECE
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
        val endpoint = EndpointUtils.toCallEndpointCompat(state)
        assertEquals("EARPIECE", endpoint.name)
        assertEquals(CallEndpointCompat.TYPE_EARPIECE, endpoint.type)
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @Test
    fun testSupportedMask() {
        val supportedRouteMask =
            CallAudioState.ROUTE_EARPIECE or
                CallAudioState.ROUTE_SPEAKER or
                CallAudioState.ROUTE_WIRED_HEADSET
        val state = CallAudioState(false, CallAudioState.ROUTE_EARPIECE, supportedRouteMask)
        val endpoints = EndpointUtils.toCallEndpointsCompat(state)
        assertEquals(3, endpoints.size)
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @Test
    fun testCallAudioRouteToEndpointTypeMapping() {
        assertEquals(
            CallEndpointCompat.TYPE_EARPIECE,
            EndpointUtils.mapRouteToType(CallAudioState.ROUTE_EARPIECE)
        )
        assertEquals(
            CallEndpointCompat.TYPE_SPEAKER,
            EndpointUtils.mapRouteToType(CallAudioState.ROUTE_SPEAKER)
        )
        assertEquals(
            CallEndpointCompat.TYPE_WIRED_HEADSET,
            EndpointUtils.mapRouteToType(CallAudioState.ROUTE_WIRED_HEADSET)
        )
        assertEquals(
            CallEndpointCompat.TYPE_BLUETOOTH,
            EndpointUtils.mapRouteToType(CallAudioState.ROUTE_BLUETOOTH)
        )
        assertEquals(
            CallEndpointCompat.TYPE_STREAMING,
            EndpointUtils.mapRouteToType(CallAudioState.ROUTE_STREAMING)
        )
        assertEquals(CallEndpointCompat.TYPE_UNKNOWN, EndpointUtils.mapRouteToType(-1))
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @Test
    fun testTypeToRouteMapping() {
        assertEquals(
            CallAudioState.ROUTE_EARPIECE,
            EndpointUtils.mapTypeToRoute(CallEndpointCompat.TYPE_EARPIECE)
        )
        assertEquals(
            CallAudioState.ROUTE_SPEAKER,
            EndpointUtils.mapTypeToRoute(CallEndpointCompat.TYPE_SPEAKER)
        )
        assertEquals(
            CallAudioState.ROUTE_BLUETOOTH,
            EndpointUtils.mapTypeToRoute(CallEndpointCompat.TYPE_BLUETOOTH)
        )
        assertEquals(
            CallAudioState.ROUTE_WIRED_HEADSET,
            EndpointUtils.mapTypeToRoute(CallEndpointCompat.TYPE_WIRED_HEADSET)
        )
        assertEquals(
            CallAudioState.ROUTE_STREAMING,
            EndpointUtils.mapTypeToRoute(CallEndpointCompat.TYPE_STREAMING)
        )
        assertEquals(CallAudioState.ROUTE_EARPIECE, EndpointUtils.mapTypeToRoute(-1))
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @Test
    fun testAudioDeviceInfoTypeToCallEndpointTypeRemapping() {
        assertEquals(
            CallEndpointCompat.TYPE_EARPIECE,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
        )
        assertEquals(
            CallEndpointCompat.TYPE_SPEAKER,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
        )
        // Wired Headset Devices
        assertEquals(
            CallEndpointCompat.TYPE_WIRED_HEADSET,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_WIRED_HEADSET)
        )
        assertEquals(
            CallEndpointCompat.TYPE_WIRED_HEADSET,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
        )
        assertEquals(
            CallEndpointCompat.TYPE_WIRED_HEADSET,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_USB_DEVICE)
        )
        assertEquals(
            CallEndpointCompat.TYPE_WIRED_HEADSET,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_USB_ACCESSORY)
        )
        assertEquals(
            CallEndpointCompat.TYPE_WIRED_HEADSET,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_USB_HEADSET)
        )
        // Bluetooth Devices
        assertEquals(
            CallEndpointCompat.TYPE_BLUETOOTH,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        )
        assertEquals(
            CallEndpointCompat.TYPE_BLUETOOTH,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_HEARING_AID)
        )
        assertEquals(
            CallEndpointCompat.TYPE_BLUETOOTH,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_BLE_HEADSET)
        )
        assertEquals(
            CallEndpointCompat.TYPE_BLUETOOTH,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_BLE_SPEAKER)
        )
        assertEquals(
            CallEndpointCompat.TYPE_BLUETOOTH,
            remapAudioDeviceTypeToCallEndpointType(AudioDeviceInfo.TYPE_BLE_BROADCAST)
        )
    }

    @SdkSuppress(minSdkVersion = VERSION_CODES.O)
    @Test
    fun testDefaultSort() {
        val highestPriorityEndpoint = CallEndpointCompat("F", CallEndpointCompat.TYPE_WIRED_HEADSET)
        val second = CallEndpointCompat("E", CallEndpointCompat.TYPE_BLUETOOTH)
        val third = CallEndpointCompat("D", CallEndpointCompat.TYPE_SPEAKER)
        val fourth = CallEndpointCompat("C", CallEndpointCompat.TYPE_EARPIECE)
        val fifth = CallEndpointCompat("B", CallEndpointCompat.TYPE_STREAMING)
        val lowestPriorityEndpoint = CallEndpointCompat("A", CallEndpointCompat.TYPE_UNKNOWN)

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
        val highestPriorityEndpoint = CallEndpointCompat("A", CallEndpointCompat.TYPE_BLUETOOTH)
        val second = CallEndpointCompat("B", CallEndpointCompat.TYPE_BLUETOOTH)
        val third = CallEndpointCompat("C", CallEndpointCompat.TYPE_BLUETOOTH)
        val fourth = CallEndpointCompat("D", CallEndpointCompat.TYPE_BLUETOOTH)
        val fifth = CallEndpointCompat("E", CallEndpointCompat.TYPE_BLUETOOTH)
        val lowestPriorityEndpoint = CallEndpointCompat("F", CallEndpointCompat.TYPE_BLUETOOTH)

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
