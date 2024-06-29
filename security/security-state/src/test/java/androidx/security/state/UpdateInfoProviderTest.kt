/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.security.state

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.security.state.SecurityPatchState.Companion.COMPONENT_SYSTEM
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.robolectric.shadows.ShadowContentResolver

@RunWith(AndroidJUnit4::class)
class UpdateInfoProviderTest {

    private lateinit var provider: UpdateInfoProvider
    private val mockSpl = SecurityPatchState.DateBasedSecurityPatchLevel(2022, 1, 1)
    private val mockSecurityState: SecurityPatchState =
        mock<SecurityPatchState> {
            on {
                getComponentSecurityPatchLevel(eq(COMPONENT_SYSTEM), Mockito.anyString())
            } doReturn mockSpl
        }
    private val authority = "com.example.provider"
    private val contentUri = Uri.parse("content://$authority/updateinfo")
    @SuppressLint("NewApi")
    private val publishedDate = Date.from(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC))
    private val updateInfo =
        UpdateInfo.Builder()
            .setUri("content://example.com/updateinfo")
            .setComponent(COMPONENT_SYSTEM)
            .setSecurityPatchLevel("2022-01-01")
            .setPublishedDate(publishedDate)
            .build()
    private val expectedJson = Gson().toJson(updateInfo)
    private val mockEmptyEditor: SharedPreferences.Editor = mock<SharedPreferences.Editor> {}
    private val mockEditor: SharedPreferences.Editor =
        mock<SharedPreferences.Editor> {
            on { putString(Mockito.anyString(), Mockito.anyString()) } doReturn mockEmptyEditor
            on { remove(Mockito.anyString()) } doReturn mockEmptyEditor
        }
    private val mockPrefs: SharedPreferences =
        mock<SharedPreferences> {
            on { edit() } doReturn mockEditor
            on { all } doReturn mapOf(Pair("key", Gson().toJson(updateInfo)))
        }
    private val mockContext: Context =
        mock<Context> {
            on { getSharedPreferences("UpdateInfoPrefs", Context.MODE_PRIVATE) } doReturn mockPrefs
        }

    @Before
    fun setUp() {
        provider = UpdateInfoProvider(mockContext, authority, mockSecurityState)
        assertTrue(provider.onCreate())
        ShadowContentResolver.registerProviderInternal(authority, provider)
    }

    @Test
    fun query_WithCorrectUri_ReturnsExpectedCursor() {
        val cursor = provider.query(contentUri, null, null, null, null)

        assertNotNull(cursor)
        assertTrue(cursor.moveToFirst())
        assertEquals(expectedJson, cursor.getString(cursor.getColumnIndex("json")))
        cursor.close()
    }

    @Test(expected = IllegalArgumentException::class)
    fun query_WithIncorrectUri_ThrowsException() {
        provider.query(Uri.parse("content://$authority/invalid"), null, null, null, null)
    }

    @Test
    fun getType_CorrectUri_ReturnsCorrectType() {
        val type = provider.getType(contentUri)

        assertEquals("vnd.android.cursor.dir/vnd.$authority.updateinfo", type)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun insert_NotSupported_ThrowsException() {
        provider.insert(contentUri, ContentValues())
    }

    @Test(expected = UnsupportedOperationException::class)
    fun delete_NotSupported_ThrowsException() {
        provider.delete(contentUri, null, null)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun update_NotSupported_ThrowsException() {
        provider.update(contentUri, ContentValues(), null, null)
    }

    @Test
    fun testRegisterUnregisterUpdate() {
        `when`(mockSecurityState.getDeviceSecurityPatchLevel(COMPONENT_SYSTEM))
            .thenReturn(SecurityPatchState.DateBasedSecurityPatchLevel(2020, 1, 1))

        provider.registerUpdate(updateInfo)

        Mockito.verify(mockEditor).putString(Mockito.anyString(), Mockito.anyString())
        Mockito.verify(mockEditor, times(2)).apply()

        provider.unregisterUpdate(updateInfo)

        Mockito.verify(mockEditor).remove(Mockito.anyString())
    }

    @Test
    fun testCleanupUpdateInfo_removesUpdateInfoFromSharedPreferences() {
        `when`(mockSecurityState.getDeviceSecurityPatchLevel(COMPONENT_SYSTEM))
            .thenReturn(SecurityPatchState.DateBasedSecurityPatchLevel(2020, 1, 1))

        provider.registerUpdate(updateInfo)

        Mockito.verify(mockEditor).putString(Mockito.anyString(), Mockito.anyString())
        Mockito.verify(mockEditor, times(2)).apply()

        `when`(mockSecurityState.getDeviceSecurityPatchLevel(COMPONENT_SYSTEM))
            .thenReturn(SecurityPatchState.DateBasedSecurityPatchLevel(2023, 1, 1))

        provider.registerUpdate(updateInfo)

        Mockito.verify(mockEditor).remove(Mockito.anyString())
        Mockito.verify(mockEditor, times(4)).apply()
    }
}
