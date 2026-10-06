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

@file:OptIn(androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@Repeat(5)
class CaptureRemoteDocumentRecompositionTest {
    @get:Rule val repeatRule = RepeatRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
    }

    @After
    fun tearDown() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
    }

    @Test
    fun testRecompositionEmitsNewDocuments() = runTest {
        val state = mutableStateOf("Initial")

        val flow =
            captureRemoteDocument(
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                writerEvents = WriterEvents(),
                context = context,
                coroutineContext = coroutineContext,
                content = { RemoteText(state.value.rs) },
            )

        val documents = mutableListOf<ByteArray>()
        val job = launch { flow.take(2).toList(documents) }

        // Allow initial composition and emission to complete
        testScheduler.advanceUntilIdle()

        // Update state to trigger recomposition
        Snapshot.withMutableSnapshot { state.value = "Updated" }
        Snapshot.sendApplyNotifications()
        testScheduler.advanceUntilIdle()

        job.join()

        assertThat(documents).hasSize(2)

        val doc1 = RemoteDocument(documents[0])
        val doc2 = RemoteDocument(documents[1])

        val hierarchy1 = doc1.document.displayHierarchy()
        val hierarchy2 = doc2.document.displayHierarchy()

        assertThat(hierarchy1).contains("Initial")
        assertThat(hierarchy2).contains("Updated")
        assertThat(hierarchy1).isNotEqualTo(hierarchy2)
    }

    @Test
    fun testRecompositionWithLaunchedEffect() = runTest {
        val keyState = mutableStateOf(0)

        val flow =
            captureRemoteDocument(
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                writerEvents = WriterEvents(),
                context = context,
                coroutineContext = coroutineContext,
                content = {
                    var text by remember { mutableStateOf("Initial") }
                    LaunchedEffect(keyState.value) {
                        if (keyState.value > 0) {
                            text = "Updated ${keyState.value}"
                        }
                    }
                    RemoteText(text.rs)
                },
            )

        val documents = mutableListOf<ByteArray>()
        val job = launch { flow.take(3).toList(documents) }

        testScheduler.advanceUntilIdle()

        // Trigger LaunchedEffect by changing key to 1
        Snapshot.withMutableSnapshot { keyState.value = 1 }
        Snapshot.sendApplyNotifications()
        testScheduler.advanceUntilIdle()

        // Trigger LaunchedEffect by changing key to 2
        Snapshot.withMutableSnapshot { keyState.value = 2 }
        Snapshot.sendApplyNotifications()
        testScheduler.advanceUntilIdle()

        job.join()

        assertThat(documents).hasSize(3)
        val doc1 = RemoteDocument(documents[0])
        val doc2 = RemoteDocument(documents[1])
        val doc3 = RemoteDocument(documents[2])

        // Renders are skipped unless the node tree or a state read while rendering changed, and
        // byte-identical documents are de-duplicated, so each distinct text emits exactly once.
        assertThat(doc1.document.displayHierarchy()).contains("Initial")
        assertThat(doc2.document.displayHierarchy()).contains("Updated 1")
        assertThat(doc3.document.displayHierarchy()).contains("Updated 2")
    }

    @Test
    fun testRecompositionWithCollectAsState() = runTest {
        val stateFlow = MutableStateFlow("Initial")

        val flow =
            captureRemoteDocument(
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                writerEvents = WriterEvents(),
                context = context,
                coroutineContext = coroutineContext,
                content = {
                    val text by stateFlow.collectAsState()
                    RemoteText(text.rs)
                },
            )

        // No manual Snapshot.sendApplyNotifications() loop: captureRemoteDocument's
        // SnapshotWriteMonitor must deliver the collector's global write by itself.
        val documents = mutableListOf<ByteArray>()
        val job = launch { flow.take(2).toList(documents) }

        testScheduler.advanceUntilIdle()

        // Update StateFlow to trigger recomposition
        stateFlow.value = "Updated"
        testScheduler.advanceUntilIdle()

        job.join()

        assertThat(documents).hasSize(2)
        val doc1 = RemoteDocument(documents[0])
        val doc2 = RemoteDocument(documents[1])

        assertThat(doc1.document.displayHierarchy()).contains("Initial")
        assertThat(doc2.document.displayHierarchy()).contains("Updated")
    }

    /**
     * A `RemoteCanvas` draw lambda runs at render time, so a state read only there never causes a
     * composition change. The render-skip logic must still re-render when that state changes.
     */
    @Test
    fun testStateReadOnlyWhileRenderingEmitsNewDocument() = runTest {
        val fill = mutableStateOf(Color.Red)
        val renders = AtomicInteger()

        val flow =
            captureRemoteDocument(
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                context = context,
                coroutineContext = coroutineContext,
                content = {
                    RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                        renders.incrementAndGet()
                        drawRect(paint = RemotePaint { color = fill.value.rc })
                    }
                },
            )

        val documents = mutableListOf<ByteArray>()
        val job = launch { flow.take(2).toList(documents) }
        testScheduler.advanceUntilIdle()
        assertThat(documents).hasSize(1)

        Snapshot.withMutableSnapshot { fill.value = Color.Blue }
        Snapshot.sendApplyNotifications()
        testScheduler.advanceUntilIdle()

        assertThat(documents).hasSize(2)
        job.join()
        assertThat(documents[1]).isNotEqualTo(documents[0])
        assertThat(renders.get()).isEqualTo(2)
    }

    /**
     * A global snapshot write that neither composition nor rendering reads must not cost a render.
     * Byte de-duplication would hide an extra render from the emitted documents, so the render
     * count is asserted directly.
     */
    @Test
    fun testUnrelatedGlobalWriteDoesNotRender() = runTest {
        val unrelated = mutableStateOf(0)
        val renders = AtomicInteger()

        val flow =
            captureRemoteDocument(
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                context = context,
                coroutineContext = coroutineContext,
                content = {
                    RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                        renders.incrementAndGet()
                        drawRect(paint = RemotePaint { color = Color.Red.rc })
                    }
                },
            )

        val documents = mutableListOf<ByteArray>()
        val job = launch { flow.toList(documents) }
        testScheduler.advanceUntilIdle()
        assertThat(documents).hasSize(1)
        assertThat(renders.get()).isEqualTo(1)

        repeat(3) {
            Snapshot.withMutableSnapshot { unrelated.value++ }
            Snapshot.sendApplyNotifications()
            testScheduler.advanceUntilIdle()
        }

        assertThat(renders.get()).isEqualTo(1)
        assertThat(documents).hasSize(1)
        job.cancel()
    }

    /**
     * A recomposition whose output is identical applies no changes to the node tree, so the render
     * is skipped.
     */
    @Test
    fun testRecompositionWithoutAppliedChangesDoesNotRender() = runTest {
        val trigger = mutableStateOf(0)
        val compositions = AtomicInteger()
        val renders = AtomicInteger()

        val flow =
            captureRemoteDocument(
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                context = context,
                coroutineContext = coroutineContext,
                content = {
                    // Read in composition so that writes recompose this scope, without feeding
                    // any node.
                    trigger.value
                    compositions.incrementAndGet()
                    val modifier = remember { RemoteModifier.fillMaxSize() }
                    RemoteCanvas(modifier = modifier) {
                        renders.incrementAndGet()
                        drawRect(paint = RemotePaint { color = Color.Red.rc })
                    }
                },
            )

        val documents = mutableListOf<ByteArray>()
        val job = launch { flow.toList(documents) }
        testScheduler.advanceUntilIdle()
        assertThat(renders.get()).isEqualTo(1)
        val initialCompositions = compositions.get()

        Snapshot.withMutableSnapshot { trigger.value++ }
        Snapshot.sendApplyNotifications()
        testScheduler.advanceUntilIdle()

        assertThat(compositions.get()).isGreaterThan(initialCompositions)
        assertThat(renders.get()).isEqualTo(1)
        assertThat(documents).hasSize(1)
        job.cancel()
    }
}
