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
import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.boundsInRoot
import androidx.ui.core.onChildPositioned
import androidx.ui.core.onPositioned
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.contentColor
import androidx.ui.foundation.currentTextStyle
import androidx.ui.foundation.shape.corner.CutCornerShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.compositeOver
import androidx.ui.layout.Column
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSize
import androidx.ui.semantics.Semantics
import androidx.ui.test.assertHasClickAction
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertIsNotEnabled
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import androidx.ui.test.runOnIdleCompose
import androidx.ui.unit.Dp
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.center
import androidx.ui.unit.dp
import androidx.ui.unit.height
import androidx.ui.unit.sp
import androidx.ui.unit.toPx
import androidx.ui.unit.width
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.abs

@MediumTest
@RunWith(JUnit4::class)
class ButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun defaultSemantics() {
        composeTestRule.setMaterialContent {
            Stack {
                Button(modifier = Modifier.testTag("myButton"), onClick = {}) {
                    Text("myButton")
                }
            }
        }

        findByTag("myButton")
            .assertIsEnabled()
    }

    @Test
    fun disabledSemantics() {
        composeTestRule.setMaterialContent {
            Stack {
                Button(modifier = Modifier.testTag("myButton"), onClick = {}, enabled = false) {
                    Text("myButton")
                }
            }
        }

        findByTag("myButton")
            .assertIsNotEnabled()
    }

    @Test
    fun findByTextAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "myButton"

        composeTestRule.setMaterialContent {
            Stack {
                Button(onClick = onClick) {
                    Text(text)
                }
            }
        }

        // TODO(b/129400818): this actually finds the text, not the button as
        // merge semantics aren't implemented yet
        findByText(text)
            .doClick()

        runOnIdleCompose {
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    fun canBeDisabled() {
        val tag = "myButton"

        composeTestRule.setMaterialContent {
            var enabled by state { true }
            val onClick = { enabled = false }
            Stack {
                Button(modifier = Modifier.testTag(tag), onClick = onClick, enabled = enabled) {
                    Text("Hello")
                }
            }
        }
        findByTag(tag)
            // Confirm the button starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .doClick()
            // Then confirm it's disabled with no click action after clicking it
            .assertHasNoClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun clickIsIndependentBetweenButtons() {
        var button1Counter = 0
        val button1OnClick: () -> Unit = { ++button1Counter }
        val button1Tag = "button1"

        var button2Counter = 0
        val button2OnClick: () -> Unit = { ++button2Counter }
        val button2Tag = "button2"

        val text = "myButton"

        composeTestRule.setMaterialContent {
            Column {
                Button(modifier = Modifier.testTag(button1Tag), onClick = button1OnClick) {
                    Text(text)
                }
                Button(modifier = Modifier.testTag(button2Tag), onClick = button2OnClick) {
                    Text(text)
                }
            }
        }

        findByTag(button1Tag)
            .doClick()

        runOnIdleCompose {
            assertThat(button1Counter).isEqualTo(1)
            assertThat(button2Counter).isEqualTo(0)
        }

        findByTag(button2Tag)
            .doClick()

        runOnIdleCompose {
            assertThat(button1Counter).isEqualTo(1)
            assertThat(button2Counter).isEqualTo(1)
        }
    }

    @Test
    fun buttonHeightIsFromSpec() {
        if (composeTestRule.density.fontScale > 1f) {
            // This test can be reasonable failing on the non default font scales
            // so lets skip it.
            return
        }
        composeTestRule
            .setMaterialContentAndCollectSizes {
                Button(onClick = {}) {
                    Text("Test button")
                }
            }
            .assertHeightEqualsTo(36.dp)
    }

    @Test
    fun ButtonWithLargeFontSizeIsLargerThenMinHeight() {
        val realSize: PxSize = composeTestRule.setMaterialContentAndGetPixelSize {
            Button(onClick = {}) {
                Text(
                    text = "Test button",
                    fontSize = 50.sp
                )
            }
        }

        with(composeTestRule.density) {
            assertThat(realSize.height)
                .isGreaterThan(36.dp.toIntPx().value.toFloat())
        }
    }

    @Test
    fun containedButtonPropagateDefaultTextStyle() {
        composeTestRule.setMaterialContent {
            Button(onClick = {}) {
                assertThat(currentTextStyle()).isEqualTo(MaterialTheme.typography.button)
            }
        }
    }

    @Test
    fun outlinedButtonPropagateDefaultTextStyle() {
        composeTestRule.setMaterialContent {
            OutlinedButton(onClick = {}) {
                assertThat(currentTextStyle()).isEqualTo(MaterialTheme.typography.button)
            }
        }
    }

    @Test
    fun textButtonPropagateDefaultTextStyle() {
        composeTestRule.setMaterialContent {
            TextButton(onClick = {}) {
                assertThat(currentTextStyle()).isEqualTo(MaterialTheme.typography.button)
            }
        }
    }

    @Test
    fun containedButtonHorPaddingIsFromSpec() {
        assertLeftPaddingIs(16.dp) { text ->
            Button(onClick = {}, text = text)
        }
    }

    @Test
    fun outlinedButtonHorPaddingIsFromSpec() {
        assertLeftPaddingIs(16.dp) { text ->
            OutlinedButton(onClick = {}, text = text)
        }
    }

    @Test
    fun textButtonHorPaddingIsFromSpec() {
        assertLeftPaddingIs(8.dp) { text ->
            TextButton(onClick = {}, text = text)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun shapeAndColorFromThemeIsUsed() {
        val shape = CutCornerShape(10.dp)
        var surface = Color.Transparent
        var primary = Color.Transparent
        composeTestRule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            primary = MaterialTheme.colors.primary
            Providers(ShapesAmbient provides Shapes(small = shape)) {
                Button(modifier = Modifier.testTag("myButton"), onClick = {}, elevation = 0.dp) {
                    Box(Modifier.preferredSize(10.dp, 10.dp))
                }
            }
        }

        findByTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = shape,
                shapeColor = primary,
                backgroundColor = surface,
                shapeOverlapPixelCount = with(composeTestRule.density) { 1.dp.toPx() }
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun containedButtonDisabledBackgroundIsCorrect() {
        var surface = Color.Transparent
        var onSurface = Color.Transparent
        val padding = 8.dp
        composeTestRule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            onSurface = MaterialTheme.colors.onSurface
            Box(Modifier.testTag("myButton")) {
                // stack allows to verify there is no shadow
                Stack(Modifier.padding(padding)) {
                    Button(
                        onClick = {},
                        enabled = false,
                        shape = RectangleShape) {}
                }
            }
        }

        findByTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                horizontalPadding = padding,
                verticalPadding = padding,
                backgroundColor = surface,
                shapeColor = onSurface.copy(alpha = 0.12f).compositeOver(surface)
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun containedButtonWithCustomColorDisabledBackgroundIsCorrect() {
        var surface = Color.Transparent
        var onSurface = Color.Transparent
        val padding = 8.dp
        composeTestRule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            onSurface = MaterialTheme.colors.onSurface
            Box(Modifier.testTag("myButton")) {
                // stack allows to verify there is no shadow
                Stack(Modifier.padding(padding)) {
                    Button(
                        onClick = {},
                        enabled = false,
                        backgroundColor = Color.Red,
                        shape = RectangleShape
                    ) {}
                }
            }
        }

        findByTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                horizontalPadding = padding,
                verticalPadding = padding,
                backgroundColor = surface,
                shapeColor = onSurface.copy(alpha = 0.12f).compositeOver(surface)
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun outlinedButtonDisabledBackgroundIsCorrect() {
        var surface = Color.Transparent
        val padding = 8.dp
        composeTestRule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            // stack allows to verify there is no shadow
            Stack(Modifier.padding(padding)) {
                OutlinedButton(
                    modifier = Modifier.testTag("myButton"),
                    onClick = {},
                    enabled = false,
                    shape = RectangleShape,
                    border = null
                ) {}
            }
        }

        findByTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = RectangleShape,
                shapeColor = surface,
                backgroundColor = surface
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun textButtonDisabledBackgroundIsCorrect() {
        var surface = Color.Transparent
        composeTestRule.setMaterialContent {
            surface = MaterialTheme.colors.surface
            // stack allows to verify there is no shadow
            Stack(Modifier.padding(8.dp)) {
                TextButton(
                    modifier = Modifier.testTag("myButton"),
                    onClick = {},
                    enabled = false,
                    shape = RectangleShape
                ) {}
            }
        }

        findByTag("myButton")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = RectangleShape,
                shapeColor = surface,
                backgroundColor = surface
            )
    }

    @Test
    fun containedButtonDisabledContentColorIsCorrect() {
        var onSurface = Color.Transparent
        var content = Color.Transparent
        var emphasis: Emphasis? = null
        composeTestRule.setMaterialContent {
            onSurface = MaterialTheme.colors.onSurface
            emphasis = EmphasisAmbient.current.disabled
            Button(onClick = {}, enabled = false) {
                content = contentColor()
            }
        }

        assertThat(content).isEqualTo(emphasis!!.applyEmphasis(onSurface))
    }

    @Test
    fun outlinedButtonDisabledContentColorIsCorrect() {
        var onSurface = Color.Transparent
        var content = Color.Transparent
        var emphasis: Emphasis? = null
        composeTestRule.setMaterialContent {
            onSurface = MaterialTheme.colors.onSurface
            emphasis = EmphasisAmbient.current.disabled
            OutlinedButton(onClick = {}, enabled = false) {
                content = contentColor()
            }
        }

        assertThat(content).isEqualTo(emphasis!!.applyEmphasis(onSurface))
    }

    @Test
    fun textButtonDisabledContentColorIsCorrect() {
        var onSurface = Color.Transparent
        var content = Color.Transparent
        var emphasis: Emphasis? = null
        composeTestRule.setMaterialContent {
            onSurface = MaterialTheme.colors.onSurface
            emphasis = EmphasisAmbient.current.disabled
            TextButton(onClick = {}, enabled = false) {
                content = contentColor()
            }
        }

        assertThat(content).isEqualTo(emphasis!!.applyEmphasis(onSurface))
    }

    @Test
    fun contentIsWrappedAndCentered() {
        var buttonCoordinates: LayoutCoordinates? = null
        var contentCoordinates: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Stack {
                Button({}, Modifier.onPositioned { buttonCoordinates = it }) {
                    Box(
                        Modifier.preferredSize(2.dp)
                            .onPositioned { contentCoordinates = it }
                    )
                }
            }
        }

        runOnIdleCompose {
            val buttonBounds = buttonCoordinates!!.boundsInRoot
            val contentBounds = contentCoordinates!!.boundsInRoot
            assertThat(contentBounds.width).isLessThan(buttonBounds.width)
            assertThat(contentBounds.height).isLessThan(buttonBounds.height)
            with(composeTestRule.density) {
                assertThat(contentBounds.width).isEqualTo(2.dp.toIntPx().value.toFloat())
                assertThat(contentBounds.height).isEqualTo(2.dp.toIntPx().value.toFloat())
            }
            assertWithinOnePixel(buttonBounds.center(), contentBounds.center())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun zOrderingBasedOnElevationIsApplied() {
        composeTestRule.setMaterialContent {
            Semantics(container = true, mergeAllDescendants = true) {
                Stack(Modifier.testTag("stack").preferredSize(10.dp, 10.dp)) {
                    Button(
                        backgroundColor = Color.Yellow,
                        elevation = 2.dp,
                        onClick = {},
                        shape = RectangleShape
                    ) {
                        Box(Modifier.fillMaxSize())
                    }
                    Button(
                        backgroundColor = Color.Green,
                        elevation = 0.dp,
                        onClick = {},
                        shape = RectangleShape
                    ) {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }

        findByTag("stack")
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = RectangleShape,
                shapeColor = Color.Yellow,
                backgroundColor = Color.White
            )
    }

    private fun assertLeftPaddingIs(
        padding: Dp,
        button: @Composable (@Composable () -> Unit) -> Unit
    ) {
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Stack(Modifier.onChildPositioned { parentCoordinates = it }) {
                button {
                    Text("Test button",
                        Modifier.onPositioned { childCoordinates = it }
                    )
                }
            }
        }

        runOnIdleCompose {
            val topLeft = childCoordinates!!.localToGlobal(PxPosition.Origin).x -
                    parentCoordinates!!.localToGlobal(PxPosition.Origin).x
            val currentPadding = with(composeTestRule.density) {
                padding.toIntPx().value.toFloat()
            }
            assertThat(currentPadding).isEqualTo(topLeft)
        }
    }
}

fun assertWithinOnePixel(expected: PxPosition, actual: PxPosition) {
    assertWithinOnePixel(expected.x, actual.x)
    assertWithinOnePixel(expected.y, actual.y)
}

fun assertWithinOnePixel(expected: Float, actual: Float) {
    val diff = abs(expected - actual)
    assertThat(diff).isLessThan(1.1f)
}
