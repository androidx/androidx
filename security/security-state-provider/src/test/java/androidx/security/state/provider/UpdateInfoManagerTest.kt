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

package androidx.security.state.provider

import android.content.Context
import android.content.SharedPreferences
import androidx.security.state.SecurityPatchState
import androidx.security.state.SecurityPatchState.Companion.COMPONENT_SYSTEM
import androidx.security.state.SerializableUpdateInfo
import androidx.security.state.UpdateInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class UpdateInfoManagerTest {

    private lateinit var manager: UpdateInfoManager
    private val mockSecurityState: SecurityPatchState = mock<SecurityPatchState>()
    private val publishedDateMillis = 1L
    private val updateInfo =
        UpdateInfo.Builder()
            .setComponent(COMPONENT_SYSTEM)
            .setSecurityPatchLevel("2022-01-01")
            .setPublishedDateMillis(publishedDateMillis)
            .build()
    private val expectedJson =
        Json.encodeToString(
            SerializableUpdateInfo.serializer(),
            updateInfo.toSerializableUpdateInfo(),
        )
    private val mockEmptyEditor: SharedPreferences.Editor = mock<SharedPreferences.Editor> {}
    private val mockEditor: SharedPreferences.Editor =
        mock<SharedPreferences.Editor> {
            on { putString(Mockito.anyString(), Mockito.anyString()) } doReturn mockEmptyEditor
            on { remove(Mockito.anyString()) } doReturn mockEmptyEditor
        }

    private val mockMetadataEditor: SharedPreferences.Editor =
        mock<SharedPreferences.Editor> {
            on { putLong(Mockito.anyString(), Mockito.anyLong()) } doReturn mockEmptyEditor
        }

    private val mockPrefs: SharedPreferences =
        mock<SharedPreferences> {
            on { edit() } doReturn mockEditor
            on { all } doReturn mapOf(Pair("key", expectedJson))
        }

    private val mockMetadataPrefs: SharedPreferences =
        mock<SharedPreferences> {
            on { edit() } doReturn mockMetadataEditor
            on { getLong(Mockito.anyString(), Mockito.anyLong()) } doReturn 0L
        }

    private val mockContext: Context =
        mock<Context> {
            on { getSharedPreferences("UPDATE_INFO_PREFS", Context.MODE_PRIVATE) } doReturn
                mockPrefs
            on { getSharedPreferences(eq("UPDATE_INFO_METADATA_PREFS"), Mockito.anyInt()) } doReturn
                mockMetadataPrefs
        }

    @Before
    fun setUp() {
        manager = UpdateInfoManager(mockContext, mockSecurityState)
    }

    @Test
    fun testRegisterUnregisterUpdate() {
        `when`(mockSecurityState.getDeviceSecurityPatchLevel(COMPONENT_SYSTEM))
            .thenReturn(SecurityPatchState.DateBasedSecurityPatchLevel(2020, 1, 1))

        manager.registerUpdate(updateInfo)

        Mockito.verify(mockEditor).putString(Mockito.anyString(), Mockito.anyString())
        Mockito.verify(mockEditor, times(2)).apply()

        manager.unregisterUpdate(updateInfo)

        Mockito.verify(mockEditor).remove(Mockito.anyString())
    }

    @Test
    fun testCleanupUpdateInfo_removesUpdateInfoFromSharedPreferences() {
        `when`(mockSecurityState.getDeviceSecurityPatchLevel(COMPONENT_SYSTEM))
            .thenReturn(SecurityPatchState.DateBasedSecurityPatchLevel(2020, 1, 1))

        manager.registerUpdate(updateInfo)

        Mockito.verify(mockEditor).putString(Mockito.anyString(), Mockito.anyString())
        Mockito.verify(mockEditor, times(2)).apply()

        `when`(mockSecurityState.getDeviceSecurityPatchLevel(COMPONENT_SYSTEM))
            .thenReturn(SecurityPatchState.DateBasedSecurityPatchLevel(2023, 1, 1))

        manager.registerUpdate(updateInfo)

        Mockito.verify(mockEditor).remove(Mockito.anyString())
        Mockito.verify(mockEditor, times(4)).apply()
    }

    @Test
    fun testLastCheckTime_storesInSeparateFile() {
        // 1. Verify retrieval reads from metadata prefs
        assertEquals(0L, manager.getLastCheckTimeMillis())
        Mockito.verify(mockMetadataPrefs).getLong("last_check_time_millis", 0L)

        // 2. Verify setting writes to metadata editor
        val now = 1000L
        manager.setLastCheckTimeMillis(now)

        Mockito.verify(mockMetadataEditor).putLong("last_check_time_millis", now)
        Mockito.verify(mockMetadataEditor, times(1)).apply()

        // 3. Verify Isolation: Ensure we NEVER write metadata to the update list file
        Mockito.verify(mockEditor, Mockito.never()).putLong(Mockito.anyString(), Mockito.anyLong())
    }

    @Test
    fun testGetAllUpdates_retrievesAndDeserializesCorrectly() {
        `when`(mockPrefs.all).thenReturn(mapOf("key1" to expectedJson))

        val results = manager.getAllUpdates()

        assertEquals(1, results.size)
        assertEquals("SYSTEM", results[0].component)
        assertEquals("2022-01-01", results[0].securityPatchLevel)
        assertEquals(publishedDateMillis, results[0].publishedDateMillis)
    }
}
