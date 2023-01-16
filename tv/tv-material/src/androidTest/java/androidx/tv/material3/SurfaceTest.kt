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

@file:OptIn(ExperimentalTvMaterial3Api::class)

package androidx.tv.material3

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SurfaceTest {

    @get:Rule
    val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun noTonalElevationColorIsSetOnNonElevatedSurfaceColor() {
        var absoluteTonalElevation: Dp = 0.dp
        var surfaceColor: Color = Color.Unspecified
        rule.setMaterialContent(lightColorScheme()) {
            surfaceColor = MaterialTheme.colorScheme.surface
            Box(
                Modifier
                    .size(10.dp, 10.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("box")
            ) {
                Surface(
                    color = surfaceColor,
                    tonalElevation = 0.dp,
                    selected = false,
                    onClick = {}
                ) {
                    absoluteTonalElevation = LocalAbsoluteTonalElevation.current
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(absoluteTonalElevation).isEqualTo(0.dp)
        }

        rule.onNodeWithTag("box")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = surfaceColor,
                backgroundColor = Color.White
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun tonalElevationColorIsSetOnElevatedSurfaceColor() {
        var absoluteTonalElevation: Dp = 0.dp
        var surfaceTonalColor: Color = Color.Unspecified
        var surfaceColor: Color
        rule.setMaterialContent(lightColorScheme()) {
            surfaceColor = MaterialTheme.colorScheme.surface
            Box(
                Modifier
                    .size(10.dp, 10.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("box")
            ) {
                Surface(
                    color = surfaceColor,
                    tonalElevation = 2.dp,
                    selected = false,
                    onClick = {}
                ) {
                    absoluteTonalElevation = LocalAbsoluteTonalElevation.current
                    Box(Modifier.fillMaxSize())
                }
                surfaceTonalColor =
                    MaterialTheme.colorScheme.surfaceColorAtElevation(absoluteTonalElevation)
            }
        }

        rule.runOnIdle {
            Truth.assertThat(absoluteTonalElevation).isEqualTo(2.dp)
        }

        rule.onNodeWithTag("box")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = surfaceTonalColor,
                backgroundColor = Color.White
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun tonalElevationColorIsNotSetOnNonSurfaceColor() {
        var absoluteTonalElevation: Dp = 0.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box(
                Modifier
                    .size(10.dp, 10.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("box")
            ) {
                Surface(
                    color = Color.Green,
                    tonalElevation = 2.dp,
                    selected = false,
                    onClick = {}
                ) {
                    Box(Modifier.fillMaxSize())
                    absoluteTonalElevation = LocalAbsoluteTonalElevation.current
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(absoluteTonalElevation).isEqualTo(2.dp)
        }

        rule.onNodeWithTag("box")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Green,
                backgroundColor = Color.White
            )
    }

    @Test
    fun absoluteElevationCompositionLocalIsSet() {
        var outerElevation: Dp? = null
        var innerElevation: Dp? = null
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                tonalElevation = 2.dp,
                selected = false,
                onClick = {}
            ) {
                outerElevation = LocalAbsoluteTonalElevation.current
                Surface(
                    tonalElevation = 4.dp,
                    selected = false,
                    onClick = {}
                ) {
                    innerElevation = LocalAbsoluteTonalElevation.current
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(outerElevation).isEqualTo(2.dp)
            Truth.assertThat(innerElevation).isEqualTo(6.dp)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun absoluteElevationIsNotUsedForShadows() {
        rule.setMaterialContent(lightColorScheme()) {
            Column {
                Box(
                    Modifier
                        .padding(10.dp)
                        .size(10.dp, 10.dp)
                        .semantics(mergeDescendants = true) {}
                        .testTag("top level")
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(0.dp),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        color = Color.Blue,
                        content = {},
                        selected = false,
                        onClick = {}
                    )
                }

                // Set LocalAbsoluteTonalElevation to increase the absolute elevation
                CompositionLocalProvider(
                    LocalAbsoluteTonalElevation provides 2.dp
                ) {
                    Box(
                        Modifier
                            .padding(10.dp)
                            .size(10.dp, 10.dp)
                            .semantics(mergeDescendants = true) {}
                            .testTag("nested")
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(0.dp),
                            tonalElevation = 0.dp,
                            shadowElevation = 2.dp,
                            color = Color.Blue,
                            content = {},
                            selected = false,
                            onClick = {}
                        )
                    }
                }
            }
        }

        val topLevelSurfaceBitmap = rule.onNodeWithTag("top level").captureToImage()
        val nestedSurfaceBitmap = rule.onNodeWithTag("nested").captureToImage()
            .asAndroidBitmap()

        topLevelSurfaceBitmap.assertPixels {
            Color(nestedSurfaceBitmap.getPixel(it.x, it.y))
        }
    }

    /**
     * Tests that composed modifiers applied to Surface are applied within the changes to
     * [LocalContentColor], so they can consume the updated values.
     */
    @Test
    fun contentColorSetBeforeModifier() {
        var contentColor: Color = Color.Unspecified
        val expectedColor = Color.Blue
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalContentColor provides Color.Red) {
                Surface(
                    modifier = Modifier.composed {
                        contentColor = LocalContentColor.current
                        Modifier
                    },
                    tonalElevation = 2.dp,
                    contentColor = expectedColor,
                    content = {},
                    selected = false,
                    onClick = {}
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(contentColor).isEqualTo(expectedColor)
        }
    }

    @Test
    fun surface_blockClicksBehind() {
        val state = mutableStateOf(0)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("clickable")
                        .clickable { state.value += 1 },
                ) { Text("button fullscreen") }
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("surface"),
                    onClick = {},
                    selected = false
                ) {}
            }
        }
        rule.onNodeWithTag("clickable").assertHasClickAction().performClick()
        // still 0
        Truth.assertThat(state.value).isEqualTo(0)
    }

    @Test
    fun selectable_semantics() {
        val selected = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                selected = selected.value,
                onClick = { selected.value = !selected.value },
                modifier = Modifier.testTag("surface"),
            ) {
                Text("${selected.value}")
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("surface")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("false")
            .performClick()
            .assertTextEquals("true")
    }

    @Test
    fun selectable_customSemantics() {
        val selected = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                selected = selected.value,
                onClick = { selected.value = !selected.value },
                modifier = Modifier
                    .semantics { role = Role.Switch }
                    .testTag("surface"),
            ) {
                Text("${selected.value}")
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("surface")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("false")
            .performClick()
            .assertTextEquals("true")
    }

    @Test
    fun selectable_clickAction() {
        val selected = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                selected = selected.value,
                onClick = { selected.value = !selected.value },
                modifier = Modifier.testTag("surface")
            ) { Spacer(Modifier.size(30.dp)) }
        }
        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(selected.value).isTrue()

        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(selected.value).isFalse()
    }

    @Test
    fun selectable_clickOutsideShapeBounds() {
        val selected = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                selected = selected.value,
                onClick = { selected.value = !selected.value },
                modifier = Modifier.testTag("surface"),
                shape = CircleShape
            ) { Spacer(Modifier.size(100.dp)) }
        }
        // Click inside the circular shape bounds. Expecting a selection change.
        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(selected.value).isTrue()

        // Click outside the circular shape bounds. Expecting a selection to stay as it.
        rule.onNodeWithTag("surface").performTouchInput { click(Offset(10f, 10f)) }
        Truth.assertThat(selected.value).isTrue()
    }

    @Test
    fun selectable_smallTouchTarget_clickOutsideShapeBounds() {
        val selected = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                selected = selected.value,
                onClick = { selected.value = !selected.value },
                modifier = Modifier.testTag("surface"),
                shape = CircleShape
            ) { Spacer(Modifier.size(40.dp)) }
        }
        // Click inside the circular shape bounds. Expecting a selection change.
        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(selected.value).isTrue()

        // Click outside the circular shape bounds. Still expecting a selection change, as the
        // touch target has a minimum size of 48dp.
        rule.onNodeWithTag("surface").performTouchInput { click(Offset(2f, 2f)) }
        Truth.assertThat(selected.value).isFalse()
    }

    private fun ComposeContentTestRule.setMaterialContent(
        colorScheme: ColorScheme = lightColorScheme(),
        content: @Composable () -> Unit
    ) {
        setContent {
            MaterialTheme(
                colorScheme = colorScheme,
                content = content
            )
        }
    }
}
