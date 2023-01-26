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

package androidx.credentials

import android.app.Activity
import android.os.Looper
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.ClearCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CredentialManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var credentialManager: CredentialManager

    @Before
    fun setup() {
        credentialManager = CredentialManager.create(context)
    }

    @Test
    fun createCredential_throws() = runBlocking<Unit> {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        assertThrows<CreateCredentialProviderConfigurationException> {
            credentialManager.executeCreateCredential(
                CreatePasswordRequest("test-user-id", "test-password"),
                Activity()
            )
        }
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }

    @Test
    fun getCredential_throws() = runBlocking<Unit> {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val request = GetCredentialRequest.Builder()
            .addGetCredentialOption(GetPasswordOption())
            .build()
        assertThrows<GetCredentialProviderConfigurationException> {
            credentialManager.executeGetCredential(request, Activity())
        }
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }

    @Test
    fun testClearCredentialSession_throws() = runBlocking<Unit> {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        assertThrows<ClearCredentialProviderConfigurationException> {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }

    @Test
    fun testCreateCredentialAsyc_successCallbackThrows() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val loadedResult: AtomicReference<CreateCredentialException> = AtomicReference()
        credentialManager.executeCreateCredentialAsync(
            request = CreatePasswordRequest("test-user-id", "test-password"),
            activity = Activity(),
            cancellationSignal = null,
            executor = Runnable::run,
            callback = object : CredentialManagerCallback<CreateCredentialResponse,
                CreateCredentialException> {
                override fun onResult(result: CreateCredentialResponse) {}
                override fun onError(e: CreateCredentialException) { loadedResult.set(e) }
            }
        )
        assertThat(loadedResult.get().type).isEqualTo(
            CreateCredentialProviderConfigurationException
            .TYPE_CREATE_CREDENTIAL_PROVIDER_CONFIGURATION_EXCEPTION)
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }

    @Test
    fun testGetCredentialAsyc_successCallbackThrows() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val loadedResult: AtomicReference<GetCredentialException> = AtomicReference()
        credentialManager.executeGetCredentialAsync(
            request = GetCredentialRequest.Builder()
                .addGetCredentialOption(GetPasswordOption())
                .build(),
            activity = Activity(),
            cancellationSignal = null,
            executor = Runnable::run,
            callback = object : CredentialManagerCallback<GetCredentialResponse,
                GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {}
                override fun onError(e: GetCredentialException) { loadedResult.set(e) }
            }
        )
        assertThat(loadedResult.get().type).isEqualTo(
            GetCredentialProviderConfigurationException
            .TYPE_GET_CREDENTIAL_PROVIDER_CONFIGURATION_EXCEPTION)
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }

    @Test
    fun testClearCredentialSessionAsync_throws() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val loadedResult: AtomicReference<ClearCredentialException> = AtomicReference()

        credentialManager.clearCredentialStateAsync(
            ClearCredentialStateRequest(),
            null, Executor { obj: Runnable -> obj.run() },
            object : CredentialManagerCallback<Void?, ClearCredentialException> {
                override fun onError(e: ClearCredentialException) { loadedResult.set(e) }
                override fun onResult(result: Void?) {}
            })

        assertThat(loadedResult.get().type).isEqualTo(
            ClearCredentialProviderConfigurationException
            .TYPE_CLEAR_CREDENTIAL_PROVIDER_CONFIGURATION_EXCEPTION)
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }
}