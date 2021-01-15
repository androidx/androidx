/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.phone.interactions

import android.content.ContentProvider
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.wear.phone.interactions.PhoneTypeHelper.Companion.getPhoneDeviceType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowContentResolver

@RunWith(WearPhoneInteractionsTestRunner::class)
@DoNotInstrument // Stop Robolectric instrumenting this class due to it being in package "android".
class PhoneTypeHelperTest {
    private val bluetoothModeUri = Uri.Builder()
        .scheme("content")
        .authority(PhoneTypeHelper.SETTINGS_AUTHORITY)
        .path(PhoneTypeHelper.BLUETOOTH_MODE)
        .build()

    @Mock
    var mockContentProvider: ContentProvider? = null
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        ShadowContentResolver.registerProviderInternal(
            PhoneTypeHelper.SETTINGS_AUTHORITY,
            mockContentProvider
        )
    }

    @Test
    fun testGetDeviceType_returnsIosWhenAltMode() {
        Mockito.`when`(
            mockContentProvider!!.query(
                ArgumentMatchers.eq(bluetoothModeUri),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            )
        )
            .thenReturn(createFakeBluetoothModeCursor(PhoneTypeHelper.IOS_BLUETOOTH_MODE))
        Assert.assertEquals(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext()).toLong(),
            PhoneTypeHelper.DEVICE_TYPE_IOS.toLong()
        )
    }

    @Test
    fun testGetDeviceType_returnsAndroidWhenNonAltMode() {
        Mockito.`when`(
            mockContentProvider!!.query(
                ArgumentMatchers.eq(bluetoothModeUri),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            )
        )
            .thenReturn(createFakeBluetoothModeCursor(PhoneTypeHelper.ANDROID_BLUETOOTH_MODE))
        Assert.assertEquals(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext()).toLong(),
            PhoneTypeHelper.DEVICE_TYPE_ANDROID.toLong()
        )
    }

    @Test
    fun testGetDeviceType_returnsErrorWhenModeUnknown() {
        Mockito.`when`(
            mockContentProvider!!.query(
                ArgumentMatchers.eq(bluetoothModeUri),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            )
        )
            .thenReturn(createFakeBluetoothModeCursor(PhoneTypeHelper.DEVICE_TYPE_UNKNOWN))
        Assert.assertEquals(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext()).toLong(),
            PhoneTypeHelper.DEVICE_TYPE_UNKNOWN.toLong()
        )
    }

    @Test
    fun testGetDeviceType_returnsErrorWhenContentMissing() {
        Assert.assertEquals(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext()).toLong(),
            PhoneTypeHelper.DEVICE_TYPE_ERROR.toLong()
        )
    }

    companion object {
        private fun createFakeBluetoothModeCursor(bluetoothMode: Int): Cursor {
            val cursor = MatrixCursor(arrayOf("key", "value"))
            cursor.addRow(arrayOf<Any>(PhoneTypeHelper.BLUETOOTH_MODE, bluetoothMode))
            return cursor
        }
    }
}