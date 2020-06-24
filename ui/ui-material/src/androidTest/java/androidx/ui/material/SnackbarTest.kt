/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material

import android.os.Build
import androidx.compose.Providers
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.Modifier
import androidx.ui.core.semantics.semantics
import androidx.ui.core.testTag
import androidx.ui.foundation.Text
import androidx.ui.foundation.shape.corner.CutCornerShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.compositeOver
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Stack
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertIsEqualTo
import androidx.ui.test.assertIsNotEqualTo
import androidx.ui.test.assertShape
import androidx.ui.test.assertTopPositionInRootIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.test.getAlignmentLinePosition
import androidx.ui.test.getBoundsInRoot
import androidx.ui.text.FirstBaseline
import androidx.ui.text.LastBaseline
import androidx.ui.unit.dp
import androidx.ui.unit.height
import androidx.ui.unit.width
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SnackbarTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val longText = "Message is very long and long and long and long and long " +
            "and long and long and long and long and long and long"

    @Test
    fun defaultSnackbar_semantics() {
        var clicked = false
        composeTestRule.setMaterialContent {
            Stack {
                Snackbar(text = { Text("Message") }, action = {
                    TextButton(onClick = { clicked = true }) {
                        Text("UNDO")
                    }
                })
            }
        }

        findByText("Message")
            .assertExists()

        assertThat(clicked).isFalse()

        findByText("UNDO")
            .doClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun snackbar_shortTextOnly_sizes() {
        composeTestRule.setMaterialContentWithConstraints(DpConstraints(maxWidth = 300.dp)) {
            Snackbar(
                modifier = Modifier.testTag("snackbar"),
                text = {
                    Text("Message")
                }
            )
        }

        findByTag("snackbar")
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(48.dp)

        val fistBaseLine = findByText("Message").getAlignmentLinePosition(FirstBaseline)
        val lastBaseLine = findByText("Message").getAlignmentLinePosition(LastBaseline)
        fistBaseLine.assertIsNotEqualTo(0.dp, "first baseline")
        fistBaseLine.assertIsEqualTo(lastBaseLine, "first baseline")

        findByText("Message")
            .assertTopPositionInRootIsEqualTo(30.dp - fistBaseLine)
    }

    @Test
    fun snackbar_shortTextAndButton_alignment() {
        composeTestRule.setMaterialContentWithConstraints(DpConstraints(maxWidth = 300.dp)) {
            Snackbar(
                modifier = Modifier.testTag("snackbar"),
                text = {
                    Text("Message")
                },
                action = {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.testTag("button")
                    ) {
                        Text("Undo")
                    }
                }
            )
        }

        findByTag("snackbar")
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(48.dp)

        val textBaseLine = findByText("Message").getAlignmentLinePosition(FirstBaseline)
        val buttonBaseLine = findByTag("button").getAlignmentLinePosition(FirstBaseline)
        textBaseLine.assertIsNotEqualTo(0.dp, "text baseline")
        buttonBaseLine.assertIsNotEqualTo(0.dp, "button baseline")

        findByText("Message")
            .assertTopPositionInRootIsEqualTo(30.dp - textBaseLine)

        findByTag("button")
            .assertTopPositionInRootIsEqualTo(30.dp - buttonBaseLine)
    }

    @Test
    fun snackbar_longText_sizes() {
        composeTestRule.setMaterialContentWithConstraints(DpConstraints(maxWidth = 300.dp)) {
            Snackbar(
                modifier = Modifier.testTag("snackbar"),
                text = {
                    Text(longText, Modifier.testTag("text"), maxLines = 2)
                }
            )
        }

        findByTag("snackbar")
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(68.dp)

        val firstBaseline = findByTag("text").getFirstBaselinePosition()
        val lastBaseline = findByTag("text").getLastBaselinePosition()

        firstBaseline.assertIsNotEqualTo(0.dp, "first baseline")
        lastBaseline.assertIsNotEqualTo(0.dp, "last baseline")
        firstBaseline.assertIsNotEqualTo(lastBaseline, "first baseline")

        findByTag("text")
            .assertTopPositionInRootIsEqualTo(30.dp - firstBaseline)
    }

    @Test
    fun snackbar_longTextAndButton_alignment() {
        composeTestRule.setMaterialContentWithConstraints(DpConstraints(maxWidth = 300.dp)) {
            Snackbar(
                modifier = Modifier.testTag("snackbar"),
                text = {
                    Text(longText, Modifier.testTag("text"), maxLines = 2)
                },
                action = {
                    TextButton(
                        modifier = Modifier.testTag("button"),
                        onClick = {}
                    ) {
                        Text("Undo")
                    }
                }
            )
        }

        findByTag("snackbar")
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(68.dp)

        val textFirstBaseLine = findByTag("text").getFirstBaselinePosition()
        val textLastBaseLine = findByTag("text").getLastBaselinePosition()

        textFirstBaseLine.assertIsNotEqualTo(0.dp, "first baseline")
        textLastBaseLine.assertIsNotEqualTo(0.dp, "last baseline")
        textFirstBaseLine.assertIsNotEqualTo(textLastBaseLine, "first baseline")

        findByTag("text")
            .assertTopPositionInRootIsEqualTo(30.dp - textFirstBaseLine)

        val buttonBounds = findByTag("button").getBoundsInRoot()
        val snackBounds = findByTag("snackbar").getBoundsInRoot()

        val buttonCenter = buttonBounds.top + (buttonBounds.height / 2)
        buttonCenter.assertIsEqualTo(snackBounds.height / 2, "button center")
    }

    @Test
    fun snackbar_textAndButtonOnSeparateLine_alignment() {
        composeTestRule.setMaterialContentWithConstraints(DpConstraints(maxWidth = 300.dp)) {
            Snackbar(
                modifier = Modifier.testTag("snackbar"),
                text = {
                    Text("Message")
                },
                action = {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.testTag("button")
                    ) {
                        Text("Undo")
                    }
                },
                actionOnNewLine = true
            )
        }

        val textFirstBaseLine = findByText("Message").getFirstBaselinePosition()
        val textLastBaseLine = findByText("Message").getLastBaselinePosition()
        val textBounds = findByText("Message").getBoundsInRoot()
        val buttonBounds = findByTag("button").getBoundsInRoot()

        findByText("Message")
            .assertTopPositionInRootIsEqualTo(30.dp - textFirstBaseLine)

        findByTag("button")
            .assertTopPositionInRootIsEqualTo(18.dp + textBounds.top + textLastBaseLine)

        findByTag("snackbar")
            .assertHeightIsEqualTo(8.dp + buttonBounds.top + buttonBounds.height)
            .assertWidthIsEqualTo(8.dp + buttonBounds.left + buttonBounds.width)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shapeAndColorFromThemeIsUsed() {
        val shape = CutCornerShape(8.dp)
        var background = Color.Yellow
        var snackBarColor = Color.Transparent
        composeTestRule.setMaterialContent {
            Stack {
                background = MaterialTheme.colors.surface
                // Snackbar has a background color of onSurface with an alpha applied blended
                // on top of surface
                snackBarColor = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    .compositeOver(background)
                Providers(ShapesAmbient provides Shapes(medium = shape)) {
                    Snackbar(modifier = Modifier
                        .semantics(mergeAllDescendants = true)
                        .testTag("snackbar"),
                        text = { Text("") }
                    )
                }
            }
        }

        findByTag("snackbar")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = shape,
                shapeColor = snackBarColor,
                backgroundColor = background,
                shapeOverlapPixelCount = with(composeTestRule.density) { 2.dp.toPx() }
            )
    }
}