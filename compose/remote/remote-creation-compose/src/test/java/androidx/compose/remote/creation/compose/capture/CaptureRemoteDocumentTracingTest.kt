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

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTrace

/**
 * Verifies the Android trace sections emitted during single-document and streaming RemoteCompose
 * captures.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalRemoteCreationComposeApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class CaptureRemoteDocumentTracingTest {

    private var previousEnforceCleanRecomposition: Boolean = false

    @Before
    fun setUp() {
        previousEnforceCleanRecomposition =
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled
        ShadowTrace.setEnabled(true)
        ShadowTrace.reset()
    }

    @After
    fun tearDown() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled =
            previousEnforceCleanRecomposition
        ShadowTrace.setEnabled(false)
        ShadowTrace.reset()
    }

    @Test
    fun captureSingleRemoteDocument_emitsExpectedSyncAndAsyncTraceSections() =
        runTest(UnconfinedTestDispatcher()) {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val document =
                captureSingleRemoteDocument(context = context) {
                    RemoteBox(modifier = RemoteModifier.size(100.rdp))
                }

            assertThat(document.bytes).isNotEmpty()
            assertThat(ShadowTrace.getCurrentSections()).isEmpty()
            assertThat(ShadowTrace.getCurrentAsyncSections()).isEmpty()

            val syncSections = ShadowTrace.getPreviousSections().toList()
            assertThat(syncSections)
                .containsAtLeast(
                    "CaptureRemoteDocument:captureSingleRemoteDocument:setContent",
                    "CaptureRemoteDocument:captureSingleRemoteDocument:rootNodeRender",
                    "CaptureRemoteDocument:captureSingleRemoteDocument:encodeToByteArray",
                )
                .inOrder()

            val asyncSections = ShadowTrace.getPreviousAsyncSections().map { it.sectionName }
            assertThat(asyncSections)
                .containsAtLeast(
                    "CaptureRemoteDocument:captureSingleRemoteDocument",
                    "CaptureRemoteDocument:captureSingleRemoteDocument:settle",
                )
        }

    @Test
    fun captureRemoteDocument_emitsExpectedTraceSectionsAcrossInitialAndSubsequentRenders() =
        runTest {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
            val context = ApplicationProvider.getApplicationContext<Context>()
            val sizeDp = mutableIntStateOf(50)
            val emitted = mutableListOf<ByteArray>()

            val collector = launch {
                captureRemoteDocument(
                        context = context,
                        creationDisplayInfo = createCreationDisplayInfo(context),
                        updateThrottle = CaptureUpdateThrottle.None,
                        coroutineContext = EmptyCoroutineContext,
                    ) {
                        RemoteBox(modifier = RemoteModifier.size(sizeDp.intValue.rdp))
                    }
                    .collect { emitted += it }
            }
            runCurrent()
            assertThat(emitted).hasSize(1)

            val initialSyncSections = ShadowTrace.getPreviousSections().toList()
            assertThat(initialSyncSections)
                .containsAtLeast(
                    "CaptureRemoteDocument:captureRemoteDocument:setContent",
                    "CaptureRemoteDocument:captureRemoteDocument:rootNodeRender",
                    "CaptureRemoteDocument:captureRemoteDocument:encodeToByteArray",
                )
                .inOrder()
            val initialAsyncSections = ShadowTrace.getPreviousAsyncSections().map { it.sectionName }
            assertThat(initialAsyncSections)
                .contains("CaptureRemoteDocument:captureRemoteDocument:settle")
            assertThat(initialAsyncSections)
                .doesNotContain("CaptureRemoteDocument:captureSingleRemoteDocument")

            ShadowTrace.reset()

            Snapshot.withMutableSnapshot { sizeDp.intValue = 100 }
            runCurrent()
            assertThat(emitted).hasSize(2)

            val updateSyncSections = ShadowTrace.getPreviousSections().toList()
            assertThat(updateSyncSections)
                .doesNotContain("CaptureRemoteDocument:captureRemoteDocument:setContent")
            assertThat(updateSyncSections)
                .containsAtLeast(
                    "CaptureRemoteDocument:captureRemoteDocument:rootNodeRender",
                    "CaptureRemoteDocument:captureRemoteDocument:encodeToByteArray",
                )
                .inOrder()
            val updateAsyncSections = ShadowTrace.getPreviousAsyncSections().map { it.sectionName }
            assertThat(updateAsyncSections)
                .contains("CaptureRemoteDocument:captureRemoteDocument:settle")

            collector.cancel()
            runCurrent()
            assertThat(ShadowTrace.getCurrentSections()).isEmpty()
            assertThat(ShadowTrace.getCurrentAsyncSections()).isEmpty()
        }
}
