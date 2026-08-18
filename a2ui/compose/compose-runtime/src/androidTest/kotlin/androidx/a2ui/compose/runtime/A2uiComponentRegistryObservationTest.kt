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

package androidx.a2ui.compose.runtime

import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class A2uiComponentRegistryObservationTest {

    private val registry = A2uiComponentRegistry()

    @Test
    fun observe_unknownComponent_registersDependencyAndUpdatesWhenLoaded() = runComposeUiTest {
        val componentId = "future_comp"

        setContent {
            when (val record = registry.get(componentId)) {
                null -> BasicText("Waiting...")
                is A2uiComponentRecord.Valid -> BasicText("Arrived: ${record.type}")
                is A2uiComponentRecord.Error -> BasicText("Error")
            }
        }

        onNodeWithText("Waiting...").assertIsDisplayed()

        registry.update(listOf(A2uiComponentPayload(componentId, "Text", emptyMap())))
        waitForIdle()

        onNodeWithText("Waiting...").assertIsNotDisplayed()
        onNodeWithText("Arrived: Text").assertIsDisplayed()
    }

    @Test
    fun observe_componentUpdate_triggersRecomposition() = runComposeUiTest {
        val componentId = "reactive_comp"

        setContent {
            when (val record = registry.get(componentId)) {
                null -> BasicText("Loading...")
                is A2uiComponentRecord.Valid -> {
                    val text = record.properties.raw["text"] as? String ?: "Empty"
                    BasicText("Success: $text")
                }
                is A2uiComponentRecord.Error -> BasicText("Error")
            }
        }

        onNodeWithText("Loading...").assertIsDisplayed()

        registry.update(
            listOf(A2uiComponentPayload(componentId, "Text", mapOf("text" to "Initial Value")))
        )
        waitForIdle()
        onNodeWithText("Success: Initial Value").assertIsDisplayed()

        registry.update(
            listOf(A2uiComponentPayload(componentId, "Text", mapOf("text" to "Updated Value")))
        )
        waitForIdle()
        onNodeWithText("Success: Updated Value").assertIsDisplayed()
    }

    @Test
    fun observe_componentError_triggersRecomposition() = runComposeUiTest {
        val componentId = "error_prone_comp"
        registry.update(
            listOf(A2uiComponentPayload(componentId, "Text", mapOf("text" to "Initial Content")))
        )

        setContent {
            when (val record = registry.get(componentId)) {
                null -> BasicText("Loading...")
                is A2uiComponentRecord.Valid ->
                    BasicText("Success: ${record.properties.raw["text"]}")
                is A2uiComponentRecord.Error -> BasicText("Error: ${record.exception.message}")
            }
        }

        onNodeWithText("Success: Initial Content").assertIsDisplayed()

        registry.reportError(componentId, A2uiRuntimeException("Failure"))
        waitForIdle()

        onNodeWithText("Error: Failure").assertIsDisplayed()
    }

    @Test
    fun observe_identicalComponentUpdate_doesNotTriggerRecomposition() = runComposeUiTest {
        registry.update(listOf(A2uiComponentPayload("comp_1", "Text", mapOf("text" to "Initial"))))

        var recompositionCount = 0
        setContent {
            registry.get("comp_1")
            recompositionCount++
            BasicText(text = "Recomposed $recompositionCount")
        }

        waitForIdle()
        val initialCount = recompositionCount

        registry.update(listOf(A2uiComponentPayload("comp_1", "Text", mapOf("text" to "Initial"))))
        waitForIdle()

        assertThat(recompositionCount).isEqualTo(initialCount)
    }

    @Test
    fun observe_updateUnrelatedComponent_doesNotTriggerRecomposition() = runComposeUiTest {
        registry.update(
            listOf(
                A2uiComponentPayload("comp_A", "Text", mapOf("text" to "A")),
                A2uiComponentPayload("comp_B", "Text", mapOf("text" to "B")),
            )
        )

        var recompositionCountB = 0
        setContent {
            registry.get("comp_B")
            recompositionCountB++
            BasicText(text = "Recomposed B: $recompositionCountB")
        }
        waitForIdle()
        val initialCountB = recompositionCountB

        registry.update(
            listOf(A2uiComponentPayload("comp_A", "Text", mapOf("text" to "A_Updated")))
        )
        waitForIdle()

        assertThat(recompositionCountB).isEqualTo(initialCountB)
    }
}
