/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.modifiers

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SelectionControllerTest {

    @get:Rule
    val rule = createComposeRule()

    private val boxTag = "boxTag"
    private val tag = "tag"

    private val highlightColor = Color.Blue
    private val foregroundColor = Color.Black
    private val backgroundColor = Color.White

    private val highlightArgb get() = highlightColor.toArgb()
    private val foregroundArgb get() = foregroundColor.toArgb()
    private val backgroundArgb get() = backgroundColor.toArgb()

    private val density = Density(1f)
    private val textSelectionColors = TextSelectionColors(highlightColor, highlightColor)

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun drawWithClip_doesClip() = runDrawWithClipTest(TextOverflow.Clip) { argbSet ->
        assertThat(argbSet).containsExactly(backgroundArgb)
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun drawWithVisible_doesNotClip() = runDrawWithClipTest(TextOverflow.Visible) { argbSet ->
        // there could be more colors due to anti-aliasing, so check that we have at least the
        // expected colors, and none of the unexpected colors.
        assertThat(argbSet).containsAtLeast(highlightArgb, foregroundArgb)
        assertThat(argbSet).doesNotContain(backgroundArgb)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun runDrawWithClipTest(overflow: TextOverflow, assertBlock: (Set<Int>) -> Unit) {
        rule.setContent {
            CompositionLocalProvider(
                LocalTextSelectionColors provides textSelectionColors,
                LocalDensity provides density,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind { drawRect(backgroundColor) }
                        .testTag(boxTag),
                    contentAlignment = Alignment.Center,
                ) {
                    SelectionContainer {
                        BasicText(
                            modifier = Modifier
                                .width(10.dp)
                                .testTag(tag),
                            text = "OOOOOOO",
                            overflow = overflow,
                            softWrap = false,
                            style = TextStyle(color = foregroundColor, fontSize = 48.sp),
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag(tag).performTouchInput { longClick() }
        rule.waitForIdle()

        with(density) {
            val bitmap = rule.onNodeWithTag(boxTag).captureToImage().asAndroidBitmap()
            val bitmapPositionInRoot =
                rule.onNodeWithTag(boxTag).getBoundsInRoot().toRect().roundToIntRect().topLeft
            val centerRightOffset =
                rule.onNodeWithTag(tag).getBoundsInRoot().toRect().roundToIntRect().centerRight

            val centerRightOffsetInRoot = centerRightOffset - bitmapPositionInRoot
            val (x, y) = centerRightOffsetInRoot

            // grab a row of pixels in the area that may be clipped.
            val seenColors = (1..50).map { bitmap.getPixel(x + it, y) }.toSet()
            assertBlock(seenColors)
        }
    }
}
