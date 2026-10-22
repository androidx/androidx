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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.operations.FloatConstant
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
class RcPlayerPreprocessTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun componentScopedNamedVariable_resolvesThroughRcPlayerState() {
        val doc = buildDocument {
            root {
                startBox(
                    RecordingModifier().fillMaxSize(),
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                val compWidth = writer.addComponentWidthValue()
                NamedVariable.apply(
                    writer.buffer.buffer,
                    Utils.idFromNan(compWidth),
                    NamedVariable.FLOAT_TYPE,
                    "USER:boxWidth",
                )
                endBox()
            }
        }
        val state = RcPlayerState(doc)
        rule.setContent { RcPlayer(state = state, modifier = Modifier.size(120.dp, 80.dp)) }
        rule.waitForIdle()

        assertThat(state.floatState("boxWidth").value).isEqualTo(120f)
    }

    @Test
    fun loopBodyConstants_areNotPrematurelyAppliedDuringSetup() {
        val targetId = 90
        val loopIndexId = 91
        val doc = buildDocument {
            val wire = writer.buffer.buffer
            FloatConstant.apply(wire, targetId, 5f)
            NamedVariable.apply(wire, targetId, NamedVariable.FLOAT_TYPE, "USER:loopTarget")
            writer.buffer.addLoopStart(loopIndexId, 0f, 1f, 3f)
            FloatConstant.apply(wire, targetId, 777f)
            writer.buffer.addLoopEnd()
        }

        val state = RcPlayerState(doc)

        // Setup must retain the top-level constant (5f) rather than hoisting the loop body's
        // FloatConstant (777f) into setup-time constantOps.
        assertThat(state.floatState("loopTarget").value).isEqualTo(5f)
    }

    @Test
    fun namedVariableFirstReferencedInsideLoop_isRegisteredDuringSetup() {
        val varId = 92
        val loopIndexId = 93
        val doc = buildDocument {
            val wire = writer.buffer.buffer
            FloatConstant.apply(wire, varId, 3.5f)
            writer.buffer.addLoopStart(loopIndexId, 0f, 1f, 3f)
            NamedVariable.apply(wire, varId, NamedVariable.FLOAT_TYPE, "USER:loopNamed")
            writer.buffer.addLoopEnd()
        }

        val state = RcPlayerState(doc)

        // NamedVariable only registers the name-to-ID mapping without mutating values, so
        // declarations emitted inside a loop body (such as lazy DSL expressions first referenced
        // inside loop { ... }) must still be registered during setup before the first frame draw.
        assertThat(state.floatState("loopNamed").value).isEqualTo(3.5f)
    }
}
