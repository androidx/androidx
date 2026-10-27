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
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Layout fields bound directly to a system variable resolve to that variable's value, for every
 * system id, matching how the core operations resolve any NaN-encoded field.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
class RcPlayerSystemVariableBindingTest {

    @get:Rule val rule = RcPlayerTestRule()

    private fun RemoteComposeWriter.labeledBox(description: String, modifier: RecordingModifier) {
        val descId = addText(description)
        startBox(
            modifier.then { w ->
                CoreSemantics().apply { mContentDescriptionId = descId }.write(w.buffer.buffer)
            },
            BoxLayout.START,
            BoxLayout.TOP,
        )
    }

    private fun render(block: RemoteComposeWriter.() -> Unit) {
        val document =
            buildWriterDocument(
                width = 100,
                height = 100,
                profiles = RcProfiles.PROFILE_ANDROIDX,
                block = block,
            )
        rule.setContent { RcPlayer(document = document, modifier = Modifier.size(100.dp)) }
        rule.waitForIdle()
    }

    private fun boundsOf(description: String) =
        rule.onNodeWithContentDescription(description).getUnclippedBoundsInRoot()

    private fun widthOf(description: String): Dp = boundsOf(description).width

    /** The player publishes the font size as 14sp in px: 14px at mdpi and font scale 1. */
    private val fontSizePx = 14f

    // ID_FONT_SIZE (33) lies in the 10..40 range that Utils.isVariable reports as "not a variable".
    private val fontSize = Utils.asNan(RemoteContext.ID_FONT_SIZE)

    @Test
    fun width_boundToFontSize_usesFontSize() {
        render {
            root {
                labeledBox("Probe", RecordingModifier().width(fontSize).height(10f))
                endBox()
            }
        }

        assertThat(widthOf("Probe")).isEqualTo(fontSizePx.dp)
    }

    @Test
    fun padding_boundToFontSize_insetsByFontSize() {
        render {
            root {
                startBox(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.TOP)
                startBox(
                    RecordingModifier().padding(fontSize, fontSize, 0f, 0f),
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                labeledBox("Inner", RecordingModifier().size(10))
                endBox()
                endBox()
                endBox()
            }
        }

        val bounds = boundsOf("Inner")
        assertThat(bounds.left).isEqualTo(fontSizePx.dp)
        assertThat(bounds.top).isEqualTo(fontSizePx.dp)
        assertThat(bounds.height).isEqualTo(10.dp)
    }

    @Test
    fun rowSpacing_boundToFontSize_spacesByFontSize() {
        render {
            root {
                startRow(
                    RecordingModifier().fillMaxSize().spacedBy(fontSize),
                    RowLayout.START,
                    RowLayout.TOP,
                )
                labeledBox("First", RecordingModifier().size(10))
                endBox()
                labeledBox("Second", RecordingModifier().size(10))
                endBox()
                endRow()
            }
        }

        assertThat(boundsOf("Second").left - boundsOf("First").right).isEqualTo(fontSizePx.dp)
    }
}
