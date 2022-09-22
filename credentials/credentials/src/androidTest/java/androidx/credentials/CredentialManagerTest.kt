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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
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
    fun testCreateCredential() = runBlocking<Unit> {
        assertThrows<UnsupportedOperationException> {
            credentialManager.executeCreateCredential(
                CreatePasswordRequest("test-user-id", "test-password")
            )
        }
    }

    @Test
    fun testGetCredential() = runBlocking<Unit> {
        val request = GetCredentialRequest.Builder()
            .addGetCredentialOption(GetPasswordOption())
            .build()
        assertThrows<UnsupportedOperationException> {
            credentialManager.executeGetCredential(request)
        }
    }

    @Test
    fun testCreateCredentialAsyc() {
        assertThrows<UnsupportedOperationException> {
            credentialManager.executeCreateCredentialAsync(
                request = CreatePasswordRequest("test-user-id", "test-password"),
                activity = null,
                cancellationSignal = null,
                executor = Runnable::run,
                callback = object : CredentialManagerCallback<CreateCredentialResponse> {
                    override fun onResult(result: CreateCredentialResponse) {}
                }
            )
        }
    }

    @Test
    fun testGetCredentialAsyc() {
        assertThrows<UnsupportedOperationException> {
            credentialManager.executeGetCredentialAsync(
                request = GetCredentialRequest.Builder()
                    .addGetCredentialOption(GetPasswordOption())
                    .build(),
                activity = null,
                cancellationSignal = null,
                executor = Runnable::run,
                callback = object : CredentialManagerCallback<GetCredentialResponse> {
                    override fun onResult(result: GetCredentialResponse) {}
                }
            )
        }
    }
}