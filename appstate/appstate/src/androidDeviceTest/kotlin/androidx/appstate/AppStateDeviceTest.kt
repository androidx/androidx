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

package androidx.appstate

import androidx.appstate.transform.listener
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
class AppStateDeviceTest {
    @Serializable object StringKey : AppStateKey<String>()

    @Serializable object AutoClearKey : AppStateKey<String>(autoClearKey = StringKey)

    @Serializable
    object StringKeyWithPredicate :
        AppStateKey<String>(
            autoClearKey = StringKey,
            shouldClearState = { appState -> appState.getState(StringKey, "").value == "clear" },
        )

    @Test
    fun testListenerReceivesAppStateUpdates() = runTest {
        val appState = AppState()
        var receivedValue: String? = null

        appState.setState(StringKey, "initial")
        val job =
            backgroundScope.launch {
                listener(testDispatcher) {
                    receivedValue = appState.getState(StringKey, "default").value
                }
            }

        // Wait for first composition
        runRecomposition()
        assertThat(receivedValue).isEqualTo("initial")

        // Update state and verify listener is called again
        appState.setState(StringKey, "updated")
        runRecomposition()
        assertThat(receivedValue).isEqualTo("updated")

        // Remove listener
        job.cancel()

        // Update state again and verify listener is NOT called
        appState.setState(StringKey, "ignored")
        runRecomposition()
        assertThat(receivedValue).isEqualTo("updated")
    }

    @Test
    fun testAutoClear() = runTest {
        val appState = AppState()

        appState.setState(AutoClearKey, "targetValue")
        assertThat(appState.keys).contains(AutoClearKey)

        // Set key to trigger clear
        appState.setState(StringKey, "triggerValue")

        // Run recomposition to let the listener and LaunchedEffect run
        runRecomposition()

        // this clears because the default for AppStateKey is to autoclear.
        assertThat(appState.keys).doesNotContain(AutoClearKey)
    }

    @Test
    fun testAutoClearWithPredicate() = runTest {
        val appState = AppState()

        appState.setState(StringKeyWithPredicate, "targetValue")
        assertThat(appState.keys).contains(StringKeyWithPredicate)

        // Set trigger key to something that does NOT satisfy predicate
        appState.setState(StringKey, "dont-clear")
        runRecomposition()

        // Verify TargetKeyWithPredicate is NOT cleared
        assertThat(appState.keys).contains(StringKeyWithPredicate)

        // Set trigger key to "clear" which satisfies predicate
        appState.setState(StringKey, "clear")
        runRecomposition()

        // Verify TargetKeyWithPredicate IS cleared
        assertThat(appState.keys).doesNotContain(StringKeyWithPredicate)
    }

    private fun TestScope.runRecomposition() {
        runCurrent()
        Snapshot.sendApplyNotifications()
        runCurrent()
    }
}

private val TestScope.testDispatcher: CoroutineDispatcher
    get() = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
