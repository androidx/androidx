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

package androidx.compose.ui.test

import android.os.Handler
import android.os.Looper
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(minSdk = RobolectricMinSdk)
@OptIn(ExperimentalTestApi::class)
class RobolectricIdlingResourceTest {

    @Test
    fun testIdlingResourcesAreQueried() = runComposeUiTest {
        val idlingResource =
            object : IdlingResource {
                var readCount = MutableStateFlow(0)

                override var isIdleNow: Boolean = false
                    get() {
                        readCount.update { it + 1 }
                        return field
                    }

                suspend fun waitForTenReads() {
                    val start = readCount.value
                    readCount.collect {
                        if (it >= start + 10) {
                            isIdleNow = true
                        }
                    }
                }
            }

        registerIdlingResource(idlingResource)
        val executor = Executors.newSingleThreadExecutor()
        executor.execute { runBlocking { idlingResource.waitForTenReads() } }

        val startReadCount = idlingResource.readCount.value
        assertThat(idlingResource.isIdleNow).isFalse()

        waitForIdle()
        val endReadCount = idlingResource.readCount.value

        assertThat(idlingResource.isIdleNow).isTrue()
        assertThat(startReadCount).isEqualTo(0)
        assertThat(endReadCount - startReadCount).isAtLeast(10)

        unregisterIdlingResource(idlingResource)
        executor.shutdownNow()
    }

    @Test
    fun testIdlingResourceFromBackgroundThread() = runComposeUiTest {
        val executor = Executors.newSingleThreadExecutor()
        val counterState = mutableStateOf(-1)

        var isIdleCheckCount = 0
        var workCompleted = false

        val customIdlingResource =
            object : IdlingResource {
                override val isIdleNow: Boolean
                    get() {
                        isIdleCheckCount++

                        if (isIdleCheckCount == 1) {
                            // On the first poll, we simulate the case:
                            // Schedule background work that updates compose state and report busy.
                            executor.execute {
                                counterState.value = 100
                                workCompleted = true
                            }
                            return false
                        }
                        return workCompleted
                    }
            }

        registerIdlingResource(customIdlingResource)

        setContent { Text(text = "Counter: ${counterState.value}") }

        // This assertion forces `runUntilIdle()` to execute.
        // It will poll `isIdleNow` (triggering the background work),
        // wait for `workCompleted` to become true, and verify that Compose processed
        // the snapshot state change from the background thread before proceeding.
        onNodeWithText("Counter: 100").assertIsDisplayed()

        unregisterIdlingResource(customIdlingResource)
        executor.shutdownNow()
    }

    @Test
    fun testIsIdleNow_calledBeforeMainQueueDrained_causesCrash() = runComposeUiTest {
        var criticalAppState: String? = null

        // Queue a state initialization on the main thread.
        // We use this to verify that the idling strategy correctly drains
        // the main looper before querying idling resources.
        Handler(Looper.getMainLooper()).post { criticalAppState = "Initialized and Ready" }

        val strictlyOrderedIdlingResource =
            object : IdlingResource {
                override val isIdleNow: Boolean
                    get() {
                        // The runUntilIdle() loop ensures the main queue is drained before
                        // evaluating `isIdleNow`. Consequently, the Handler task above is
                        // guaranteed to have executed, `criticalAppState` is safely initialized,
                        // and no exception occurs.
                        return criticalAppState!!.isNotEmpty()
                    }
            }

        registerIdlingResource(strictlyOrderedIdlingResource)
        waitForIdle()
        unregisterIdlingResource(strictlyOrderedIdlingResource)
    }
}
