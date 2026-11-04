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

package androidx.compose.remote.player.compose.action

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.collection.intObjectMapOf
import androidx.compose.remote.creation.compose.action.PendingIntentAction
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.rememberPendingIntentAction
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.compose.embedded.LauncherWidgetTestProfiles
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PendingIntentActionHandlerTest {

    @get:Rule val captureRule = RemoteCaptureTestRule()
    @get:Rule val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private data class HostCallback(
        val id: Int? = null,
        val name: String? = null,
        val value: Any? = null,
    )

    /**
     * Captures a clickable box with [action], plays it in [RemoteDocumentPlayer] with
     * [PendingIntentActionHandler] intercepting actions before the host callbacks, clicks the
     * player, and returns any host callback that was not handled as a [PendingIntent].
     */
    private fun playWithHandlerAndClick(
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        action: @Composable @RemoteComposable () -> RemoteAction,
    ): HostCallback? {
        val recorded = runBlocking {
            captureRule.captureRecordedDocument(context = context, profile = profile) {
                RemoteBox(modifier = RemoteModifier.size(100.rdp).clickable(action()))
            }
        }
        val handler = PendingIntentActionHandler(context, recorded.pendingIntents)
        var hostCallback: HostCallback? = null

        composeTestRule.setContent {
            RemoteDocumentPlayer(
                document = recorded.coreDocument,
                documentWidth = 100,
                documentHeight = 100,
                modifier = Modifier.testTag(PLAYER_TAG),
                onAction = { id, metadata ->
                    if (!handler.onAction(id, metadata)) {
                        hostCallback = HostCallback(id = id, value = metadata)
                    }
                },
                onNamedAction = { name, value, _ ->
                    if (!handler.onNamedAction(name, value)) {
                        hostCallback = HostCallback(name = name, value = value)
                    }
                },
            )
        }
        composeTestRule.waitForIdle()
        // Trigger a draw pass so the View-backed player lays out its RemoteCompose components.
        composeTestRule.onRoot().captureToImage()
        composeTestRule.onNodeWithTag(PLAYER_TAG).performClick()
        composeTestRule.waitForIdle()
        return hostCallback
    }

    @Test
    fun playerWithHandler_namedPendingIntentAction_sendsPendingIntentOnly() {
        val pendingIntent = testBroadcastPendingIntent(mutable = false)

        val hostCallback = playWithHandlerAndClick {
            rememberPendingIntentAction { pendingIntent }
        }

        assertThat(hostCallback).isNull()
        assertThat(sentTestBroadcasts()).hasSize(1)
    }

    @Test
    fun playerWithHandler_launcherWidgetIdPendingIntentAction_sendsPendingIntentOnly() {
        val pendingIntent = testBroadcastPendingIntent(mutable = false)

        val hostCallback =
            playWithHandlerAndClick(profile = LauncherWidgetTestProfiles.WIDGETS_V6) {
                rememberPendingIntentAction { pendingIntent }
            }

        assertThat(hostCallback).isNull()
        assertThat(sentTestBroadcasts()).hasSize(1)
    }

    @Test
    fun playerWithHandler_dynamicExtras_fillsInMutablePendingIntent() {
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(TEST_ACTION).setPackage(context.packageName).putExtra("screen", "home"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )

        val hostCallback = playWithHandlerAndClick {
            val count = remember { MutableRemoteInt(5) }
            rememberPendingIntentAction("count" to count.toRemoteString()) { pendingIntent }
        }

        assertThat(hostCallback).isNull()
        val sent = sentTestBroadcasts().single()
        assertThat(sent.getStringExtra("screen")).isEqualTo("home")
        assertThat(sent.getStringExtra("count")).isEqualTo("5")
    }

    @Test
    fun playerWithHandler_nonPendingIntentHostActions_fallThroughToHost() {
        val idCallback = playWithHandlerAndClick {
            hostAction(id = 42, metadata = "payload".rs)
        }
        assertThat(idCallback).isEqualTo(HostCallback(id = 42, value = "payload"))
        assertThat(sentTestBroadcasts()).isEmpty()
    }

    @Test
    fun playerWithHandler_nonPendingIntentNamedAction_fallsThroughToHost() {
        val namedCallback = playWithHandlerAndClick {
            hostAction(name = "refresh".rs, value = "inbox".rs)
        }
        assertThat(namedCallback).isEqualTo(HostCallback(name = "refresh", value = "inbox"))
        assertThat(sentTestBroadcasts()).isEmpty()
    }

    @Test
    fun onAction_unregisteredPendingIntentIndex_returnsFalse() {
        val handler = PendingIntentActionHandler(context, intObjectMapOf())

        assertThat(handler.onAction(id = PendingIntentAction.FIRST_ACTION_ID, metadata = null))
            .isFalse()
        assertThat(sentTestBroadcasts()).isEmpty()
    }

    @Test
    fun onNamedAction_invalidValueOrUnregisteredIndex_returnsFalse() {
        val pendingIntent = testBroadcastPendingIntent(mutable = false)
        val handler = PendingIntentActionHandler(context, intObjectMapOf(0, pendingIntent))

        assertThat(handler.onNamedAction(PendingIntentAction.ACTION_NAME, "notAnInt")).isFalse()
        assertThat(handler.onNamedAction(PendingIntentAction.ACTION_NAME, 1)).isFalse()
        assertThat(handler.onNamedAction("OtherAction", 0)).isFalse()
        assertThat(sentTestBroadcasts()).isEmpty()
    }

    @Test
    fun onAction_canceledPendingIntent_returnsTrueWithoutThrowing() {
        val pendingIntent = testBroadcastPendingIntent(mutable = false)
        pendingIntent.cancel()
        val handler = PendingIntentActionHandler(context, intObjectMapOf(0, pendingIntent))

        assertThat(handler.onAction(id = PendingIntentAction.FIRST_ACTION_ID, metadata = null))
            .isTrue()
        assertThat(sentTestBroadcasts()).isEmpty()
    }

    @Test
    fun fillInIntent_decodesMetadataExtras() {
        val us = '\u001F'
        val gs = '\u001D'
        val rs = '\u001E'
        val fillIn = PendingIntentActionHandler.fillInIntent("tab${us}item${gs}feed${rs}9")

        assertThat(fillIn.getStringExtra("tab")).isEqualTo("feed")
        assertThat(fillIn.getStringExtra("item")).isEqualTo("9")
        assertThat(PendingIntentActionHandler.fillInIntent(null).extras).isNull()
    }

    private fun testBroadcastPendingIntent(mutable: Boolean): PendingIntent {
        val flags =
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            0,
            Intent(TEST_ACTION).setPackage(context.packageName),
            flags,
        )
    }

    private fun sentTestBroadcasts(): List<Intent> =
        shadowOf(context as Application).broadcastIntents.filter { it.action == TEST_ACTION }

    private companion object {
        const val PLAYER_TAG = "remoteDocumentPlayer"
        const val TEST_ACTION = "androidx.compose.remote.player.compose.action.TEST_PENDING_INTENT"
    }
}
