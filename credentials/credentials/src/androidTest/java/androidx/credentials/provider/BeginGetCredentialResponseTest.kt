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

import androidx.credentials.PasswordCredential
import androidx.credentials.provider.ui.UiUtils.Companion.constructActionEntry
import androidx.credentials.provider.ui.UiUtils.Companion.constructAuthenticationActionEntry
import androidx.credentials.provider.ui.UiUtils.Companion.constructPasswordCredentialEntryDefault
import androidx.credentials.provider.ui.UiUtils.Companion.constructRemoteEntryDefault
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4::class)
@SmallTest
class BeginGetCredentialResponseTest {

    @Test
    fun constructor_success() {
        BeginGetCredentialResponse()
    }

    @Test
    fun buildConstruct_success() {
        BeginGetCredentialResponse.Builder().build()
    }

    @Test
    fun getter_credentialEntries() {
        val expectedSize = 1
        val expectedType = PasswordCredential.TYPE_PASSWORD_CREDENTIAL
        val expectedUsername = "f35"

        val response = BeginGetCredentialResponse(
            listOf(
                constructPasswordCredentialEntryDefault(
                    expectedUsername
                )
            )
        )
        val actualSize = response.credentialEntries.size
        val actualType = response.credentialEntries[0].type
        val actualUsername = (response.credentialEntries[0] as PasswordCredentialEntry)
            .username.toString()

        assertThat(actualSize).isEqualTo(expectedSize)
        assertThat(actualType).isEqualTo(expectedType)
        assertThat(actualUsername).isEqualTo(expectedUsername)
    }

    @Test
    fun getter_actionEntries() {
        val expectedSize = 1
        val expectedTitle = "boeing"
        val expectedSubtitle = "737max"

        val response = BeginGetCredentialResponse(
            emptyList(),
            listOf(constructActionEntry(expectedTitle, expectedSubtitle))
        )
        val actualSize = response.actions.size
        val actualTitle = response.actions[0].title.toString()
        val actualSubtitle = response.actions[0].subtitle.toString()

        assertThat(actualSize).isEqualTo(expectedSize)
        assertThat(actualTitle).isEqualTo(expectedTitle)
        assertThat(actualSubtitle).isEqualTo(expectedSubtitle)
    }

    @Test
    fun getter_authActionEntries() {
        val expectedSize = 1
        val expectedTitle = "boeing"

        val response = BeginGetCredentialResponse(
            emptyList(), emptyList(), listOf(
                constructAuthenticationActionEntry(expectedTitle)
            )
        )
        val actualSize = response.authenticationActions.size
        val actualTitle = response.authenticationActions[0].title.toString()

        assertThat(actualSize).isEqualTo(expectedSize)
        assertThat(actualTitle).isEqualTo(expectedTitle)
    }

    @Test
    fun getter_remoteEntry_null() {
        val expectedRemoteEntry: RemoteEntry? = null
        val response = BeginGetCredentialResponse(
            emptyList(), emptyList(), emptyList(),
            expectedRemoteEntry
        )
        val actualRemoteEntry = response.remoteEntry

        assertThat(actualRemoteEntry).isEqualTo(expectedRemoteEntry)
    }

    @Test
    fun getter_remoteEntry_nonNull() {
        val expectedRemoteEntry = constructRemoteEntryDefault()
        val response = BeginGetCredentialResponse(
            emptyList(), emptyList(), emptyList(),
            expectedRemoteEntry
        )
        val actualRemoteEntry = response.remoteEntry

        assertThat(actualRemoteEntry).isEqualTo(expectedRemoteEntry)
    }
}
