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

package androidx.credentials.provider

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.credentials.assertEquals
import androidx.credentials.provider.ui.UiUtils.Companion.constructCreateEntryWithSimpleParams
import androidx.credentials.provider.ui.UiUtils.Companion.constructRemoteEntryDefault
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class BeginCreateCredentialResponseTest {

    @Test
    fun constructor_success() {
        BeginCreateCredentialResponse(
            createEntries = listOf(constructCreateEntryWithSimpleParams("AccountName", "Desc")),
            remoteEntry = constructRemoteEntryDefault(),
        )
    }

    @Test
    fun constructor_createEntriesOnly() {
        BeginCreateCredentialResponse(
            createEntries = listOf(constructCreateEntryWithSimpleParams("AccountName", "Desc"))
        )
    }

    @Test
    fun constructor_remoteEntryOnly() {
        BeginCreateCredentialResponse(remoteEntry = constructRemoteEntryDefault())
    }

    @Test
    fun getter_createEntry() {
        val expectedAccountName = "AccountName"
        val expectedDescription = "Desc"
        val expectedSize = 1

        val beginCreateCredentialResponse =
            BeginCreateCredentialResponse(
                listOf(
                    constructCreateEntryWithSimpleParams(expectedAccountName, expectedDescription)
                ),
                null,
            )
        val actualAccountName = beginCreateCredentialResponse.createEntries[0].accountName
        val actualDescription = beginCreateCredentialResponse.createEntries[0].description

        assertThat(beginCreateCredentialResponse.createEntries.size).isEqualTo(expectedSize)
        assertThat(actualAccountName).isEqualTo(expectedAccountName)
        assertThat(actualDescription).isEqualTo(expectedDescription)
    }

    @Test
    fun getter_remoteEntry_null() {
        val expectedRemoteEntry: RemoteEntry? = null
        val beginCreateCredentialResponse =
            BeginCreateCredentialResponse(
                listOf(constructCreateEntryWithSimpleParams("AccountName", "Desc")),
                expectedRemoteEntry,
            )
        val actualRemoteEntry = beginCreateCredentialResponse.remoteEntry

        assertThat(actualRemoteEntry).isEqualTo(expectedRemoteEntry)
    }

    @Test
    fun getter_remoteEntry_nonNull() {
        val expectedRemoteEntry: RemoteEntry = constructRemoteEntryDefault()

        val beginCreateCredentialResponse =
            BeginCreateCredentialResponse(
                listOf(constructCreateEntryWithSimpleParams("AccountName", "Desc")),
                expectedRemoteEntry,
            )
        val actualRemoteEntry = beginCreateCredentialResponse.remoteEntry

        assertThat(actualRemoteEntry).isEqualTo(expectedRemoteEntry)
    }

    @Test
    fun bundleConversions_success() {
        val expected =
            BeginCreateCredentialResponse(
                createEntries = listOf(constructCreateEntryWithSimpleParams("AccountName", null)),
                remoteEntry = constructRemoteEntryDefault(),
            )

        val actual =
            BeginCreateCredentialResponse.fromBundle(
                BeginCreateCredentialResponse.asBundle(expected)
            )

        assertEquals(ApplicationProvider.getApplicationContext(), actual!!, expected)
    }

    @Test
    fun bundleConversions_multipleCreateEntries_success() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val expected =
            BeginCreateCredentialResponse(
                createEntries =
                    listOf(
                        CreateEntry(
                            "AccountName",
                            PendingIntent.getActivity(
                                context,
                                0,
                                Intent(),
                                PendingIntent.FLAG_IMMUTABLE,
                            ),
                            "Desc",
                            if (Build.VERSION.SDK_INT >= 26) Instant.now() else null,
                            ICON,
                            10,
                            20,
                            40,
                            true,
                        ),
                        CreateEntry(
                            "AccountName2",
                            PendingIntent.getActivity(
                                context,
                                0,
                                Intent(),
                                PendingIntent.FLAG_IMMUTABLE,
                            ),
                            "Desc...",
                            if (Build.VERSION.SDK_INT >= 26) Instant.now() else null,
                            ICON,
                            10,
                            null,
                            null,
                        ),
                    ),
                remoteEntry = null,
            )

        val actual =
            BeginCreateCredentialResponse.fromBundle(
                BeginCreateCredentialResponse.asBundle(expected)
            )

        assertEquals(context, actual!!, expected)
    }

    @Test
    fun bundleConversions_emptyBundle_returnsNull() {
        val actual = BeginCreateCredentialResponse.fromBundle(Bundle())

        assertThat(actual).isNull()
    }

    companion object {
        private val ICON =
            Icon.createWithBitmap(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
    }
}
