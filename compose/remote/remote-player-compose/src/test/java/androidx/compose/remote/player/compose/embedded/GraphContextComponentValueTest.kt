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
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
class GraphContextComponentValueTest {

    @get:Rule val rule = RcPlayerTestRule()

    private fun buildDocument(block: RemoteComposeWriter.() -> Unit): CoreDocument =
        buildWriterDocument(
            width = 300,
            height = 100,
            name = "ComponentValueTest",
            apiLevel = 6,
            profiles = RcProfiles.PROFILE_ANDROIDX,
            block = block,
        )

    @Test
    fun graphContextResolvesComponentValueDirectlyAndReactively() {
        val document = buildDocument {
            root {
                startBox(
                    RecordingModifier().fillMaxSize(),
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                val compWidth = addComponentWidthValue()
                NamedVariable.apply(
                    buffer.buffer,
                    Utils.idFromNan(compWidth),
                    NamedVariable.FLOAT_TYPE,
                    "USER:compWidth",
                )
                val descId = addText("MirrorChild")
                startBox(
                    RecordingModifier().width(compWidth).height(40f).then { w ->
                        CoreSemantics()
                            .apply { mContentDescriptionId = descId }
                            .write(w.buffer.buffer)
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                endBox()
                endBox()
            }
        }
        val state = RcPlayerState(document)
        var hostWidth by mutableStateOf(100.dp)
        rule.setContent { RcPlayer(state = state, modifier = Modifier.size(hostWidth, 100.dp)) }
        rule.waitForIdle()

        assertThat(state.floatState("compWidth").value).isEqualTo(100f)
        assertThat(
                rule.onNodeWithContentDescription("MirrorChild").getUnclippedBoundsInRoot().width
            )
            .isEqualTo(100.dp)

        // Mutate component size by resizing the host player
        hostWidth = 250.dp
        rule.waitForIdle()

        assertThat(state.floatState("compWidth").value).isEqualTo(250f)
        assertThat(
                rule.onNodeWithContentDescription("MirrorChild").getUnclippedBoundsInRoot().width
            )
            .isEqualTo(250.dp)
    }

    @Test
    fun expressionOverComponentValueEvaluatesAndUpdatesReactively() {
        val document = buildDocument {
            root {
                startBox(
                    RecordingModifier().fillMaxSize(),
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                val compWidth = addComponentWidthValue()
                val halfWidth = floatExpression(compWidth, 2f, AnimatedFloatExpression.DIV)
                NamedVariable.apply(
                    buffer.buffer,
                    Utils.idFromNan(halfWidth),
                    NamedVariable.FLOAT_TYPE,
                    "USER:halfWidth",
                )
                val descId = addText("HalfChild")
                startBox(
                    RecordingModifier().width(halfWidth).height(40f).then { w ->
                        CoreSemantics()
                            .apply { mContentDescriptionId = descId }
                            .write(w.buffer.buffer)
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                endBox()
                endBox()
            }
        }
        val state = RcPlayerState(document)
        var hostWidth by mutableStateOf(100.dp)
        rule.setContent { RcPlayer(state = state, modifier = Modifier.size(hostWidth, 100.dp)) }
        rule.waitForIdle()

        assertThat(state.floatState("halfWidth").value).isEqualTo(50f)
        assertThat(rule.onNodeWithContentDescription("HalfChild").getUnclippedBoundsInRoot().width)
            .isEqualTo(50.dp)

        // Mutate component size
        hostWidth = 200.dp
        rule.waitForIdle()

        assertThat(state.floatState("halfWidth").value).isEqualTo(100f)
        assertThat(rule.onNodeWithContentDescription("HalfChild").getUnclippedBoundsInRoot().width)
            .isEqualTo(100.dp)
    }

    @Test
    fun componentValueTakesPrecedenceOverStoreFloatOverride() {
        val document = buildDocument {
            root {
                startBox(
                    RecordingModifier().fillMaxSize(),
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                val compWidth = addComponentWidthValue()
                NamedVariable.apply(
                    buffer.buffer,
                    Utils.idFromNan(compWidth),
                    NamedVariable.FLOAT_TYPE,
                    "USER:compWidth",
                )
                endBox()
            }
        }
        val state = RcPlayerState(document)
        state.floatState("compWidth").value = 999f

        rule.setContent { RcPlayer(state = state, modifier = Modifier.size(100.dp, 100.dp)) }
        rule.waitForIdle()

        // Measured component value must take precedence over the store override
        assertThat(state.floatState("compWidth").value).isEqualTo(100f)
    }
}
