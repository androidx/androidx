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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.unit.em
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Layout and text fields that may be NaN-encoded float variables, which the embedded player
 * ([RcPlayer]) resolves rather than reading as NaN. Each value is written as `value * 1`, a float
 * expression whose result is only known at playback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerFloatVariableFieldsTest {

    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun rowWeight_variable_splitsMainAxis() {
        val bitmap =
            rule.renderDocument(
                buildDocument {
                    root {
                        startRow(RecordingModifier().fillMaxSize(), RowLayout.START, RowLayout.TOP)
                        startBox(
                            RecordingModifier()
                                .horizontalWeight(variable(3f))
                                .fillMaxHeight()
                                .background(RED)
                        )
                        endBox()
                        startBox(RecordingModifier().horizontalWeight(1f).fillMaxHeight())
                        endBox()
                        endRow()
                    }
                }
            )

        // 3:1 of 100px.
        assertRed(bitmap, 70, 50)
        assertNotRed(bitmap, 80, 50)
    }

    @Test
    fun columnWeight_variable_splitsMainAxis() {
        val bitmap =
            rule.renderDocument(
                buildDocument {
                    root {
                        startColumn(
                            RecordingModifier().fillMaxSize(),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                        )
                        startBox(
                            RecordingModifier()
                                .verticalWeight(variable(3f))
                                .fillMaxWidth()
                                .background(RED)
                        )
                        endBox()
                        startBox(RecordingModifier().verticalWeight(1f).fillMaxWidth())
                        endBox()
                        endColumn()
                    }
                }
            )

        assertRed(bitmap, 50, 70)
        assertNotRed(bitmap, 50, 80)
    }

    @Test
    fun rowWeight_variableResolvingToZero_leavesChildUnweighted() {
        // Compose rejects a weight of 0 or below, which a variable can be before the value it
        // depends on is known. The child is left unweighted instead of throwing.
        val bitmap =
            rule.renderDocument(
                buildDocument {
                    root {
                        startRow(RecordingModifier().fillMaxSize(), RowLayout.START, RowLayout.TOP)
                        startBox(
                            RecordingModifier()
                                .horizontalWeight(floatExpression(0f, 1f, MUL))
                                .width(20f)
                                .fillMaxHeight()
                                .background(RED)
                        )
                        endBox()
                        startBox(RecordingModifier().horizontalWeight(1f).fillMaxHeight())
                        endBox()
                        endRow()
                    }
                }
            )

        assertRed(bitmap, 10, 50)
        assertNotRed(bitmap, 30, 50)
    }

    @Test
    fun coreTextLineHeight_variable_isResolved() {
        val text = "AAAA\nBBBB"
        rule.renderDocument(
            textDocument(text) {
                it.lineHeightMultiplier = variable(1.5f)
                it.lineHeightAdd = variable(4f)
            }
        )

        // Density is 1, so sp == px: 20 * 1.5 + 4.
        assertThat(rule.textLayoutResultOf(text).layoutInput.style.lineHeight.value)
            .isWithin(0.01f)
            .of(34f)
    }

    @Test
    fun coreTextLetterSpacing_variable_isResolved() {
        val text = "Spaced"
        rule.renderDocument(textDocument(text) { it.letterSpacing = variable(0.1f) })

        assertThat(rule.textLayoutResultOf(text).layoutInput.style.letterSpacing).isEqualTo(0.1f.em)
    }

    @Test
    fun contentWidth_subtractsVariablePadding() {
        val bitmap =
            rule.renderDocument(
                buildDocument {
                    root {
                        startColumn(
                            RecordingModifier().fillMaxSize(),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                        )
                        // 100px wide with 10px padding each side, 40px tall.
                        startBox(
                            RecordingModifier().padding(variable(10f)).fillMaxWidth().height(20f),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        )
                        val contentWidth = writer.addComponentContentWidthValue()
                        endBox()
                        startBox(
                            RecordingModifier().width(contentWidth).height(10f).background(RED)
                        )
                        endBox()
                        endColumn()
                    }
                }
            )

        // 100 - 2 * 10, below the 40px padded box.
        assertRed(bitmap, 75, 45)
        assertNotRed(bitmap, 85, 45)
    }

    private class TextFields {
        var fontWeight = 400f
        var letterSpacing = 0f
        var lineHeightAdd = 0f
        var lineHeightMultiplier = 1f
    }

    private fun textDocument(
        text: String,
        fields: RemoteComposeContextAndroid.(TextFields) -> Unit,
    ): CoreDocument = buildDocument {
        val values = TextFields().also { fields(it) }
        val textId = addText(text)
        root {
            startTextComponent(
                RecordingModifier().size(100, 100),
                textId,
                -1,
                0xFF000000.toInt(),
                -1,
                20f,
                -1f,
                -1f,
                0,
                values.fontWeight,
                null,
                CoreText.TEXT_ALIGN_LEFT,
                CoreText.OVERFLOW_CLIP,
                Int.MAX_VALUE,
                values.letterSpacing,
                values.lineHeightAdd,
                values.lineHeightMultiplier,
                0,
                0,
                0,
                false,
                false,
                null,
                null,
                false,
                0,
            )
            endTextComponent()
        }
    }

    private companion object {
        const val RED = 0xFFFF0000.toInt()

        /** [value] as a float expression (`value * 1`), so the field holds a NaN variable id. */
        fun RemoteComposeContextAndroid.variable(value: Float): Float =
            floatExpression(value, 1f, MUL)
    }
}
