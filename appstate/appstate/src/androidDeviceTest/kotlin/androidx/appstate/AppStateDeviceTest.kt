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

import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AppStateDeviceTest {

    private val testDispatcher = StandardTestDispatcher()

    @Serializable object StringKey : AppStateKey<String>()

    @Test
    fun testAddAppStateListener() =
        runTest(testDispatcher) {
            Dispatchers.setMain(testDispatcher)
            val appState = AppState()
            var receivedValue: String? = null

            appState.setState(StringKey, "initial")

            val token =
                appState.addAppStateListener(coroutineContext) { map ->
                    receivedValue = map[StringKey]?.value as? String
                }

            // Wait for first composition
            runCurrent()
            assertThat(receivedValue).isEqualTo("initial")

            // Update state and verify listener is called again
            appState.setState(StringKey, "updated")
            runCurrent()
            assertThat(receivedValue).isEqualTo("updated")

            // Remove listener
            appState.removeAppStateListener(token)

            // Update state again and verify listener is NOT called
            appState.setState(StringKey, "ignored")
            runCurrent()
            assertThat(receivedValue).isEqualTo("updated")
        }
}
