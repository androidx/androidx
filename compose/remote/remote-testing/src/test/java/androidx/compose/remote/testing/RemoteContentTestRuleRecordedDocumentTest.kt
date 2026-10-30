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

package androidx.compose.remote.testing

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.testing.RemoteBaseContentTestRule.Creation
import androidx.compose.remote.testing.RemoteBaseContentTestRule.Player
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteContentTestRuleRecordedDocumentTest {
    @get:Rule val remoteContentTestRule = RemoteContentTestRule()

    @Test
    fun setContent_passesCreatedDocumentToPlayer() {
        val baseTestRule = remoteContentTestRule.remoteBaseContentTestRule
        val document =
            RecordedDocument(bytes = byteArrayOf(1, 2, 3), profile = RcPlatformProfiles.ANDROIDX)
        val player = RecordingPlayer()

        baseTestRule.setContent(creation = FixedCreation(document), player = player, size = SIZE) {}
        baseTestRule.waitForIdle()

        assertThat(player.playedDocument).isSameInstanceAs(document)
    }

    @Test
    fun remoteContentTestRule_setContent_passesCapturedDocumentToPlayerAndCallbacks() {
        var onCreateDocument: CoreDocument? = null
        var onCoreDocumentCreatedDocument: CoreDocument? = null
        val player = RecordingPlayer()

        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(width = 100, height = 100, densityDpi = 160),
            onCreate = { onCreateDocument = it },
            onCoreDocumentCreated = { onCoreDocumentCreatedDocument = it },
            player = player,
        ) {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Red.rc)) {}
        }
        remoteContentTestRule.composeTestRule.waitUntil { player.playedDocument != null }

        val playedDocument = checkNotNull(player.playedDocument)
        assertThat(playedDocument.bytes).isNotEmpty()
        assertThat(playedDocument.coreDocument.operations).isNotEmpty()
        assertThat(onCreateDocument).isSameInstanceAs(playedDocument.coreDocument)
        assertThat(onCoreDocumentCreatedDocument).isSameInstanceAs(playedDocument.coreDocument)
    }

    private class FixedCreation(private val document: RecordedDocument) : Creation {
        @Composable
        override fun rememberRemoteDocument(
            composable: @RemoteComposable @Composable () -> Unit
        ): MutableState<RecordedDocument?> = remember { mutableStateOf(document) }
    }

    private class RecordingPlayer : Player {
        var playedDocument: RecordedDocument? = null

        @Composable
        override fun Play(document: RecordedDocument, size: Size) {
            playedDocument = document
        }
    }

    private companion object {
        val SIZE = Size(100f, 100f)
    }
}
