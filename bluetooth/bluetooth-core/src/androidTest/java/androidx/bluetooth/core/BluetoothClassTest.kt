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

package androidx.bluetooth.core

import android.os.Bundle
import android.os.Parcel
import android.bluetooth.BluetoothClass as FwkBluetoothClass
import androidx.test.filters.SmallTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BluetoothClassTest {

    private val mBluetoothClassHeadphones: BluetoothClass =
        createBluetoothClass(BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES)
    private val mBluetoothClassPhone: BluetoothClass =
        createBluetoothClass(BluetoothClass.Device.Major.PHONE)
    private val mBluetoothClassService: BluetoothClass =
        createBluetoothClass(BluetoothClass.Service.NETWORKING)

    private fun createBluetoothClass(deviceClass: Int): BluetoothClass {
        // create FwkBluetoothClass from parcel since there is no constructor
        val p = Parcel.obtain()
        p.writeInt(deviceClass)
        p.setDataPosition(0)
        val fwkBluetoothClass: FwkBluetoothClass = FwkBluetoothClass.CREATOR.createFromParcel(p)
        p.recycle()

        val bundle = Bundle()
        bundle.putParcelable(
            BluetoothClass.keyForField(BluetoothClass.FIELD_FWK_BLUETOOTH_CLASS),
            fwkBluetoothClass
        )
        return BluetoothClass.CREATOR.fromBundle(bundle)
    }

    @SmallTest
    @Test
    fun hasService() {
        assertTrue(mBluetoothClassService.hasService(BluetoothClass.Service.NETWORKING))
        assertFalse(mBluetoothClassService.hasService(BluetoothClass.Service.TELEPHONY))
    }

    @SmallTest
    @Test
    fun getMajorDeviceClass() {
        assertEquals(
            mBluetoothClassHeadphones.majorDeviceClass,
            BluetoothClass.Device.Major.AUDIO_VIDEO
        )
        assertEquals(mBluetoothClassPhone.majorDeviceClass, BluetoothClass.Device.Major.PHONE)
    }

    @SmallTest
    @Test
    fun getDeviceClass() {
        assertEquals(
            mBluetoothClassHeadphones.deviceClass,
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        )
        assertEquals(
            mBluetoothClassPhone.deviceClass,
            BluetoothClass.Device.PHONE_UNCATEGORIZED
        )
    }

    @SmallTest
    @Test
    fun getClassOfDevice() {
        assertEquals(
            mBluetoothClassHeadphones.deviceClass,
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        )
        assertEquals(mBluetoothClassPhone.majorDeviceClass, BluetoothClass.Device.Major.PHONE)
    }

    @SmallTest
    @Test
    fun doesClassMatch() {
        assertTrue(mBluetoothClassHeadphones.doesClassMatch(BluetoothClass.PROFILE_A2DP))
        assertFalse(mBluetoothClassHeadphones.doesClassMatch(BluetoothClass.PROFILE_HEADSET))
        assertTrue(mBluetoothClassPhone.doesClassMatch(BluetoothClass.PROFILE_OPP))
        assertFalse(mBluetoothClassPhone.doesClassMatch(BluetoothClass.PROFILE_HEADSET))
        assertTrue(mBluetoothClassService.doesClassMatch(BluetoothClass.PROFILE_PANU))
        assertFalse(mBluetoothClassService.doesClassMatch(BluetoothClass.PROFILE_OPP))
    }
}