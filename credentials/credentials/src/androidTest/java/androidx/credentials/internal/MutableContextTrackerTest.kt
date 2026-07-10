/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.internal

import android.content.Context
import android.content.MutableContextWrapper
import android.os.CancellationSignal
import android.os.Looper
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.TestActivity
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MutableContextTrackerTest {

    @Before
    fun setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
    }

    private fun createEmptyCallback() =
        object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
            override fun onResult(result: GetCredentialResponse) {}

            override fun onError(e: GetCredentialException) {}
        }

    private inline fun runWithScenario(
        crossinline block:
            (
                scenario: ActivityScenario<TestActivity>,
                initialActivity: TestActivity,
                wrapper: MutableContextWrapper,
            ) -> Unit
    ) {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        try {
            var initialActivity: TestActivity? = null
            var wrapper: MutableContextWrapper? = null
            scenario.onActivity { activity ->
                initialActivity = activity
                wrapper = MutableContextWrapper(activity)
            }
            block(scenario, initialActivity!!, wrapper!!)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun wrapCallback_survivesRecreation() = runWithScenario { scenario, initialActivity, wrapper ->
        scenario.onActivity {
            MutableContextTracker.wrapCallback(wrapper, null, createEmptyCallback())
        }

        assertThat(wrapper.baseContext).isSameInstanceAs(initialActivity)

        // Recreate the activity (simulates rotation configuration change)
        scenario.recreate()

        var newActivity: TestActivity? = null
        scenario.onActivity { activity -> newActivity = activity }

        // Verify context swapped successfully!
        assertThat(wrapper.baseContext).isSameInstanceAs(newActivity)
        assertThat(wrapper.baseContext).isNotSameInstanceAs(initialActivity)
    }

    @Test
    fun wrapCallback_onPermanentDestruction_fallsBackToApplicationContext() =
        runWithScenario { scenario, initialActivity, wrapper ->
            var appContext: Context? = null

            scenario.onActivity { activity ->
                appContext = activity.applicationContext
                MutableContextTracker.wrapCallback(wrapper, null, createEmptyCallback())
            }

            assertThat(wrapper.baseContext).isSameInstanceAs(initialActivity)

            // Close the activity permanently
            scenario.close()

            // Verify it fell back to Application Context safely!
            assertThat(wrapper.baseContext).isSameInstanceAs(appContext)
        }

    @Test
    fun wrapCallback_onResult_unregistersToken() =
        runWithScenario { scenario, initialActivity, wrapper ->
            var wrappedCallback:
                CredentialManagerCallback<GetCredentialResponse, GetCredentialException>? =
                null

            val callbackLatch = CountDownLatch(1)
            val originalCallback =
                object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                    override fun onResult(result: GetCredentialResponse) {
                        callbackLatch.countDown()
                    }

                    override fun onError(e: GetCredentialException) {}
                }

            scenario.onActivity { _ ->
                val wrapped = MutableContextTracker.wrapCallback(wrapper, null, originalCallback)
                wrappedCallback = wrapped
            }

            // Fire onResult to complete the flow and trigger unregistration
            val mockResponse = GetCredentialResponse(PasswordCredential("id", "password"))
            wrappedCallback!!.onResult(mockResponse)

            assertThat(callbackLatch.await(100, TimeUnit.MILLISECONDS)).isTrue()

            // Recreate the activity
            scenario.recreate()

            // Verify that the wrapper's context is NOT updated because the listener was
            // successfully
            // unregistered!
            assertThat(wrapper.baseContext).isSameInstanceAs(initialActivity)
        }

    @Test
    fun wrapCallback_onError_unregistersToken() =
        runWithScenario { scenario, initialActivity, wrapper ->
            var wrappedCallback:
                CredentialManagerCallback<GetCredentialResponse, GetCredentialException>? =
                null

            val callbackLatch = CountDownLatch(1)
            val originalCallback =
                object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                    override fun onResult(result: GetCredentialResponse) {}

                    override fun onError(e: GetCredentialException) {
                        callbackLatch.countDown()
                    }
                }

            scenario.onActivity { _ ->
                val wrapped = MutableContextTracker.wrapCallback(wrapper, null, originalCallback)
                wrappedCallback = wrapped
            }

            // Fire onError to complete the flow and trigger unregistration
            wrappedCallback!!.onError(GetCredentialUnknownException("mock-error"))

            assertThat(callbackLatch.await(100, TimeUnit.MILLISECONDS)).isTrue()

            // Recreate the activity
            scenario.recreate()

            // Verify that the wrapper's context is NOT updated because the listener was
            // successfully
            // unregistered!
            assertThat(wrapper.baseContext).isSameInstanceAs(initialActivity)
        }

    @Test
    fun wrapCallback_onCancellation_unregistersToken() =
        runWithScenario { scenario, initialActivity, wrapper ->
            val cancellationSignal = CancellationSignal()

            scenario.onActivity { _ ->
                MutableContextTracker.wrapCallback(
                    wrapper,
                    cancellationSignal,
                    createEmptyCallback(),
                )
            }

            // Cancel the cancellation signal to trigger unregistration!
            cancellationSignal.cancel()

            // Recreate the activity
            scenario.recreate()

            // Verify that the wrapper's context is NOT updated because the listener was
            // successfully
            // unregistered!
            assertThat(wrapper.baseContext).isSameInstanceAs(initialActivity)
        }
}
