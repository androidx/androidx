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
package androidx.credentials

import android.os.Build
import androidx.credentials.ClearCredentialStateRequest.Companion.TYPE_CLEAR_RESTORE_CREDENTIAL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CredentialProviderFactoryTest {
    companion object {
        private const val TEST_USERNAME = "test-user-natcme@gmail.com"
        private const val TEST_USER_DISPLAYNAME = "Test User"
        private const val TEST_REQUEST_JSON =
            "{\"rp\":{\"name\":true,\"id\":\"app-id\"}," +
                "\"user\":{\"name\":\"$TEST_USERNAME\",\"id\":\"id-value\",\"displayName" +
                "\":\"$TEST_USER_DISPLAYNAME\",\"icon\":true}, \"challenge\":true," +
                "\"pubKeyCredParams\":true,\"excludeCredentials\":true," +
                "\"attestation\":true}"
    }

    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var credentialProviderFactory: CredentialProviderFactory

    @Before
    fun setup() {
        credentialProviderFactory = CredentialProviderFactory(context)
    }

    private fun clearState() {
        credentialProviderFactory.testMode = false
        credentialProviderFactory.testPreUProvider = null
        credentialProviderFactory.testPostUProvider = null
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun getBestAvailableProvider_postU_success() {
        if (Build.VERSION.SDK_INT <= 33) {
            return
        }
        clearState()
        val expectedProvider = FakeProvider(success = true)
        credentialProviderFactory.testMode = true
        credentialProviderFactory.testPostUProvider = expectedProvider

        val actualProvider = credentialProviderFactory.getBestAvailableProvider()

        assertThat(actualProvider).isNotNull()
        assertThat(actualProvider).isEqualTo(expectedProvider)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun getBestAvailableProvider_postU_notAvailable_preUSuccess() {
        if (Build.VERSION.SDK_INT <= 33) {
            return
        }
        clearState()
        val expectedPreUProvider = FakeProvider(success = true)
        credentialProviderFactory.testMode = true
        credentialProviderFactory.testPreUProvider = expectedPreUProvider
        credentialProviderFactory.testPostUProvider = null

        val actualProvider = credentialProviderFactory.getBestAvailableProvider()

        assertThat(actualProvider).isNotNull()
        assertThat(actualProvider).isEqualTo(expectedPreUProvider)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun getBestAvailableProvider_postU_notAvailableNoFallbackAllowed_returnsNull() {
        if (Build.VERSION.SDK_INT <= 33) {
            return
        }
        clearState()
        val expectedPreUProvider = FakeProvider(success = true)
        credentialProviderFactory.testMode = true
        credentialProviderFactory.testPreUProvider = expectedPreUProvider
        credentialProviderFactory.testPostUProvider = null

        val actualProvider =
            credentialProviderFactory.getBestAvailableProvider(shouldFallbackToPreU = false)

        assertNull(actualProvider)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun getBestAvailableProvider_postU_preAndPostUProvNull_returnsNull() {
        if (Build.VERSION.SDK_INT <= 33) {
            return
        }
        clearState()
        credentialProviderFactory.testMode = true
        credentialProviderFactory.testPostUProvider = null
        credentialProviderFactory.testPreUProvider = null

        assertNull(credentialProviderFactory.getBestAvailableProvider())
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun getBestAvailableProvider_postU_preAndPostUProvNotAvailable_returnsNull() {
        if (Build.VERSION.SDK_INT <= 33) {
            return
        }
        clearState()
        credentialProviderFactory.testMode = true
        credentialProviderFactory.testPostUProvider = FakeProvider(success = false)
        credentialProviderFactory.testPreUProvider = FakeProvider(success = false)

        assertNull(credentialProviderFactory.getBestAvailableProvider())
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun getBestAvailableProvider_postU_restoreCredential_returnsPreU() {
        if (Build.VERSION.SDK_INT <= 33) {
            return
        }
        clearState()
        val expectedProvider = FakeProvider(success = true)
        credentialProviderFactory.testMode = true
        credentialProviderFactory.testPreUProvider = expectedProvider

        val credentialOptions = ArrayList<CredentialOption>()
        credentialOptions.add(GetRestoreCredentialOption(TEST_REQUEST_JSON))
        val request = GetCredentialRequest(credentialOptions)

        assertThat(
                credentialProviderFactory.getBestAvailableProvider(TYPE_CLEAR_RESTORE_CREDENTIAL)
            )
            .isEqualTo(expectedProvider)
        assertThat(
                credentialProviderFactory.getBestAvailableProvider(
                    CreateRestoreCredentialRequest(TEST_REQUEST_JSON)
                )
            )
            .isEqualTo(expectedProvider)
        assertThat(credentialProviderFactory.getBestAvailableProvider(request))
            .isEqualTo(expectedProvider)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun getBestAvailableProvider_preU_success() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }
        clearState()
        val expectedProvider = FakeProvider(success = true)
        credentialProviderFactory.testMode = true
        credentialProviderFactory.testPreUProvider = expectedProvider

        val actualProvider = credentialProviderFactory.getBestAvailableProvider()

        assertThat(actualProvider).isNotNull()
        assertThat(actualProvider).isEqualTo(expectedProvider)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun getBestAvailableProvider_preU_providerNotAvailable_returnsNull() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }
        clearState()
        credentialProviderFactory.testMode = true
        credentialProviderFactory.testPreUProvider = FakeProvider(success = false)

        val provider = credentialProviderFactory.getBestAvailableProvider()

        assertNull(provider)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun getBestAvailableProvider_preUNull_returnsNull() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }
        clearState()
        credentialProviderFactory.testMode = true
        credentialProviderFactory.testPreUProvider = null

        assertNull(credentialProviderFactory.getBestAvailableProvider())
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun getBestAvailableProvider_preU_restoreCredential_returnsPreU() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }
        clearState()
        val expectedProvider = FakeProvider(success = true)
        credentialProviderFactory.testMode = true
        credentialProviderFactory.testPreUProvider = expectedProvider

        val credentialOptions = ArrayList<CredentialOption>()
        credentialOptions.add(GetRestoreCredentialOption(TEST_REQUEST_JSON))
        val request = GetCredentialRequest(credentialOptions)

        assertThat(
                credentialProviderFactory.getBestAvailableProvider(TYPE_CLEAR_RESTORE_CREDENTIAL)
            )
            .isEqualTo(expectedProvider)
        assertThat(
                credentialProviderFactory.getBestAvailableProvider(
                    CreateRestoreCredentialRequest(TEST_REQUEST_JSON)
                )
            )
            .isEqualTo(expectedProvider)
        assertThat(credentialProviderFactory.getBestAvailableProvider(request))
            .isEqualTo(expectedProvider)
    }
}
