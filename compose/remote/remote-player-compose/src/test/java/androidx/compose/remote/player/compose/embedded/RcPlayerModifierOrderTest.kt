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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.DrawContentOperation
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.layout.modifiers.RoundedClipRectModifierOperation
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerModifierOrderTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun testDrawBeforePadding() {
        // Draw content BEFORE padding in the wire modifier list
        val elements =
            renderAndCollectBoxModifierTypes(
                { DrawContentOperation.apply(it) },
                { PaddingModifierOperation.apply(it, 10f, 10f, 10f, 10f) },
            )

        assertThat(elements).containsExactly("DrawWithContent", "Padding").inOrder()
    }

    @Test
    fun testPaddingBeforeDraw() {
        // Padding BEFORE draw content in the wire modifier list
        val elements =
            renderAndCollectBoxModifierTypes(
                { PaddingModifierOperation.apply(it, 10f, 10f, 10f, 10f) },
                { DrawContentOperation.apply(it) },
            )

        assertThat(elements).containsExactly("Padding", "DrawWithContent").inOrder()
    }

    @Test
    fun testDrawContentProcessedOnce() {
        val elements =
            renderAndCollectBoxModifierTypes({
                PaddingModifierOperation.apply(it, 10f, 10f, 10f, 10f)
            })

        assertThat(elements.count { it == "DrawWithContent" }).isEqualTo(1)
    }

    @Test
    fun testRoundedClipHoistedBeforeDrawContent() {
        // DrawContent precedes RoundedClipRect in wire modifier list; clip must be hoisted before
        // DrawWithContent so it clips the background.
        val elements =
            renderAndCollectBoxModifierTypes(
                { DrawContentOperation.apply(it) },
                { RoundedClipRectModifierOperation.apply(it, 10f, 10f, 10f, 10f) },
            )

        assertThat(elements).containsExactly("Clip", "DrawWithContent").inOrder()
    }

    @Test
    fun testPaddingBeforeRoundedClipWithoutPriorDrawContent() {
        // Padding before RoundedClipRect without prior DrawContent
        val elements =
            renderAndCollectBoxModifierTypes(
                { PaddingModifierOperation.apply(it, 10f, 10f, 10f, 10f) },
                { RoundedClipRectModifierOperation.apply(it, 10f, 10f, 10f, 10f) },
            )

        assertThat(elements).containsExactly("Padding", "Clip", "DrawWithContent").inOrder()
    }

    private fun renderAndCollectBoxModifierTypes(
        vararg modifiers: (WireBuffer) -> Unit
    ): List<String> {
        val document = buildDocument {
            val descId = addText("target")
            root {
                startBox(
                    RecordingModifier().width(100f).height(100f).then { w ->
                        CoreSemantics()
                            .apply { mContentDescriptionId = descId }
                            .write(w.buffer.buffer)
                        modifiers.forEach { writeOp -> writeOp(w.buffer.buffer) }
                        w.startCanvasOperations()
                        w.drawRect(0f, 0f, 50f, 50f)
                        w.endCanvasOperations()
                    },
                    BoxLayout.START,
                    BoxLayout.TOP,
                )
                endBox()
            }
        }

        rule.setContent { Box(Modifier.size(100.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()

        val node = rule.onNodeWithContentDescription("target").fetchSemanticsNode()
        return node.layoutInfo.getModifierInfo().mapNotNull { info ->
            val className = info.modifier.javaClass.name
            when {
                className.contains("DrawWithContent") -> "DrawWithContent"
                className.contains("Padding") -> "Padding"
                className.contains("GraphicsLayer") || className.contains("Clip") -> "Clip"
                else -> null
            }
        }
    }
}
