/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.test

import android.graphics.Rect
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Popup
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class BitmapCapturingTest(val config: TestConfig) {
    data class TestConfig(val activityClass: Class<out ComponentActivity>)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> =
            listOf(
                TestConfig(ComponentActivity::class.java),
                TestConfig(CustomComposeHostActivity::class.java),
            )
    }

    @get:Rule val rule = createAndroidComposeRule(config.activityClass)

    private val rootTag = "Root"
    private val tagTopLeft = "TopLeft"
    private val tagTopRight = "TopRight"
    private val tagBottomLeft = "BottomLeft"
    private val tagBottomRight = "BottomRight"

    private val colorTopLeft = Color.Red
    private val colorTopRight = Color.Blue
    private val colorBottomLeft = Color.Green
    private val colorBottomRight = Color.Yellow
    private val colorBg = Color.Black

    @Test
    fun captureIndividualRects_checkSizeAndColors() {
        composeCheckerboard()

        var calledCount = 0
        rule.onNodeWithTag(tagTopLeft).captureToImage().assertPixels(
            expectedSize = IntSize(100, 50)
        ) {
            calledCount++
            colorTopLeft
        }
        assertThat(calledCount).isEqualTo((100 * 50))

        rule.onNodeWithTag(tagTopRight).captureToImage().assertPixels(
            expectedSize = IntSize(100, 50)
        ) {
            colorTopRight
        }
        rule.onNodeWithTag(tagBottomLeft).captureToImage().assertPixels(
            expectedSize = IntSize(100, 50)
        ) {
            colorBottomLeft
        }
        rule.onNodeWithTag(tagBottomRight).captureToImage().assertPixels(
            expectedSize = IntSize(100, 50)
        ) {
            colorBottomRight
        }
    }

    @Test
    fun captureRootContainer_checkSizeAndColors() {
        composeCheckerboard()

        rule.onNodeWithTag(rootTag).captureToImage().assertPixels(
            expectedSize = IntSize(200, 100)
        ) {
            expectedColorProvider(it)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P) // b/163023027
    fun captureDialog_verifyBackground() {
        // Test that we are really able to capture dialogs to bitmap.
        setContent {
            AlertDialog(onDismissRequest = {}, confirmButton = {}, backgroundColor = Color.Red)
        }

        rule.onNode(isDialog()).captureToImage().assertContainsColor(Color.Red)
    }

    @Test
    fun capturePopup_verifyBackground() {
        setContent { Box { Popup { Box(Modifier.background(Color.Red)) { Text("Hello") } } } }

        rule.onNode(isPopup()).captureToImage().assertContainsColor(Color.Red)
    }

    @Test
    fun captureComposable_withPopUp_verifyBackground() {
        setContent {
            Box(Modifier.testTag(rootTag).size(300.dp).background(Color.Yellow)) {
                Popup { Box(Modifier.background(Color.Red)) { Text("Hello") } }
            }
        }

        rule
            .onNodeWithTag(rootTag)
            .captureToImage()
            .assertContainsColor(Color.Yellow)
            .assertDoesNotContainColor(Color.Red)
    }

    @Test
    fun captureComposable_withDialog_verifyBackground() {
        setContent {
            Box(Modifier.testTag(rootTag).size(300.dp).background(Color.Yellow)) {
                Dialog({}) { Box(Modifier.size(300.dp).background(Color.Red)) { Text("Hello") } }
            }
        }
        rule
            .onNodeWithTag(rootTag)
            .captureToImage()
            .assertContainsColor(Color.Yellow)
            .assertDoesNotContainColor(Color.Red)
    }

    @Test
    fun capturePopup_verifySize() {
        val boxSize = 200.dp
        val boxSizePx = boxSize.toPixel(rule.density).roundToInt()
        setContent { Box { Popup { Box(Modifier.size(boxSize)) { Text("Hello") } } } }

        rule.onNode(isPopup()).captureToImage().let {
            assertThat(IntSize(it.width, it.height)).isEqualTo(IntSize(boxSizePx, boxSizePx))
        }
    }

    @Test
    fun capturePopupWithAnchor_verifySize() {
        val boxSize = 200.dp
        val popUpSizePx = boxSize.toPixel(rule.density).roundToInt()
        setContent {
            Box(Modifier.size(boxSize).background(Color.Yellow)) { Popup { Box { Text("Hello") } } }
        }
        rule.onRoot().captureToImage().let {
            assertThat(IntSize(it.width, it.height)).isEqualTo(IntSize(popUpSizePx, popUpSizePx))
        }
    }

    @Test
    fun captureDialogWithAnchor_verifySize() {
        val boxSize = 200.dp

        setContent {
            Box(Modifier.size(boxSize).background(Color.Red)) {
                Dialog(onDismissRequest = {}) {
                    Box(Modifier.background(Color.Yellow)) { Text("Hello") }
                }
            }
        }

        val visibleFrame = Rect()
        rule.activity.window.decorView.getWindowVisibleDisplayFrame(visibleFrame)
        val expectedWidthPx = visibleFrame.width()
        val expectedHeightPx = visibleFrame.height()

        rule.onRoot().captureToImage().let {
            assertThat(IntSize(it.width, it.height))
                .isEqualTo(IntSize(expectedWidthPx, expectedHeightPx))
        }
    }

    @Test
    fun capturePopupWithAnchor_verifyColors() {
        setContent {
            Box(Modifier.size(200.dp).background(Color.Yellow)) {
                Popup(alignment = Alignment.Center) {
                    Box(Modifier.size(50.dp).background(Color.Red))
                }
            }
        }

        rule.onRoot().captureToImage().let { bitmap ->
            bitmap.assertContainsColor(Color.Yellow)
            bitmap.assertContainsColor(Color.Red)
        }
    }

    @Test
    fun captureDialogWithAnchor_verifyColors() {
        setContent {
            Box(Modifier.size(200.dp).background(Color.Red)) {
                Dialog(onDismissRequest = {}) {
                    val view = LocalView.current
                    val window = (view.parent as? DialogWindowProvider)?.window
                    window?.setDimAmount(0f)
                    Box(Modifier.size(50.dp).background(Color.Yellow))
                }
            }
        }

        rule.onRoot().captureToImage().let { bitmap ->
            bitmap.assertContainsColor(Color.Red)
            bitmap.assertContainsColor(Color.Yellow)
        }
    }

    @Test
    fun capturePopup_partiallyOffScreen_doesNotCrash() {
        setContent {
            Box(Modifier.size(100.dp)) {
                // Offset aggressively so the popup attempts to draw outside the top-left of the
                // screen
                Popup(alignment = Alignment.TopStart, offset = IntOffset(-1000, -1000)) {
                    Box(Modifier.size(2000.dp).background(Color.Red))
                }
            }
        }

        val bitmap = rule.onRoot().captureToImage()

        assertThat(bitmap.width).isGreaterThan(0)
        assertThat(bitmap.height).isGreaterThan(0)
    }

    @Test
    fun captureMultiplePopups_verifyColors() {
        setContent {
            Box(Modifier.size(200.dp).background(Color.White)) {
                Popup(alignment = Alignment.TopStart) {
                    Box(Modifier.size(50.dp).background(Color.Red))
                }
                Popup(alignment = Alignment.BottomEnd) {
                    Box(Modifier.size(50.dp).background(Color.Blue))
                }
            }
        }

        rule.onRoot().captureToImage().let { bitmap ->
            bitmap.assertContainsColor(Color.White)
            bitmap.assertContainsColor(Color.Red)
            bitmap.assertContainsColor(Color.Blue)
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.O_MR1)
    fun captureDialog_apiBelow28_throwsException() {
        setContent {
            Dialog(onDismissRequest = {}) { Box(Modifier.size(100.dp).background(Color.Red)) }
        }

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                rule.onNode(isDialog()).captureToImage()
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("Cannot currently capture dialogs on API lower than 28")
    }

    private fun Dp.toPixel(density: Density) = this.value * density.density

    private fun expectedColorProvider(pos: IntOffset): Color {
        if (pos.y < 50) {
            if (pos.x < 100) {
                return colorTopLeft
            } else if (pos.x < 200) {
                return colorTopRight
            }
        } else if (pos.y < 100) {
            if (pos.x < 100) {
                return colorBottomLeft
            } else if (pos.x < 200) {
                return colorBottomRight
            }
        }
        throw IllegalArgumentException("Expected color undefined for position $pos")
    }

    private fun composeCheckerboard() {
        with(rule.density) {
            setContent {
                Box(Modifier.background(colorBg).windowInsetsPadding(WindowInsets.navigationBars)) {
                    Box(Modifier.padding(top = 20.toDp()).background(colorBg)) {
                        Column(Modifier.testTag(rootTag)) {
                            Row {
                                Box(
                                    Modifier.testTag(tagTopLeft)
                                        .size(100.toDp(), 50.toDp())
                                        .background(color = colorTopLeft)
                                )
                                Box(
                                    Modifier.testTag(tagTopRight)
                                        .size(100.toDp(), 50.toDp())
                                        .background(colorTopRight)
                                )
                            }
                            Row {
                                Box(
                                    Modifier.testTag(tagBottomLeft)
                                        .size(100.toDp(), 50.toDp())
                                        .background(colorBottomLeft)
                                )
                                Box(
                                    Modifier.testTag(tagBottomRight)
                                        .size(100.toDp(), 50.toDp())
                                        .background(colorBottomRight)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setContent(content: @Composable () -> Unit) {
        when (val activity = rule.activity) {
            is CustomComposeHostActivity -> activity.setContent(content)
            else -> rule.setContent(content)
        }
    }
}
