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

import androidx.compose.remote.core.operations.Utils.colorInt
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.remotecompose.RemoteComposeLayoutInspection.getProperty
import androidx.glance.appwidget.remotecompose.RemoteComposeLayoutInspection.getSubProperty
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.debugPrintDoc
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.getSimpleLeaf
import androidx.glance.appwidget.remotecompose.RemoteComposeTestUtils.runAndTranslateSingleRoot
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class TranslateTextTest : BaseRemoteComposeTest() {

    @Test
    fun translateText_basic() =
        fakeCoroutineScope.runTest {
            val string = "hello world"

            val (_, wireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(modifier = GlanceModifier.size(100.dp, 100.dp)) { Text(string) }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val debugContext = GlanceDebugCreationContext()
            doc.initializeContext(debugContext)

            debugPrintDoc(doc)
            val textNode = getSimpleLeaf(doc) as TextLayout

            val textId: Int = textNode.textId!!
            val loadedStr: String = debugContext.getText(textId)
            assertEquals(string, loadedStr)
        }

    @Test
    fun translateTextStyle_fontSize() =
        fakeCoroutineScope.runTest {
            val string = "24sp"
            val size = 24

            val textNode = testTextWithStyle(string, TextStyle(fontSize = size.sp))
            val fontSizeValue = getSubProperty<Float>(textNode, "fontSize", "value")

            assertEquals(size.toFloat(), fontSizeValue)
        }

    @Test
    fun translateTextStyle_color_fixedColorProvider() =
        fakeCoroutineScope.runTest {
            val string = "hardcoded color: Blue"
            val blue: Color = Color.Blue
            val blueArgb: Int = blue.toArgb()

            val rcTextLayout = testTextWithStyle(string, TextStyle(color = ColorProvider(blue)))
            val colorArgbStr = getProperty<String>(rcTextLayout, "color")

            assertEquals(colorInt(blueArgb), colorArgbStr)
        }

    @Test
    fun translateTextStyle_fontWeight_bold() =
        fakeCoroutineScope.runTest {
            // test 1: bold
            val boldStr = "text with bold font weight"
            val bold = FontWeight.Bold

            val rcTextLayoutBold = testTextWithStyle(boldStr, TextStyle(fontWeight = bold))
            val actualWeight = getProperty<Float>(rcTextLayoutBold, "fontWeight")

            assertEquals(bold.value.toFloat(), actualWeight)
        }

    @Test
    fun translateTextStyle_fontWeight_Medium() =
        fakeCoroutineScope.runTest {
            val mediumStr = "text with medium font weight"
            val medium = FontWeight.Medium

            val rcTextLayoutMedium = testTextWithStyle(mediumStr, TextStyle(fontWeight = medium))
            val actualWeight_ExpectedMedium = getProperty<Float>(rcTextLayoutMedium, "fontWeight")

            assertEquals(medium.value.toFloat(), actualWeight_ExpectedMedium)
        }

    @Test
    fun translateTextStyle_fontWeight_Normal() =
        fakeCoroutineScope.runTest {
            val normalStr = "text with medium font weight"
            val normal = FontWeight.Normal

            val rcTextLayoutNormal = testTextWithStyle(normalStr, TextStyle(fontWeight = normal))
            val actualWeight_ExpectedMedium = getProperty<Float>(rcTextLayoutNormal, "fontWeight")

            assertEquals(normal.value.toFloat(), actualWeight_ExpectedMedium)
        }

    @Test
    fun translateTextStyle_fontStyle_normal() =
        fakeCoroutineScope.runTest {
            val fontStyle = FontStyle.Normal

            val rcTextLayout = testTextWithStyle(style = TextStyle(fontStyle = fontStyle))
            val actualStyle = getProperty<Int>(rcTextLayout, "fontStyle")

            assertEquals(0, actualStyle)
        }

    @Test
    fun translateTextStyle_fontStyle_italic() =
        fakeCoroutineScope.runTest {
            val fontStyle = FontStyle.Italic

            val rcTextLayout: TextLayout =
                testTextWithStyle(style = TextStyle(fontStyle = fontStyle))
            val actualStyle = getProperty<Int>(rcTextLayout, "fontStyle")

            assertEquals(1, actualStyle)
        }

    @Test
    fun translateText_maxLines_3() =
        fakeCoroutineScope.runTest {
            val maxLines = 3
            val string = "hello world"

            val (_, wireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(modifier = GlanceModifier.size(100.dp, 100.dp)) {
                        Text(string, maxLines = maxLines)
                    }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val rcTextLayout = getSimpleLeaf(doc) as TextLayout
            val actualMaxLines = getProperty<Int>(rcTextLayout, "maxLines")

            assertEquals(maxLines, actualMaxLines)
        }

    @Test
    fun translateText_maxLines_default() =
        fakeCoroutineScope.runTest {
            val string = "hello world"

            val (_, wireBuffer) =
                context.runAndTranslateSingleRoot {
                    Box(modifier = GlanceModifier.size(100.dp, 100.dp)) { Text(string) }
                }

            val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

            val rcTextLayout = getSimpleLeaf(doc) as TextLayout
            val actualMaxLines = getProperty<Int>(rcTextLayout, "maxLines")

            assertEquals(Integer.MAX_VALUE, actualMaxLines)
        }

    /** Helper function for functions that just test styles */
    private suspend fun testTextWithStyle(string: String = "test", style: TextStyle): TextLayout {

        val (_, wireBuffer) =
            context.runAndTranslateSingleRoot {
                Box(modifier = GlanceModifier.size(100.dp, 100.dp)) { Text(string, style = style) }
            }

        val doc = makeCoreDocumentForDebug(wireBuffer = wireBuffer)

        //        val debugContext = GlanceDebugCreationContext()
        //        doc.initializeContext(debugContext)
        //        doc.paint()
        return getSimpleLeaf(doc) as TextLayout
    }
}
