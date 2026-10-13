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

package androidx.compose.remote.player.compose.embedded

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.actions.HostAction as CreationHostAction
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Each host action shape, played by the embedded [RcPlayer] and delivered to the host. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerHostActionTest {

    @get:Rule val rule = RcPlayerTestRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private data class Fired(val id: Int? = null, val name: String? = null, val value: Any? = null)

    private fun clickAndCapture(
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        action: @Composable () -> Action,
    ): Fired? {
        val captured = runBlocking {
            captureSingleRemoteDocument(context = context, profile = profile) {
                RemoteBox(modifier = RemoteModifier.size(100.rdp).clickable(action()))
            }
        }
        var fired: Fired? = null
        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                RcPlayer(
                    capturedDocument = captured,
                    onAction = { id, metadata -> fired = Fired(id = id, value = metadata) },
                    onNamedAction = { name, value, _ -> fired = Fired(name = name, value = value) },
                )
            }
        }
        rule.waitForIdle()
        rule.onNode(hasClickAction()).performClick()
        rule.waitForIdle()
        return fired
    }

    @Test
    fun namedAction_noValue() {
        assertThat(clickAndCapture { hostAction("refresh".rs) }).isEqualTo(Fired(name = "refresh"))
    }

    @Test
    fun namedAction_floatValue() {
        assertThat(clickAndCapture { hostAction("volume".rs, 0.5f.rf) })
            .isEqualTo(Fired(name = "volume", value = 0.5f))
    }

    @Test
    fun namedAction_intValue() {
        assertThat(clickAndCapture { hostAction("page".rs, 3.ri) })
            .isEqualTo(Fired(name = "page", value = 3))
    }

    @Test
    fun namedAction_stringValue() {
        assertThat(clickAndCapture { hostAction("open".rs, "inbox".rs) })
            .isEqualTo(Fired(name = "open", value = "inbox"))
    }

    @Test
    fun namedAction_dynamicName_isResolved() {
        assertThat(
                clickAndCapture {
                    hostAction("item_".rs + 7.ri.toRemoteString(), 1.ri)
                }
            )
            .isEqualTo(Fired(name = "item_7", value = 1))
    }

    /** An id action with dynamic metadata, written with the procedural writer. */
    @Test
    fun idActionWithMetadata_deliversResolvedMetadata() {
        val writer = RemoteComposeWriterAndroid(100, 100, "test", AndroidxRcPlatformServices())
        writer.root {
            val metadataId =
                writer.textMerge(writer.addText("screen=home&index="), writer.addText("2"))
            writer.box(
                RecordingModifier()
                    .size(100, 100)
                    .background(0xFFFF0000.toInt())
                    .onClick(CreationHostAction(42, metadataId))
            )
        }
        val document =
            CoreDocument(RemoteClock.SYSTEM).apply {
                ByteArrayInputStream(writer.buffer(), 0, writer.bufferSize()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }

        var fired: Fired? = null
        rule.setContent {
            Box(modifier = Modifier.size(100.dp)) {
                RcPlayer(
                    document = document,
                    onAction = { id, metadata -> fired = Fired(id = id, value = metadata) },
                )
            }
        }
        rule.waitForIdle()
        rule.onNode(hasClickAction()).performClick()
        rule.waitForIdle()

        assertThat(fired).isEqualTo(Fired(id = 42, value = "screen=home&index=2"))
    }

    /** The default encoding, a named action that Wear and other hosts dispatch. */
    @Test
    fun pendingIntentAction_namedAction_sendsPendingIntentWithoutNotifyingHost() {
        val pendingIntent = testPendingIntent()
        val fired = clickAndCapture { pendingIntentAction { pendingIntent } }
        assertThat(fired).isNull()
        assertThat(sentTestBroadcasts()).isEqualTo(1)
    }

    /** The launcher widgets encoding, an id action above every writer-allocated id. */
    @Test
    fun pendingIntentAction_idAction_sendsPendingIntentWithoutNotifyingHost() {
        val pendingIntent = testPendingIntent()
        val fired =
            clickAndCapture(profile = LauncherWidgetTestProfiles.WIDGETS_V6) {
                pendingIntentAction { pendingIntent }
            }
        assertThat(fired).isNull()
        assertThat(sentTestBroadcasts()).isEqualTo(1)
    }

    /**
     * An id action whose id equals an explicit component id. Core's `performClick(id)` (used by the
     * View and platform players) would also click that component; the embedded player must only
     * notify the host, without running the other component's actions.
     */
    @Test
    fun idAction_matchingComponentId_onlyNotifiesHost() {
        val collidingId = 100
        val writer = RemoteComposeWriterAndroid(100, 200, "test", AndroidxRcPlatformServices())
        writer.root {
            writer.column(
                RecordingModifier().size(100, 200),
                ColumnLayout.START,
                ColumnLayout.TOP,
            ) {
                writer.box(
                    RecordingModifier()
                        .size(100, 100)
                        .background(0xFFFF0000.toInt())
                        .onClick(CreationHostAction(collidingId))
                )
                writer.box(
                    RecordingModifier()
                        .componentId(collidingId)
                        .size(100, 100)
                        .background(0xFF0000FF.toInt())
                        .onClick(CreationHostAction("otherComponentClicked"))
                )
            }
        }
        val document =
            CoreDocument(RemoteClock.SYSTEM).apply {
                ByteArrayInputStream(writer.buffer(), 0, writer.bufferSize()).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        assertThat(document.getComponent(collidingId)).isNotNull()

        val actions = mutableListOf<Int>()
        val namedActions = mutableListOf<String>()
        rule.setContent {
            Box(modifier = Modifier.size(100.dp, 200.dp)) {
                RcPlayer(
                    document = document,
                    onAction = { id, _ -> actions += id },
                    onNamedAction = { name, _, _ -> namedActions += name },
                )
            }
        }
        rule.waitForIdle()
        rule.onAllNodes(hasClickAction())[0].performClick()
        rule.waitForIdle()

        assertThat(actions).containsExactly(collidingId)
        assertThat(namedActions).isEmpty()
    }

    private fun testPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(TEST_ACTION).setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )

    private fun sentTestBroadcasts(): Int =
        shadowOf(context as Application).broadcastIntents.count { it.action == TEST_ACTION }

    private companion object {
        const val TEST_ACTION = "androidx.compose.remote.test.HOST_ACTION"
    }
}
