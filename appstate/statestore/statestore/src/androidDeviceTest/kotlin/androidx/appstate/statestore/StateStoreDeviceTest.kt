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

package androidx.appstate.statestore

import androidx.appstate.transform.listener
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class StateStoreDeviceTest {
    @Serializable object StringKey : StateStoreKey<String>("default")

    @Serializable object TriggerKey : StateStoreKey<String?>(null)

    @Test
    fun testListenerReceivesStateStoreUpdates() = runTest {
        val stateStore = StateStore()
        var receivedValue: String? = null

        stateStore.setState(StringKey, "initial")
        val job = backgroundScope.launch {
            listener(testDispatcher) { receivedValue = stateStore.getState(StringKey).value }
        }

        // Wait for first composition
        runRecomposition()
        assertThat(receivedValue).isEqualTo("initial")

        // Update state and verify listener is called again
        stateStore.setState(StringKey, "updated")
        runRecomposition()
        assertThat(receivedValue).isEqualTo("updated")

        // Remove listener
        job.cancel()

        // Update state again and verify listener is NOT called
        stateStore.setState(StringKey, "ignored")
        runRecomposition()
        assertThat(receivedValue).isEqualTo("updated")
    }

    @Test
    fun testManualClearWithListener() = runTest {
        val stateStore = StateStore()
        stateStore.setState(TriggerKey, "user-1")
        stateStore.setState(StringKey, "draft")

        val job = backgroundScope.launch {
            listener(testDispatcher) {
                val trigger by stateStore.getState(TriggerKey)
                LaunchedEffect(trigger) {
                    if (trigger == null) {
                        stateStore.setState(StringKey, StringKey.defaultValue)
                    }
                }
            }
        }
        runRecomposition()
        assertThat(stateStore.getState(StringKey).value).isEqualTo("draft")

        stateStore.setState(TriggerKey, null)
        runRecomposition()
        assertThat(stateStore.getState(StringKey).value).isEqualTo(StringKey.defaultValue)

        job.cancel()
    }

    private fun TestScope.runRecomposition() {
        runCurrent()
        Snapshot.sendApplyNotifications()
        runCurrent()
    }
}

private val TestScope.testDispatcher: CoroutineDispatcher
    get() = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
