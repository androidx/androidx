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
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaptureRemoteDocumentRecompositionTest {

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
                content = { RemoteText(state.value) },
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
                    RemoteText(text)
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

        // TODO: We fixed duplicate emissions (reducing to 3 docs) by using distinctUntilChanged on
        //  the byte arrays. Investigate if we can use structural changes or track state
        // changes/applier
        //  events to avoid even generating the document byte array when nothing visually changed.
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
                    RemoteText(text)
                },
            )

        // Start background snapshot apply observer loop to handle async writes from Flow collector
        backgroundScope.launch {
            while (true) {
                Snapshot.sendApplyNotifications()
                kotlinx.coroutines.delay(10)
            }
        }

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
}
