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

package androidx.compose.remote.creation.compose.gesture

import android.content.Context
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.key
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteOneHandedGestureConfigurationTest {

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun explicitGestureId_isKept() = runTest {
        var configuration: RemoteOneHandedGestureConfiguration? = null

        captureRule.captureDocument(context) {
            configuration =
                rememberRemoteOneHandedGestureConfiguration(
                    action = RemoteOneHandedGestureAction.Primary,
                    gestureId = "stopwatch_start",
                )
            RemoteBox {}
        }

        assertThat(configuration)
            .isEqualTo(
                RemoteOneHandedGestureConfiguration(
                    RemoteOneHandedGestureAction.Primary,
                    "stopwatch_start",
                )
            )
    }

    @Test
    fun generatedGestureId_isStableAcrossDocuments() = runTest {
        val gestureIds = mutableListOf<String>()

        repeat(2) {
            captureRule.captureDocument(context) {
                gestureIds +=
                    rememberRemoteOneHandedGestureConfiguration(
                            RemoteOneHandedGestureAction.Primary
                        )
                        .gestureId
                RemoteBox {}
            }
        }

        assertThat(gestureIds).hasSize(2)
        assertThat(gestureIds[0]).endsWith("01")
        assertThat(gestureIds[1]).isEqualTo(gestureIds[0])
    }

    @Test
    fun generatedGestureId_dependsOnPositionInComposition() = runTest {
        val gestureIds = mutableListOf<String>()

        for (position in 1..2) {
            captureRule.captureDocument(context) {
                key(position) {
                    gestureIds +=
                        rememberRemoteOneHandedGestureConfiguration(
                                RemoteOneHandedGestureAction.Primary
                            )
                            .gestureId
                }
                RemoteBox {}
            }
        }

        assertThat(gestureIds).hasSize(2)
        assertThat(gestureIds[1]).isNotEqualTo(gestureIds[0])
    }

    @Test
    fun generateGestureId_isHashCodeInBase36FollowedByTwoDigitAction() {
        val gestureId = generateGestureId(35L, RemoteOneHandedGestureAction.Primary)

        assertThat(gestureId).isEqualTo("z01")
    }

    @Test
    fun secondConfigurationForSameAction_throws() = runTest {
        val error =
            assertFailsWith<IllegalStateException> {
                captureRule.captureDocument(context) {
                    rememberRemoteOneHandedGestureConfiguration(
                        RemoteOneHandedGestureAction.Primary,
                        "first",
                    )
                    rememberRemoteOneHandedGestureConfiguration(
                        RemoteOneHandedGestureAction.Primary,
                        "second",
                    )
                    RemoteBox {}
                }
            }

        assertThat(error).hasMessageThat().isEqualTo("Only one Primary gesture hint per document")
    }

    @Test
    fun configurationsInSeparateDocuments_doNotConflict() = runTest {
        val gestureIds = mutableListOf<String>()

        for (gestureId in listOf("first", "second")) {
            captureRule.captureDocument(context) {
                gestureIds +=
                    rememberRemoteOneHandedGestureConfiguration(
                            RemoteOneHandedGestureAction.Primary,
                            gestureId,
                        )
                        .gestureId
                RemoteBox {}
            }
        }

        assertThat(gestureIds).containsExactly("first", "second").inOrder()
    }

    @Test
    fun names_areScopedByAction() {
        val action = RemoteOneHandedGestureAction.Primary

        assertThat(OneHandedGestureNames.PrimaryHintCount).isEqualTo("OneHandedGestureHintCount")
        assertThat(OneHandedGestureNames.hintCount(action))
            .isEqualTo("OneHandedGesture.Primary.HintCount")
        assertThat(OneHandedGestureNames.id(action)).isEqualTo("OneHandedGesture.Primary.Id")
        assertThat(OneHandedGestureNames.hostAction(action))
            .isEqualTo("OneHandedGesture.Primary.HostAction")
        assertThat(OneHandedGestureNames.label(action)).isEqualTo("OneHandedGesture.Primary.Label")
        assertThat(OneHandedGestureNames.delayMillis(action))
            .isEqualTo("OneHandedGesture.Primary.DelayMillis")
    }
}
