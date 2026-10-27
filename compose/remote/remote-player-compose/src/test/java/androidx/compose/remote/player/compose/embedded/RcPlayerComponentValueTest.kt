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

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * How the embedded player publishes measured component values (ComponentValue WIDTH, HEIGHT,
 * POS_ROOT_X, ...) to the expressions, layouts and draws that read them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "mdpi")
class RcPlayerComponentValueTest {

    @get:Rule val rule = RcPlayerTestRule()

    private fun buildDocument(block: RemoteComposeWriter.() -> Unit): CoreDocument =
        buildWriterDocument(
            width = 100,
            height = 100,
            name = "ComponentValueTest",
            profiles = RcProfiles.PROFILE_ANDROIDX,
            block = block,
        )

    /** Starts a box with the [description] content description, sized by [modifier]. */
    private fun RemoteComposeWriter.labeledBox(
        description: String,
        modifier: RecordingModifier,
        horizontal: Int = BoxLayout.START,
        vertical: Int = BoxLayout.TOP,
    ) {
        val descId = addText(description)
        startBox(
            modifier.then { w ->
                CoreSemantics().apply { mContentDescriptionId = descId }.write(w.buffer.buffer)
            },
            horizontal,
            vertical,
        )
    }

    private fun widthOf(description: String): Dp =
        rule.onNodeWithContentDescription(description).getUnclippedBoundsInRoot().width

    /**
     * WIDTH / HEIGHT values declared directly on the document root follow the player's size, both
     * when read directly by a layout and through an expression.
     */
    @Test
    fun rootWidthAndHeight_followPlayerSize() {
        val document = buildDocument {
            root {
                val rootWidth = addComponentWidthValue()
                val rootHeight = addComponentHeightValue()
                val halfWidth = floatExpression(rootWidth, 2f, AnimatedFloatExpression.DIV)
                labeledBox(
                    "Full",
                    RecordingModifier().width(rootWidth).height(rootHeight).background(RED),
                )
                endBox()
                labeledBox("Half", RecordingModifier().width(halfWidth).height(10f))
                endBox()
            }
        }
        var hostWidth by mutableStateOf(100.dp)
        rule.setContent {
            RcPlayer(document = document, modifier = Modifier.size(hostWidth, 80.dp))
        }
        rule.waitForIdle()

        assertThat(widthOf("Full")).isEqualTo(100.dp)
        assertThat(widthOf("Half")).isEqualTo(50.dp)
        val bitmap = rule.captureRootBitmap()
        assertRed(bitmap, 95, 75)

        hostWidth = 60.dp
        rule.waitForIdle()

        assertThat(widthOf("Full")).isEqualTo(60.dp)
        assertThat(widthOf("Half")).isEqualTo(30.dp)
    }

    /** A 1px change of a child's size reaches the layouts that read its WIDTH value. */
    @Test
    fun childWidth_onePixelChanges_updateReaders() {
        val document = buildDocument {
            root {
                startBox(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.TOP)
                val width = addComponentWidthValue()
                labeledBox("Mirror", RecordingModifier().width(width).height(10f))
                endBox()
                endBox()
            }
        }
        var hostWidth by mutableStateOf(100.dp)
        rule.setContent {
            RcPlayer(document = document, modifier = Modifier.size(hostWidth, 50.dp))
        }
        rule.waitForIdle()
        assertThat(widthOf("Mirror")).isEqualTo(100.dp)

        for (width in 101..104) {
            hostWidth = width.dp
            rule.waitForIdle()
            assertWithMessage("Mirror width at host width $width")
                .that(widthOf("Mirror"))
                .isEqualTo(width.dp)
        }
    }

    /**
     * Root-relative positions follow every pixel of a moving component, so they are only published
     * once they moved by more than 2px: a reader keeps the last published position until then.
     */
    @Test
    fun rootRelativePosition_publishedOnceMovedPastThreshold() {
        val document = buildDocument {
            root {
                startBox(RecordingModifier().fillMaxSize(), BoxLayout.END, BoxLayout.TOP)
                startBox(RecordingModifier().size(10), BoxLayout.START, BoxLayout.TOP)
                val rootX = addComponentRootXValue()
                endBox()
                labeledBox("Probe", RecordingModifier().width(rootX).height(10f))
                endBox()
                endBox()
            }
        }
        var hostWidth by mutableStateOf(100.dp)
        rule.setContent {
            RcPlayer(document = document, modifier = Modifier.size(hostWidth, 50.dp))
        }
        rule.waitForIdle()
        assertThat(widthOf("Probe")).isEqualTo(90.dp)

        hostWidth = 102.dp
        rule.waitForIdle()
        assertThat(widthOf("Probe")).isEqualTo(90.dp)

        hostWidth = 103.dp
        rule.waitForIdle()
        assertThat(widthOf("Probe")).isEqualTo(93.dp)
    }

    /**
     * A component that declares a root-relative position and moves 1px per frame (as in a scroll)
     * writes snapshot state only when the published position changes, not on every frame.
     */
    @Test
    fun rootRelativePosition_movingComponent_writesStateOnlyPastThreshold() {
        val written = stateObjectsWrittenWhileMoving(MOVING_FRAMES, declareRootX = true)

        // The box moves 1px per frame, so it publishes on every 3px move. Writing the store on
        // every move as well would write at least MOVING_FRAMES objects.
        assertWithMessage("State objects written while the box moves")
            .that(written)
            .isAtMost((MOVING_FRAMES / 3 + 1) * OBJECTS_PER_PUBLISH)
        assertWithMessage("State objects written while the box moves")
            .that(written)
            .isGreaterThan(0)
    }

    /** Without component values, the same moving box writes no snapshot state at all. */
    @Test
    fun movingComponent_withoutComponentValues_writesNoState() {
        assertThat(stateObjectsWrittenWhileMoving(MOVING_FRAMES, declareRootX = false)).isEqualTo(0)
    }

    /**
     * Plays a document whose 10px box sits at the end of a full-size box, grows the player by 1px
     * per frame for [frames] frames (moving the box 1px per frame), and returns the snapshot state
     * objects the player wrote meanwhile.
     */
    private fun stateObjectsWrittenWhileMoving(frames: Int, declareRootX: Boolean): Int {
        val document = buildDocument {
            root {
                startBox(RecordingModifier().fillMaxSize(), BoxLayout.END, BoxLayout.TOP)
                startBox(RecordingModifier().size(10), BoxLayout.START, BoxLayout.TOP)
                if (declareRootX) addComponentRootXValue()
                endBox()
                endBox()
            }
        }
        val hostWidth = mutableStateOf(100.dp)
        rule.setContent {
            RcPlayer(document = document, modifier = Modifier.size(hostWidth.value, 50.dp))
        }
        rule.waitForIdle()
        return rule.appliedStateObjects(ignored = setOf(hostWidth)) {
            repeat(frames) {
                hostWidth.value += 1.dp
                rule.waitForIdle()
            }
        }
    }

    private companion object {
        val RED = AndroidColor.RED

        const val MOVING_FRAMES = 30

        /** A publish writes the value's reactive state and its backing store entry. */
        const val OBJECTS_PER_PUBLISH = 2
    }
}
