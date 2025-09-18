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

package androidx.compose.remote.frontend.action

import android.app.PendingIntent
import android.content.Intent
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.frontend.action.PendingIntentAction.Companion.ACTION_NAME
import androidx.compose.remote.frontend.capture.PendingIntentAwareWriter
import androidx.compose.remote.frontend.capture.RemoteComposeCreationState
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
            RemoteComposeCreationState(
                platform = AndroidxPlatformServices(),
                density = 1f,
                size = Size(1f, 1f),
            )
        val testAction = PendingIntentAction(creationState, testPendingIntent)

        assertThrows(IllegalStateException::class.java) { testAction.toRemoteAction() }
    }

    @Test
    fun toRemoteAction_withPendingIntentAwareWriter_storeSuccessfully() {
        val pendingIntents: MutableList<PendingIntent> = mutableListOf()
        val creationState =
            RemoteComposeCreationState(
                density = 1f,
                size = Size(1f, 1f),
                profile = PendingIntentAwareProfile(pendingIntents),
            )

        val testAction = PendingIntentAction(creationState, testPendingIntent)
        val remoteAction = testAction.toRemoteAction()

        assertThat(pendingIntents.size).isEqualTo(1)
        assertThat(pendingIntents[0]).isEqualTo(testPendingIntent)
        assertThat(remoteAction is HostAction).isTrue()
        assertThat((remoteAction as HostAction).toString()).contains("mActionName='${ACTION_NAME}'")
    }
}

private class PendingIntentAwareProfile(val pendingIntents: MutableList<PendingIntent>) :
    Profile(
        CoreDocument.DOCUMENT_API_LEVEL,
        Operations.PROFILE_ANDROIDX,
        AndroidxPlatformServices(),
        { width, height, contentDescription, profile ->
            object :
                RemoteComposeWriter(
                    width,
                    height,
                    contentDescription,
                    CoreDocument.DOCUMENT_API_LEVEL,
                    Operations.PROFILE_ANDROIDX,
                    profile.platform,
                ),
                PendingIntentAwareWriter {

                override fun storePendingIntent(pendingIntent: PendingIntent): Int {
                    pendingIntents.add(pendingIntent)
                    return pendingIntents.size - 1
                }
            }
        },
    )
