/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose.action

import android.app.PendingIntent
import android.content.Intent
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.action.PendingIntentAction.Companion.ACTION_NAME
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.WriterEvents
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PendingIntentActionTest {
    val testPendingIntent: PendingIntent =
        PendingIntent.getActivity(
            ApplicationProvider.getApplicationContext(),
            1234,
            Intent(),
            PendingIntent.FLAG_IMMUTABLE,
        )

    @Test
    fun toRemoteAction_withDefaultRemoteComposeWriter_throws() {
        val creationState =
            RemoteComposeCreationState(platform = AndroidxRcPlatformServices(), size = Size(1f, 1f))
        val testAction = PendingIntentAction(testPendingIntent)

        assertThrows(IllegalStateException::class.java) {
            with(testAction) { creationState.toRemoteAction() }
        }
    }

    @Test
    fun toRemoteAction_withPendingIntentAwareWriter_storeSuccessfully() {
        val writerEvents = WriterEvents()
        val creationState =
            RemoteComposeCreationState(
                creationDisplayInfo = CreationDisplayInfo(1, 1, 160),
                profile = RcPlatformProfiles.ANDROIDX,
                writerEvents = writerEvents,
                layoutDirection = LayoutDirection.Ltr,
            )

        val testAction = PendingIntentAction(testPendingIntent)
        val remoteAction = with(testAction) { creationState.toRemoteAction() }

        val pendingIntents = writerEvents.pendingIntents
        assertThat(pendingIntents.size).isEqualTo(1)
        assertThat(pendingIntents[0]).isEqualTo(testPendingIntent)
        assertThat(remoteAction is HostAction).isTrue()
        assertThat((remoteAction as HostAction).toString()).contains("mActionName='${ACTION_NAME}'")
    }
}
