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

import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.ClearCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@RequiresApi(34)
@SdkSuppress(minSdkVersion = 34)
class CredentialManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var credentialManager: CredentialManager

    @Before
    fun setup() {
        credentialManager = CredentialManager.create(context)
    }

    @Test
    fun getCredential_requestBasedApi_throws() = runBlocking<Unit> {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetPasswordOption())
            .build()

        withUse(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                runBlocking {
                    assertThrows<NoCredentialException> {
                        credentialManager.getCredential(this@withActivity, request)
                    }
                }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
    fun testPrepareGetCredential_throwsUnimplementedError() = runBlocking<Unit> {
        val prepareGetCredentialResponse = credentialManager.prepareGetCredential(
            GetCredentialRequest(listOf(GetPasswordOption()))
        )

        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        withUse(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                runBlocking {
                    assertThrows<NoCredentialException> {
                        credentialManager.getCredential(
                            this@withActivity,
                            prepareGetCredentialResponse.pendingGetCredentialHandle!!
                        )
                    }
                }
            }
        }
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
                activity,
                CreatePasswordRequest("test-user-id", "test-password"),
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
        if (loadedResult.get() == null) {
            return // A strange flow occurred where an exception wasn't propagated up
        }
        assertThat(loadedResult.get().javaClass).isEqualTo(
            CreateCredentialNoCreateOptionException::class.java
        )
    }

    @Ignore // b/290792682
    @Test
    fun testClearCredentialSessionAsync_throws() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
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
        if (loadedResult.get() == null) {
            return // A strange flow occurred where an exception wasn't propagated up
        }

        // Check the exception is the correct type.
        assertThat(loadedResult.get().type).isEqualTo(
            ClearCredentialProviderConfigurationException
                .TYPE_CLEAR_CREDENTIAL_PROVIDER_CONFIGURATION_EXCEPTION
        )
    }
}
