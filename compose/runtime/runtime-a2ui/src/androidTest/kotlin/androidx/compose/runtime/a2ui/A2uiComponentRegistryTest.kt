/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.runtime.a2ui

import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.a2ui.model.protocol.A2uiException.A2uiValidationException
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class A2uiComponentRegistryTest {

    private var registry = A2uiComponentRegistry()

    @Test
    fun get_returnsNullForUnknownComponentId() {
        assertThat(registry.get("unknown_id")).isNull()
    }

    @Test
    fun update_newComponents_addsToRegistry() {
        val payload1 =
            A2uiComponentPayload(
                id = "comp_1",
                type = "Text",
                properties = mapOf("text" to "Hello"),
            )
        val payload2 = A2uiComponentPayload(id = "comp_2", type = "Button", properties = emptyMap())

        registry.update(listOf(payload1, payload2))

        val record1 = registry.get("comp_1")
        assertIs<A2uiComponentRecord.Valid>(record1)
        assertThat(record1.type).isEqualTo("Text")
        assertThat(record1.properties.raw["text"]).isEqualTo("Hello")
        val record2 = registry.get("comp_2")
        assertIs<A2uiComponentRecord.Valid>(record2)
        assertThat(record2.type).isEqualTo("Button")
    }

    @Test
    fun update_existingIdWithDifferentType_replacesComponentRecord() {
        val initialPayload = A2uiComponentPayload("comp_1", "Text", mapOf("text" to "Hello"))
        registry.update(listOf(initialPayload))
        val initialRecord = registry.get("comp_1")

        val modifiedPayload = A2uiComponentPayload("comp_1", "Button", mapOf("text" to "Hello"))
        registry.update(listOf(modifiedPayload))

        val updatedRecord = registry.get("comp_1")
        assertThat(updatedRecord).isNotSameInstanceAs(initialRecord)
        assertIs<A2uiComponentRecord.Valid>(updatedRecord)
        assertThat(updatedRecord.type).isEqualTo("Button")
        assertThat(updatedRecord.properties.raw["text"]).isEqualTo("Hello")
    }

    @Test
    fun update_existingIdWithUpdatedProperties_replacesComponentRecord() {
        registry.update(
            listOf(
                A2uiComponentPayload("comp_1", "Text", mapOf("text" to "Hello", "color" to "red"))
            )
        )
        val initialRecord = registry.get("comp_1")

        registry.update(
            listOf(
                A2uiComponentPayload(
                    "comp_1",
                    "Text",
                    mapOf("text" to "Hello2", "color" to "green"),
                )
            )
        )

        val updatedRecord = registry.get("comp_1")
        assertThat(updatedRecord).isNotSameInstanceAs(initialRecord)
        assertIs<A2uiComponentRecord.Valid>(updatedRecord)
        assertThat(updatedRecord.properties.raw["text"]).isEqualTo("Hello2")
        assertThat(updatedRecord.properties.raw["color"]).isEqualTo("green")
    }

    @Test
    fun update_existingIdWithUpdatedNestedProperties_replacesComponentRecord() {
        registry.update(
            listOf(
                A2uiComponentPayload(
                    "comp_1",
                    "Text",
                    mapOf("config" to mapOf("items" to listOf("A", "B", "C"))),
                )
            )
        )
        val initialRecord = registry.get("comp_1")

        registry.update(
            listOf(
                A2uiComponentPayload(
                    "comp_1",
                    "Text",
                    mapOf("config" to mapOf("items" to listOf("A", "B", "D"))),
                )
            )
        )

        val updatedRecord = registry.get("comp_1")
        assertThat(updatedRecord).isNotSameInstanceAs(initialRecord)
        assertIs<A2uiComponentRecord.Valid>(updatedRecord)
        @Suppress("UNCHECKED_CAST")
        val config = updatedRecord.properties.raw["config"] as Map<String, Any>
        assertThat(config["items"]).isEqualTo(listOf("A", "B", "D"))
    }

    @Test
    fun update_existingIdWithRemovedProperty_replacesComponentRecord() {
        registry.update(
            listOf(
                A2uiComponentPayload("comp_1", "Text", mapOf("text" to "Hello", "color" to "red"))
            )
        )
        val initialRecord = registry.get("comp_1")

        registry.update(listOf(A2uiComponentPayload("comp_1", "Text", mapOf("text" to "Hello"))))

        val updatedRecord = registry.get("comp_1")
        assertThat(updatedRecord).isNotSameInstanceAs(initialRecord)
        assertIs<A2uiComponentRecord.Valid>(updatedRecord)
        assertThat(updatedRecord.properties.raw["text"]).isEqualTo("Hello")
        assertThat(updatedRecord.properties.raw).doesNotContainKey("color")
    }

    @Test
    fun update_emptyList_doesNotTriggerSnapshotOrModifyRegistry() {
        val payload =
            A2uiComponentPayload(
                id = "comp_1",
                type = "Text",
                properties = mapOf("text" to "Hello"),
            )
        registry.update(listOf(payload))
        val initialRecord = registry.get("comp_1")

        var applyNotifications = 0
        val unregister = Snapshot.registerApplyObserver { _, _ -> applyNotifications++ }

        try {
            registry.update(emptyList())

            assertThat(applyNotifications).isEqualTo(0)
            assertThat(registry.get("comp_1")).isSameInstanceAs(initialRecord)
        } finally {
            unregister.dispose()
        }
    }

    @Test
    fun update_identicalComponent_skipsUpdate() {
        val payload =
            A2uiComponentPayload(
                id = "comp_1",
                type = "Text",
                properties =
                    mapOf(
                        "text" to "Hello",
                        "action" to
                            mapOf(
                                "event" to mapOf("name" to "submit", "context" to listOf(1, 2, 3))
                            ),
                    ),
            )
        registry.update(listOf(payload))
        val initialRecord = registry.get("comp_1")

        val identicalPayload =
            A2uiComponentPayload(
                id = "comp_1",
                type = "Text",
                properties =
                    mapOf(
                        "text" to "Hello",
                        "action" to
                            mapOf(
                                "event" to mapOf("name" to "submit", "context" to listOf(1, 2, 3))
                            ),
                    ),
            )
        registry.update(listOf(identicalPayload))

        val updatedRecord = registry.get("comp_1")
        assertThat(updatedRecord).isSameInstanceAs(initialRecord)
    }

    @Test
    fun update_existingErrorRecordWithValidComponent_replacesWithValidRecord() {
        val exception = A2uiValidationException("Missing required property", path = "path")
        registry.reportError("comp_1", exception)
        assertThat(registry.get("comp_1")).isInstanceOf(A2uiComponentRecord.Error::class.java)

        val validPayload =
            A2uiComponentPayload(
                id = "comp_1",
                type = "Text",
                properties = mapOf("text" to "Fixed"),
            )
        registry.update(listOf(validPayload))

        val recoveredRecord = registry.get("comp_1")
        assertIs<A2uiComponentRecord.Valid>(recoveredRecord)
        assertThat(recoveredRecord.properties.raw["text"]).isEqualTo("Fixed")
    }

    @Test
    fun update_mixedBatch_addsUpdatesAndSkipsProperly() {
        val payload1 = A2uiComponentPayload("comp_1", "Text", mapOf("text" to "A"))
        val payload2 = A2uiComponentPayload("comp_2", "Text", mapOf("text" to "B"))
        registry.update(listOf(payload1, payload2))
        val record1 = registry.get("comp_1")
        val record2 = registry.get("comp_2")

        // Construct a batch with 1 identical, 1 modified, and 1 new component
        val payload1Identical = A2uiComponentPayload("comp_1", "Text", mapOf("text" to "A"))
        val payload2Modified = A2uiComponentPayload("comp_2", "Text", mapOf("text" to "B_changed"))
        val payload3New = A2uiComponentPayload("comp_3", "Image", emptyMap())
        registry.update(listOf(payload1Identical, payload2Modified, payload3New))

        val updatedRecord1 = registry.get("comp_1")
        val updatedRecord2 = registry.get("comp_2")
        val newRecord3 = registry.get("comp_3")

        // The identical component must retain exact referential equality
        assertThat(updatedRecord1).isSameInstanceAs(record1)

        // The modified component must be replaced
        assertThat(updatedRecord2).isNotSameInstanceAs(record2)
        assertIs<A2uiComponentRecord.Valid>(updatedRecord2)
        assertThat(updatedRecord2.properties.raw["text"]).isEqualTo("B_changed")

        // The new component must be added
        assertIs<A2uiComponentRecord.Valid>(newRecord3)
        assertThat(newRecord3.type).isEqualTo("Image")
    }

    @Test
    fun update_duplicateIdsInBatch_keepsLastPayload() {
        val payload1 =
            A2uiComponentPayload(
                id = "duplicate_id",
                type = "Text",
                properties = mapOf("text" to "First Draft"),
            )
        val payload2 =
            A2uiComponentPayload(
                id = "duplicate_id",
                type = "Text",
                properties = mapOf("text" to "Final Revision"),
            )

        registry.update(listOf(payload1, payload2))

        val record = registry.get("duplicate_id")
        assertIs<A2uiComponentRecord.Valid>(record)
        assertThat(record.properties.raw["text"]).isEqualTo("Final Revision")
    }

    @Test
    fun update_batchUpdate_appliesAsSingleSnapshot() {
        var applyNotifications = 0
        val unregister = Snapshot.registerApplyObserver { _, _ -> applyNotifications++ }

        try {
            val payload1 = A2uiComponentPayload("comp_1", "Text", emptyMap())
            val payload2 = A2uiComponentPayload("comp_2", "Button", emptyMap())

            registry.update(listOf(payload1, payload2))
            Snapshot.sendApplyNotifications()

            assertThat(applyNotifications).isEqualTo(1)
        } finally {
            unregister.dispose()
        }
    }

    @Test
    fun update_allIdenticalBatch_doesNotTriggerUpdate() {
        val payload1 = A2uiComponentPayload("comp_1", "Text", mapOf("text" to "A"))
        val payload2 = A2uiComponentPayload("comp_2", "Button", emptyMap())
        registry.update(listOf(payload1, payload2))

        var applyNotifications = 0
        val unregister = Snapshot.registerApplyObserver { _, _ -> applyNotifications++ }

        try {
            registry.update(
                listOf(
                    A2uiComponentPayload("comp_1", "Text", mapOf("text" to "A")),
                    A2uiComponentPayload("comp_2", "Button", emptyMap()),
                )
            )

            assertThat(applyNotifications).isEqualTo(0)
        } finally {
            unregister.dispose()
        }
    }

    @Test
    fun reportError_onUnknownComponent_createsErrorRecord() {
        val exception = A2uiRuntimeException("Unknown type 'QuantumMatrix'")

        registry.reportError("hallucinated_id", exception)

        val record = registry.get("hallucinated_id")
        assertIs<A2uiComponentRecord.Error>(record)
        assertThat(record.exception).isEqualTo(exception)
    }

    @Test
    fun reportError_onExistingValidComponent_replacesWithErrorRecord() {
        val payload = A2uiComponentPayload("comp_1", "Text", mapOf("text" to "Hello"))
        registry.update(listOf(payload))
        assertIs<A2uiComponentRecord.Valid>(registry.get("comp_1"))

        val exception = A2uiValidationException("Missing required property", path = "path")
        registry.reportError("comp_1", exception)

        val record = registry.get("comp_1")
        assertIs<A2uiComponentRecord.Error>(record)
        assertThat(record.exception).isEqualTo(exception)
    }

    @Test
    fun reportError_onExistingErrorRecord_updatesRecord() {
        val firstException = A2uiValidationException("Missing 'text'", "path")
        registry.reportError("comp_1", firstException)

        val secondException = A2uiRuntimeException("Invalid type 'Fext'")
        registry.reportError("comp_1", secondException)

        val record = registry.get("comp_1")
        assertIs<A2uiComponentRecord.Error>(record)
        assertThat(record.exception).isEqualTo(secondException)
        assertThat(record.exception).isNotEqualTo(firstException)
    }

    @Test
    fun concurrentlyUpdateAndReportError_handlesBothSafely(): Unit = runBlocking {
        val numCoroutines = 100
        val jobs =
            List(numCoroutines) { index ->
                launch(Dispatchers.Default) {
                    if (index % 2 == 0) {
                        val payload =
                            A2uiComponentPayload(
                                "comp_$index",
                                "Text",
                                mapOf("text" to "Value_$index"),
                            )
                        registry.update(listOf(payload))
                    } else {
                        registry.reportError("comp_$index", A2uiRuntimeException("Error $index"))
                    }
                }
            }

        jobs.joinAll()

        for (i in 0 until numCoroutines) {
            val record = registry.get("comp_$i")
            if (i % 2 == 0) {
                assertIs<A2uiComponentRecord.Valid>(record)
                assertThat(record.properties.raw["text"]).isEqualTo("Value_$i")
            } else {
                assertIs<A2uiComponentRecord.Error>(record)
                assertThat(record.exception.message).isEqualTo("Error $i")
            }
        }
    }

    @Test
    fun concurrentGetAndUpdate_doNotThrowExceptions(): Unit = runBlocking {
        val numCoroutines = 200
        val jobs =
            List(numCoroutines) { index ->
                launch(Dispatchers.Default) {
                    if (index % 2 == 0) {
                        // Writer thread: adding new component records
                        registry.update(
                            listOf(A2uiComponentPayload("comp_$index", "Text", emptyMap()))
                        )
                    } else {
                        // Reader thread: attempting to read both existing and non-existing IDs
                        registry.get("comp_${index - 1}")
                        registry.get("comp_future_$index")
                    }
                }
            }

        jobs.joinAll()

        for (i in 0 until numCoroutines step 2) {
            assertThat(registry.get("comp_$i")).isNotNull()
        }
    }

    @Test
    fun concurrentGetAndUpdate_batchIsAppliedAtomicallyPreventingPartialReads() = runBlocking {
        val registry = A2uiComponentRegistry()
        registry.update(
            listOf(
                A2uiComponentPayload("comp_A", "Text", mapOf("version" to 1)),
                A2uiComponentPayload("comp_B", "Text", mapOf("version" to 1)),
            )
        )

        // Start a reader thread that constantly checks the record version consistency
        val readerJob =
            async(Dispatchers.Default) {
                var successCount = 0
                while (successCount < 1000) {
                    var versionA: Int? = null
                    var versionB: Int? = null

                    // A read-only snapshot so that reads happen within the same snapshot
                    val snapshot = Snapshot.takeSnapshot()
                    try {
                        snapshot.enter {
                            val recordA = registry.get("comp_A") as? A2uiComponentRecord.Valid
                            val recordB = registry.get("comp_B") as? A2uiComponentRecord.Valid
                            versionA = recordA?.properties?.raw?.get("version") as? Int
                            versionB = recordB?.properties?.raw?.get("version") as? Int
                        }
                    } finally {
                        snapshot.dispose()
                    }

                    assertThat(versionA).isEqualTo(versionB)

                    if (versionA == 2) break // The update has successfully propagated
                    successCount++
                }
            }

        // Batch update from another thread
        launch(Dispatchers.Default) {
            registry.update(
                listOf(
                    A2uiComponentPayload("comp_A", "Text", mapOf("version" to 2)),
                    A2uiComponentPayload("comp_B", "Text", mapOf("version" to 2)),
                )
            )
        }

        readerJob.await()
    }

    @Test
    fun concurrentGet_onUnknownId_safelyInitializesWithoutCrashing() = runBlocking {
        val registry = A2uiComponentRegistry()
        val targetId = "future_lazy_comp"

        val jobs = List(100) { async(Dispatchers.Default) { registry.get(targetId) } }

        val results = jobs.awaitAll()

        // Assert all readers received `null` (loading state) without crashing
        assertThat(results).containsExactlyElementsIn(java.util.Collections.nCopies(100, null))

        // Update the component to ensure the underlying MutableState was wired correctly
        registry.update(listOf(A2uiComponentPayload(targetId, "Text", emptyMap())))

        // A subsequent read should reflect the update, proving `getOrPut` shared the right state
        val finalRecord = registry.get(targetId)
        assertIs<A2uiComponentRecord.Valid>(finalRecord)
        assertThat(finalRecord.type).isEqualTo("Text")
    }

    @Test
    fun concurrentMutationsAndDispose_doesNotDeadlock() = runBlocking {
        val registry = A2uiComponentRegistry()
        val numCoroutines = 300

        val jobs =
            List(numCoroutines) { index ->
                launch(Dispatchers.Default) {
                    when (index % 4) {
                        0 -> {
                            // Writer
                            registry.update(
                                listOf(A2uiComponentPayload("comp_$index", "Text", emptyMap()))
                            )
                        }
                        1 -> {
                            // Error reporter
                            registry.reportError("comp_${index - 1}", A2uiRuntimeException("Test"))
                        }
                        2 -> {
                            // Reader
                            registry.get("comp_${index + 1}")
                        }
                        3 -> {
                            // Destroyer: only let the last few coroutines trigger close
                            if (index > 290) {
                                registry.close()
                            }
                        }
                    }
                }
            }

        jobs.joinAll()

        // Ensure the test completes without deadlocking
        assertThat(true).isTrue()
    }

    @OptIn(ExperimentalTestApi::class)
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

    @OptIn(ExperimentalTestApi::class)
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

    @OptIn(ExperimentalTestApi::class)
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

    @OptIn(ExperimentalTestApi::class)
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

    @OptIn(ExperimentalTestApi::class)
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

    @Test
    fun close_clearsRegistry() {
        registry.update(listOf(A2uiComponentPayload("comp_1", "Text", mapOf("text" to "Hello"))))
        assertIs<A2uiComponentRecord.Valid>(registry.get("comp_1"))

        registry.close()

        assertThat(registry.get("comp_1")).isNull()
    }
}
