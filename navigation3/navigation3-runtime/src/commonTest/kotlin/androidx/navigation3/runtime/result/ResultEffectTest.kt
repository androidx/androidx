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

package androidx.navigation3.runtime.result

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.kruth.assertThat
import kotlin.test.Test

class ResultEffectTest {

    @Test
    fun testResultEffect_capturesUpdatedOnResult() = compositionTest {
        val resultEventBus = ResultEventBus()
        var state by mutableStateOf("initial")
        var receivedResult: String? = null

        compose {
            val currentStateValue = state // Read state here to trigger recomposition
            ResultEffect<String>(
                resultKey = "testKey",
                resultEventBus = resultEventBus,
                onResult = { result -> receivedResult = "$result with state $currentStateValue" },
            )
        }

        advance() // Apply recomposition triggered by channel creation

        resultEventBus.sendResult("testKey", "event")
        advanceTimeBy(1000) // Process flow collection
        assertThat(receivedResult).isEqualTo("event with state initial")

        state = "updated"
        advance() // Recompose to apply state change

        resultEventBus.sendResult("testKey", "event")
        advanceTimeBy(1000) // Process flow collection

        assertThat(receivedResult).isEqualTo("event with state updated")
    }
}
