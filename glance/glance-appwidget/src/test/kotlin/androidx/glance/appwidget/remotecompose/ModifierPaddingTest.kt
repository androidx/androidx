/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.glance.appwidget.remotecompose

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.WireBuffer
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.PaddingModifierOperation
import androidx.compose.remote.core.operations.utilities.StringSerializer
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.debugPrintDoc
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.getSimpleLeaf
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.runAndTranslateSingleRoot
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests that glance's padding modifier translates to Remote Compose's padding modifier. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ModifierPaddingTest : BaseRemoteComposeTest() {

    @Test
    fun translateBox_horizontalPadding() =
        fakeCoroutineScope.runTest {
            val horizontal = 21

            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(modifier = GlanceModifier.padding(horizontal = horizontal.dp)) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            assertModifierStringContainsSubstring(
                expectedSubstring = makePaddingString(start = horizontal, end = horizontal),
                modifierToString = getSinglePaddingModifierStringFromDoc(doc),
            )
        }

    @Test
    fun translateBox_verticalPadding() =
        fakeCoroutineScope.runTest {
            val vertical = 21

            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(modifier = GlanceModifier.padding(vertical = vertical.dp)) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            assertModifierStringContainsSubstring(
                expectedSubstring = makePaddingString(top = vertical, bottom = vertical),
                modifierToString = getSinglePaddingModifierStringFromDoc(doc),
            )
        }

    @Test
    fun translateBox_Padding_start() =
        fakeCoroutineScope.runTest {
            val start = 21

            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(modifier = GlanceModifier.padding(start = start.dp)) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            assertModifierStringContainsSubstring(
                expectedSubstring = makePaddingString(start = start),
                modifierToString = getSinglePaddingModifierStringFromDoc(doc),
            )
        }

    @Test
    fun translateBox_Padding_top() =
        fakeCoroutineScope.runTest {
            val top = 21

            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(modifier = GlanceModifier.padding(top = top.dp)) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            assertModifierStringContainsSubstring(
                expectedSubstring = makePaddingString(top = top),
                modifierToString = getSinglePaddingModifierStringFromDoc(doc),
            )
        }

    @Test
    fun translateBox_Padding_endBottom() =
        fakeCoroutineScope.runTest {
            val end = 33
            val bottom = 44

            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(modifier = GlanceModifier.padding(end = end.dp, bottom = bottom.dp)) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            assertModifierStringContainsSubstring(
                expectedSubstring = makePaddingString(end = end, bottom = bottom),
                modifierToString = getSinglePaddingModifierStringFromDoc(doc),
            )
        }

    @Test
    fun translateBox_Padding() =
        fakeCoroutineScope.runTest {
            val start = 11
            val top = 22
            val end = 33
            val bottom = 44

            val (_, wireBuffer: WireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(
                        modifier =
                            GlanceModifier.padding(
                                start = start.dp,
                                top = top.dp,
                                end = end.dp,
                                bottom = bottom.dp,
                            )
                    ) {
                        // no content
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            assertModifierStringContainsSubstring(
                expectedSubstring =
                    makePaddingString(start = start, top = top, end = end, bottom = bottom),
                modifierToString = getSinglePaddingModifierStringFromDoc(doc),
            )
        }
}

private fun makePaddingString(start: Int = 0, top: Int = 0, end: Int = 0, bottom: Int = 0): String {
    return "[${start.toFloat()}, ${top.toFloat()}, ${end.toFloat()}, ${bottom.toFloat()}]"
}

/** Assuming the document contains a single box with a single padding modifier. */
private fun getSinglePaddingModifierStringFromDoc(doc: CoreDocument): String {
    val box = getSimpleLeaf(doc) as BoxLayout
    debugPrintDoc(doc)

    val ops = box.componentModifiers.list.filterIsInstance<PaddingModifierOperation>()
    assertEquals(1, ops.size)
    val clipOp: PaddingModifierOperation = ops.first()
    val stringer = StringSerializer()
    clipOp.serializeToString(0, stringer)
    return stringer.toString()
}

private fun assertModifierStringContainsSubstring(
    expectedSubstring: String,
    modifierToString: String,
) {
    val failureMessage =
        "Result did not contain expected substring. Expected: '$expectedSubstring': Actual: $modifierToString"
    assertContains(modifierToString, other = expectedSubstring, message = failureMessage)
}
