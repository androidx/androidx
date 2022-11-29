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

import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
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
        assertThrows<CreateCredentialUnknownException> {
            credentialManager.executeCreateCredential(
                CreatePasswordRequest("test-user-id", "test-password")
            )
        }
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }

    @Test
    fun getCredential_throws() = runBlocking<Unit> {
        val request = GetCredentialRequest.Builder()
            .addGetCredentialOption(GetPasswordOption())
            .build()
        assertThrows<GetCredentialUnknownException> {
            credentialManager.executeGetCredential(request)
        }
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }

    @Test
    fun testClearCredentialSession_throws() = runBlocking<Unit> {
        assertThrows<UnsupportedOperationException> {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }

    @Test
    fun testCreateCredentialAsyc_successCallbackThrows() {
        val loadedResult: AtomicReference<CreateCredentialException> = AtomicReference()
        credentialManager.executeCreateCredentialAsync(
            request = CreatePasswordRequest("test-user-id", "test-password"),
            activity = null,
            cancellationSignal = null,
            executor = Runnable::run,
            callback = object : CredentialManagerCallback<CreateCredentialResponse,
                CreateCredentialException> {
                override fun onResult(result: CreateCredentialResponse) {}
                override fun onError(e: CreateCredentialException) { loadedResult.set(e) }
            }
        )
        assertThat(loadedResult.get().type).isEqualTo(CreateCredentialUnknownException
            .TYPE_CREATE_CREDENTIAL_UNKNOWN_EXCEPTION)
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }

    @Test
    fun testGetCredentialAsyc_successCallbackThrows() {
        val loadedResult: AtomicReference<GetCredentialException> = AtomicReference()
        credentialManager.executeGetCredentialAsync(
            request = GetCredentialRequest.Builder()
                .addGetCredentialOption(GetPasswordOption())
                .build(),
            activity = null,
            cancellationSignal = null,
            executor = Runnable::run,
            callback = object : CredentialManagerCallback<GetCredentialResponse,
                GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {}
                override fun onError(e: GetCredentialException) { loadedResult.set(e) }
            }
        )
        assertThat(loadedResult.get().type).isEqualTo(GetCredentialUnknownException
            .TYPE_GET_CREDENTIAL_UNKNOWN_EXCEPTION)
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }

    @Test
    fun testClearCredentialSessionAsync_throws() {
        assertThrows<UnsupportedOperationException> {
            credentialManager.clearCredentialStateAsync(
                request = ClearCredentialStateRequest(),
                cancellationSignal = null,
                executor = Runnable::run,
                callback = object : CredentialManagerCallback<Void, ClearCredentialException> {
                    override fun onResult(result: Void) {}
                    override fun onError(e: ClearCredentialException) {}
                }
            )
        }
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }
}