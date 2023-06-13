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

import androidx.core.os.BuildCompat
import androidx.credentials.provider.ui.UiUtils.Companion.constructCreateEntryWithSimpleParams
import androidx.credentials.provider.ui.UiUtils.Companion.constructRemoteEntryDefault
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
@RunWith(AndroidJUnit4::class)
@SmallTest
class BeginCreateCredentialResponseTest {

    @Test
    fun constructor_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }

        BeginCreateCredentialResponse(
            createEntries = listOf(
                constructCreateEntryWithSimpleParams(
                    "AccountName",
                    "Desc"
                )
            ),
            remoteEntry = constructRemoteEntryDefault()
        )
    }

    @Test
    fun constructor_createEntriesOnly() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }

        BeginCreateCredentialResponse(
            createEntries = listOf(
                constructCreateEntryWithSimpleParams(
                    "AccountName",
                    "Desc"
                )
            )
        )
    }

    @Test
    fun constructor_remoteEntryOnly() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }

        BeginCreateCredentialResponse(
            remoteEntry = constructRemoteEntryDefault()
        )
    }

    @Test
    fun getter_createEntry() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val expectedAccountName = "AccountName"
        val expectedDescription = "Desc"
        val expectedSize = 1

        val beginCreateCredentialResponse = BeginCreateCredentialResponse(
            listOf(
                constructCreateEntryWithSimpleParams(
                    expectedAccountName,
                    expectedDescription
                )
            ), null
        )
        val actualAccountName = beginCreateCredentialResponse.createEntries[0].accountName
        val actualDescription = beginCreateCredentialResponse.createEntries[0].description

        assertThat(beginCreateCredentialResponse.createEntries.size).isEqualTo(expectedSize)
        assertThat(actualAccountName).isEqualTo(expectedAccountName)
        assertThat(actualDescription).isEqualTo(expectedDescription)
    }

    @Test
    fun getter_remoteEntry_null() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }

        val expectedRemoteEntry: RemoteEntry? = null
        val beginCreateCredentialResponse = BeginCreateCredentialResponse(
            listOf(
                constructCreateEntryWithSimpleParams(
                    "AccountName",
                    "Desc"
                )
            ),
            expectedRemoteEntry
        )
        val actualRemoteEntry = beginCreateCredentialResponse.remoteEntry

        assertThat(actualRemoteEntry).isEqualTo(expectedRemoteEntry)
    }

    @Test
    fun getter_remoteEntry_nonNull() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val expectedRemoteEntry: RemoteEntry = constructRemoteEntryDefault()

        val beginCreateCredentialResponse = BeginCreateCredentialResponse(
            listOf(
                constructCreateEntryWithSimpleParams(
                    "AccountName",
                    "Desc"
                )
            ),
            expectedRemoteEntry
        )
        val actualRemoteEntry = beginCreateCredentialResponse.remoteEntry

        assertThat(actualRemoteEntry).isEqualTo(expectedRemoteEntry)
    }
}