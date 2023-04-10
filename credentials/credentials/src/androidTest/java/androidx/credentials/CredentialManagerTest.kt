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
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
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
        if (!isPostFrameworkApiLevel()) {
            assertThrows<CreateCredentialProviderConfigurationException> {
                credentialManager.createCredential(
                    CreatePasswordRequest("test-user-id", "test-password"),
                    Activity()
                )
            }
        }
        // TODO("Add manifest tests and possibly further separate these tests by API Level
        //  - maybe a rule perhaps?")
    }

    @Test
    fun getCredential_throws() = runBlocking<Unit> {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetPasswordOption())
            .build()

        if (!isPostFrameworkApiLevel()) {
            assertThrows<GetCredentialProviderConfigurationException> {
                credentialManager.getCredential(request, Activity())
            }
        }
        // TODO("Add manifest tests and possibly further separate these tests by API Level
        //  - maybe a rule perhaps?")
    }

    @Test
    fun testClearCredentialSession_throws() = runBlocking<Unit> {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        if (!isPostFrameworkApiLevel()) {
            assertThrows<ClearCredentialProviderConfigurationException> {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
        }
        // TODO("Add manifest tests and possibly further separate these tests by API Level
        //  - maybe a rule perhaps?")
    }

    @Test
    fun testCreateCredentialAsyc_successCallbackThrows() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val latch = CountDownLatch(1)
        val loadedResult: AtomicReference<CreateCredentialException> = AtomicReference()
        val activityScenario = ActivityScenario.launch(
            TestActivity::class.java
        )

        activityScenario.onActivity { activity ->
            credentialManager.createCredentialAsync(
                CreatePasswordRequest("test-user-id", "test-password"),
                activity,
                null, Executor { obj: Runnable -> obj.run() },
                object : CredentialManagerCallback<CreateCredentialResponse,
                    CreateCredentialException> {
                    override fun onResult(result: CreateCredentialResponse) {}
                    override fun onError(e: CreateCredentialException) {
                        loadedResult.set(e)
                        latch.countDown()
                    }
                })
        }

        latch.await(100L, TimeUnit.MILLISECONDS)
        if (!isPostFrameworkApiLevel()) {
            assertThat(loadedResult.get().javaClass).isEqualTo(
                CreateCredentialProviderConfigurationException::class.java
            )
        }
        // TODO("Add manifest tests and possibly further separate these tests by API Level
        //  - maybe a rule perhaps?")
    }

    @Test
    fun testGetCredentialAsyc_successCallbackThrows() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val latch = CountDownLatch(1)
        val loadedResult: AtomicReference<GetCredentialException> = AtomicReference()

        credentialManager.getCredentialAsync(
            request = GetCredentialRequest.Builder()
                .addCredentialOption(GetPasswordOption())
                .build(),
            activity = Activity(),
            cancellationSignal = null,
            executor = Runnable::run,
            callback = object : CredentialManagerCallback<GetCredentialResponse,
                GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {}
                override fun onError(e: GetCredentialException) {
                    loadedResult.set(e)
                    latch.countDown()
                }
            }
        )

        latch.await(100L, TimeUnit.MILLISECONDS)
        if (!isPostFrameworkApiLevel()) {
            assertThat(loadedResult.get().javaClass).isEqualTo(
                GetCredentialProviderConfigurationException::class.java
            )
        }
        // TODO("Add manifest tests and possibly further separate these tests - maybe a rule
        //  perhaps?")
    }

    @Test
    fun testClearCredentialSessionAsync_throws() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        if (isPostFrameworkApiLevel()) {
            return // TODO(Support!)
        }
        val latch = CountDownLatch(1)
        val loadedResult: AtomicReference<ClearCredentialException> = AtomicReference()

        credentialManager.clearCredentialStateAsync(
            ClearCredentialStateRequest(),
            null, Executor { obj: Runnable -> obj.run() },
            object : CredentialManagerCallback<Void?, ClearCredentialException> {
                override fun onError(e: ClearCredentialException) {
                    loadedResult.set(e)
                    latch.countDown()
                }
                override fun onResult(result: Void?) {}
            })

        latch.await(100L, TimeUnit.MILLISECONDS)
        assertThat(loadedResult.get().type).isEqualTo(
            ClearCredentialProviderConfigurationException
            .TYPE_CLEAR_CREDENTIAL_PROVIDER_CONFIGURATION_EXCEPTION)
        // TODO(Add manifest tests and split this once postU is implemented for clearCreds")
    }
}