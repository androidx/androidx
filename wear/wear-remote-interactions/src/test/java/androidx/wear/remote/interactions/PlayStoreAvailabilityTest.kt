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

package androidx.wear.remote.interactions

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowPackageManager

@RunWith(WearRemoteInteractionsTestRunner::class)
@DoNotInstrument // Stop Robolectric instrumenting this class due to it being in package "android".
class PlayStoreAvailabilityTest {
    @Mock
    private var mockContentProvider: ContentProvider? = null
    private var contentResolver: ContentResolver? = null
    private var shadowPackageManager: ShadowPackageManager? = null

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        ShadowContentResolver.registerProviderInternal(
            PlayStoreAvailability.SETTINGS_AUTHORITY_URI,
            mockContentProvider
        )
        val context: Context = ApplicationProvider.getApplicationContext()
        contentResolver = context.contentResolver
        shadowPackageManager = Shadows.shadowOf(context.packageManager)
        shadowPackageManager?.setSystemFeature(RemoteInteractionsUtil.SYSTEM_FEATURE_WATCH, true)
    }

    @Test
    @Config(sdk = [25, 26, 27, 28])
    fun getPlayStoreAvailabilityOnPhone_returnsAvailable() {
        createFakePlayStoreAvailabilityQuery(PlayStoreAvailability.PLAY_STORE_AVAILABLE)
        assertEquals(
            PlayStoreAvailability.getPlayStoreAvailabilityOnPhone(
                ApplicationProvider.getApplicationContext()
            ),
            PlayStoreAvailability.PLAY_STORE_AVAILABLE
        )
    }

    @Test
    @Config(sdk = [25, 26, 27, 28])
    fun getPlayStoreAvailabilityOnPhone_returnsUnavailable() {
        createFakePlayStoreAvailabilityQuery(PlayStoreAvailability.PLAY_STORE_UNAVAILABLE)
        assertEquals(
            PlayStoreAvailability.getPlayStoreAvailabilityOnPhone(
                ApplicationProvider.getApplicationContext()
            ),
            PlayStoreAvailability.PLAY_STORE_UNAVAILABLE
        )
    }

    @Test
    @Config(sdk = [25, 26, 27, 28])
    fun getPlayStoreAvailabilityOnPhone_returnsError() {
        assertEquals(
            PlayStoreAvailability.getPlayStoreAvailabilityOnPhone(
                ApplicationProvider.getApplicationContext()
            ),
            PlayStoreAvailability.PLAY_STORE_ERROR_UNKNOWN
        )
    }

    /*
    TODO(b/178086256): Since Roboelectric doesn't support API 30 for now, add tests for it when it
     is supported.
    */

    companion object {
        private fun createFakePlayStoreAvailabilityCursor(availability: Int): Cursor {
            val cursor = MatrixCursor(arrayOf("key", "value"))
            cursor.addRow(
                arrayOf<Any>(PlayStoreAvailability.KEY_PLAY_STORE_AVAILABILITY, availability)
            )
            return cursor
        }
    }

    private fun createFakePlayStoreAvailabilityQuery(availability: Int) {
        Mockito.`when`(
            mockContentProvider!!.query(
                ArgumentMatchers.eq(PlayStoreAvailability.PLAY_STORE_AVAILABILITY_URI),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            )
        )
            .thenReturn(createFakePlayStoreAvailabilityCursor(availability))
    }
}
