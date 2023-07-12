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
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.wear.phone.interactions.PhoneTypeHelper.Companion.getPhoneDeviceType
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.robolectric.annotation.Config
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

    @get:Rule
    val mocks = MockitoJUnit.rule()

    @Mock
    var mockContentProvider: ContentProvider? = null
    private var contentResolver: ContentResolver? = null

    @Before
    fun setUp() {
        ShadowContentResolver.registerProviderInternal(
            PhoneTypeHelper.SETTINGS_AUTHORITY,
            mockContentProvider
        )
        val context: Context = ApplicationProvider.getApplicationContext()
        contentResolver = context.contentResolver
    }

    @Test
    @Config(maxSdk = 28)
    fun testGetDeviceType_returnsIosWhenAltMode() {
        createFakePhoneTypeQuery(PhoneTypeHelper.IOS_MODE)
        assertThat(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext())
        ).isEqualTo(PhoneTypeHelper.DEVICE_TYPE_IOS)
    }

    @Test
    @Config(maxSdk = 28)
    fun testGetDeviceType_returnsAndroidWhenNonAltMode() {
        createFakePhoneTypeQuery(PhoneTypeHelper.ANDROID_MODE)
        assertThat(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext())
        ).isEqualTo(PhoneTypeHelper.DEVICE_TYPE_ANDROID)
    }

    @Test
    @Config(maxSdk = 28)
    fun testGetDeviceType_returnsErrorWhenModeUnknown() {
        createFakePhoneTypeQuery(PhoneTypeHelper.DEVICE_TYPE_UNKNOWN)
        assertThat(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext())
        ).isEqualTo(PhoneTypeHelper.DEVICE_TYPE_UNKNOWN)
    }

    @Test
    @Config(maxSdk = 28)
    fun testGetDeviceType_returnsErrorWhenContentMissing() {
        assertThat(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext())
        ).isEqualTo(PhoneTypeHelper.DEVICE_TYPE_ERROR)
    }

    @Test
    @Config(minSdk = 29)
    fun testGetDeviceType_returnsIosWhenAltMode_fromQ() {
        createFakePhoneTypeQuery(PhoneTypeHelper.IOS_MODE)
        Settings.Global.putInt(contentResolver, PhoneTypeHelper.PAIRED_DEVICE_OS_TYPE,
            PhoneTypeHelper.IOS_MODE)
        assertThat(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext())
        ).isEqualTo(PhoneTypeHelper.DEVICE_TYPE_IOS)
    }

    @Test
    @Config(minSdk = 29)
    fun testGetDeviceType_returnsAndroidWhenNonAltMode_fromQ() {
        Settings.Global.putInt(contentResolver, PhoneTypeHelper.PAIRED_DEVICE_OS_TYPE,
            PhoneTypeHelper.ANDROID_MODE)
        assertThat(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext())
        ).isEqualTo(PhoneTypeHelper.DEVICE_TYPE_ANDROID)
    }

    @Test
    @Config(minSdk = 29)
    fun testGetDeviceType_returnsErrorWhenContentMissing_fromQ() {
        assertThat(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext())
        ).isEqualTo(PhoneTypeHelper.DEVICE_TYPE_UNKNOWN)
    }

    @Test
    @Config(minSdk = 29)
    fun testGetDeviceType_returnsAndroid_fromQ() {
        Settings.Global.putInt(
            contentResolver,
            PhoneTypeHelper.PAIRED_DEVICE_OS_TYPE,
            PhoneTypeHelper.ANDROID_MODE
        )
        assertThat(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext())
        ).isEqualTo(PhoneTypeHelper.DEVICE_TYPE_ANDROID)
    }

    @Test
    @Config(minSdk = 29)
    fun testGetDeviceType_returnsErrorWhenModeUnknown_fromQ() {
        Settings.Global.putInt(
            contentResolver,
            PhoneTypeHelper.PAIRED_DEVICE_OS_TYPE,
            PhoneTypeHelper.UNKNOWN_MODE
        )
        assertThat(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext())
        ).isEqualTo(PhoneTypeHelper.DEVICE_TYPE_UNKNOWN)
    }

    @Test
    @Config(minSdk = 29)
    fun testGetDeviceType_returnsIos_fromQ() {
        Settings.Global.putInt(
            contentResolver,
            PhoneTypeHelper.PAIRED_DEVICE_OS_TYPE,
            PhoneTypeHelper.IOS_MODE
        )
        assertThat(
            getPhoneDeviceType(ApplicationProvider.getApplicationContext())
        ).isEqualTo(PhoneTypeHelper.DEVICE_TYPE_IOS)
    }

    companion object {
        private fun createFakeBluetoothModeCursor(bluetoothMode: Int): Cursor {
            val cursor = MatrixCursor(arrayOf("key", "value"))
            cursor.addRow(arrayOf<Any>(PhoneTypeHelper.BLUETOOTH_MODE, bluetoothMode))
            return cursor
        }
    }

    private fun createFakePhoneTypeQuery(phoneType: Int) {
        Mockito.`when`(
            mockContentProvider!!.query(
                ArgumentMatchers.eq(bluetoothModeUri),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            )
        )
            .thenReturn(createFakeBluetoothModeCursor(phoneType))
    }
}
