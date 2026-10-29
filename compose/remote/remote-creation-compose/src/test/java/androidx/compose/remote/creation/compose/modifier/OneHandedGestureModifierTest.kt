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

package androidx.compose.remote.creation.compose.modifier

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.gesture.OneHandedGestureShowIndicatorAction
import androidx.compose.remote.creation.compose.gesture.RemoteOneHandedGestureAction
import androidx.compose.remote.creation.compose.gesture.RemoteOneHandedGestureConfiguration
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.remember
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
class OneHandedGestureModifierTest {

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val showIndicator = OneHandedGestureShowIndicatorAction("OneHandedGestureHintCount")

    private val gestureConfiguration =
        RemoteOneHandedGestureConfiguration(RemoteOneHandedGestureAction.Primary, "stopwatch_start")

    @Test
    fun oneHandedGesture_recordsNamedVariables() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteBox(
                    modifier =
                        RemoteModifier.oneHandedGesture(
                            gestureConfiguration = gestureConfiguration,
                            onGestureLabel = "start or pause the stopwatch",
                            showIndicatorDelayMillis = 300L,
                            onGestureAvailable = showIndicator,
                            onGesture = hostAction("startOrPause".rs),
                        )
                ) {}
            }

        assertThat(doc.namedString("OneHandedGesture.Primary.HintCount"))
            .isEqualTo("OneHandedGestureHintCount")
        assertThat(doc.namedString("OneHandedGesture.Primary.Id")).isEqualTo("stopwatch_start")
        assertThat(doc.namedString("OneHandedGesture.Primary.HostAction")).isEqualTo("startOrPause")
        assertThat(doc.namedString("OneHandedGesture.Primary.Label"))
            .isEqualTo("start or pause the stopwatch")
        assertThat(doc.namedFloat("OneHandedGesture.Primary.DelayMillis")).isEqualTo(300f)
    }

    @Test
    fun oneHandedGesture_defaults() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteBox(
                    modifier =
                        RemoteModifier.oneHandedGesture(
                            gestureConfiguration = gestureConfiguration,
                            onGestureAvailable = showIndicator,
                            onGesture = hostAction("startOrPause".rs),
                        )
                ) {}
            }

        assertThat(doc.namedString("OneHandedGesture.Primary.Label")).isEmpty()
        assertThat(doc.namedFloat("OneHandedGesture.Primary.DelayMillis")).isEqualTo(1000f)
    }

    @Test
    fun oneHandedGesture_withoutModifier_recordsNothing() = runTest {
        val doc = captureRule.captureDocument(context) { RemoteBox {} }

        assertThat(doc.namedVariables().keys.filter { it.contains("OneHandedGesture") }).isEmpty()
    }

    @Test
    fun oneHandedGesture_nonHostAction_throws() = runTest {
        val error =
            assertFailsWith<IllegalArgumentException> {
                captureRule.captureDocument(context) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.oneHandedGesture(
                                gestureConfiguration = gestureConfiguration,
                                onGestureAvailable = showIndicator,
                                onGesture = hostAction(7),
                            )
                    ) {}
                }
            }

        assertThat(error).hasMessageThat().contains("host action with a constant name")
    }

    @Test
    fun oneHandedGesture_dynamicHostActionName_throws() = runTest {
        assertFailsWith<IllegalArgumentException> {
            captureRule.captureDocument(context) {
                val name = remember { createNamedRemoteString("action_name", "start") }
                RemoteBox(
                    modifier =
                        RemoteModifier.oneHandedGesture(
                            gestureConfiguration = gestureConfiguration,
                            onGestureAvailable = showIndicator,
                            onGesture = hostAction(name),
                        )
                ) {}
            }
        }
    }

    @Test
    fun oneHandedGesture_onGestureAvailableNotShowIndicator_throws() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                RemoteModifier.oneHandedGesture(
                    gestureConfiguration = gestureConfiguration,
                    onGestureAvailable = hostAction("showHint".rs),
                    onGesture = hostAction("startOrPause".rs),
                )
            }

        assertThat(error).hasMessageThat().contains("showIndicator")
    }

    @Test
    fun oneHandedGesture_negativeDelay_throws() {
        assertFailsWith<IllegalArgumentException> {
            RemoteModifier.oneHandedGesture(
                gestureConfiguration = gestureConfiguration,
                showIndicatorDelayMillis = -1L,
                onGestureAvailable = showIndicator,
                onGesture = hostAction("startOrPause".rs),
            )
        }
    }

    private fun CoreDocument.namedString(name: String): String? {
        val id = namedVariables()["USER:$name"] ?: return null
        return allOperations().filterIsInstance<TextData>().lastOrNull { it.mTextId == id }?.mText
    }

    private fun CoreDocument.namedFloat(name: String): Float? {
        val id = namedVariables()["USER:$name"] ?: return null
        return allOperations().filterIsInstance<FloatConstant>().lastOrNull { it.mId == id }?.mValue
    }

    private fun CoreDocument.namedVariables(): Map<String, Int> =
        allOperations().filterIsInstance<NamedVariable>().associate { it.mVarName to it.mVarId }

    private fun CoreDocument.allOperations(): List<Operation> {
        val result = mutableListOf<Operation>()
        fun collect(ops: List<Operation>) {
            for (op in ops) {
                result.add(op)
                if (op is Container) {
                    collect(op.list)
                }
            }
        }
        collect(operations)
        return result
    }
}
