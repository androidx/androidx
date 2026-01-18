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

package androidx.compose.remote.creation.compose.action

import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.Action as CoreAction
import androidx.compose.remote.creation.compose.capture.NoRemoteCompose
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CombinedActionTest {
    val testStateScope = NoRemoteCompose()

    @Test
    fun toRemoteAction_delegatesToChildren() {
        val recordedWrites = mutableListOf<String>()

        val action1 =
            object : Action {
                override fun RemoteStateScope.toRemoteAction(): CoreAction = CoreAction {
                    recordedWrites.add("action1")
                }
            }

        val action2 =
            object : Action {
                override fun RemoteStateScope.toRemoteAction(): CoreAction = CoreAction {
                    recordedWrites.add("action2")
                }
            }

        val combinedAction = CombinedAction(action1, action2)
        val remoteAction = with(combinedAction) { testStateScope.toRemoteAction() }

        // Use a real writer
        val writer =
            RemoteComposeWriter(
                CreationDisplayInfo(100, 100, 160),
                null,
                RcPlatformProfiles.ANDROIDX,
                null,
            )

        remoteAction.write(writer)

        assertThat(recordedWrites).containsExactly("action1", "action2").inOrder()
    }
}
