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

package androidx.xr.glimmer

import android.os.Build
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.glimmer.testutils.captureToImage
import androidx.xr.glimmer.testutils.createGlimmerRule
import androidx.xr.glimmer.testutils.toIntArray
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class IconMarkerTest {

    @get:Rule(0) val rule = createComposeRule()
    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun check_onClickAction() {
        var counter = 0
        rule.setGlimmerThemeContent {
            IconMarker(
                onClick = { ++counter },
                modifier = Modifier.testTag("test"),
                contentDescription = "test",
            ) {
                Icon(FavoriteIcon, contentDescription = null)
            }
        }

        rule.onNodeWithTag("test").performClick()

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @Test
    fun defaultDot_semantics_clickable() {
        rule.setGlimmerThemeContent {
            IconMarker(
                onClick = {},
                contentDescription = "test",
                modifier = Modifier.testTag("marker"),
            )
        }

        rule
            .onNodeWithTag("marker")
            .assertContentDescriptionEquals("test")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun defaultDot_semantics_nonClickable() {
        rule.setGlimmerThemeContent {
            IconMarker(contentDescription = "test", modifier = Modifier.testTag("marker"))
        }

        rule
            .onNodeWithTag("marker")
            .assertContentDescriptionEquals("test")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun customIcon_semantics_clickable() {
        rule.setGlimmerThemeContent {
            IconMarker(
                modifier = Modifier.testTag("marker"),
                contentDescription = "test",
                onClick = {},
            ) {
                Icon(imageVector = FavoriteIcon, contentDescription = null)
            }
        }

        rule
            .onNodeWithTag("marker")
            .assertContentDescriptionEquals("test")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun customIcon_semantics_nonClickable() {
        rule.setGlimmerThemeContent {
            IconMarker(modifier = Modifier.testTag("marker"), contentDescription = "test") {
                Icon(imageVector = FavoriteIcon, contentDescription = null)
            }
        }

        rule
            .onNodeWithTag("marker")
            .assertContentDescriptionEquals("test")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun semantics_mergesDescendants_clickable() {
        rule.setGlimmerThemeContent {
            IconMarker(
                onClick = {},
                contentDescription = "tag-1",
                modifier = Modifier.testTag("marker"),
            ) {
                Icon(
                    imageVector = FavoriteIcon,
                    contentDescription = "tag-2",
                )
            }
        }

        rule
            .onNodeWithTag("marker")
            .assertContentDescriptionEquals("tag-1", "tag-2")
            .onChildren()
            .assertCountEquals(0)
    }

    @Test
    fun semantics_mergesDescendants_nonClickable() {
        rule.setGlimmerThemeContent {
            IconMarker(
                contentDescription = "tag-1",
                modifier = Modifier.testTag("marker"),
            ) {
                Icon(
                    imageVector = FavoriteIcon,
                    contentDescription = "tag-2",
                )
            }
        }

        rule
            .onNodeWithTag("marker")
            .assertContentDescriptionEquals("tag-1", "tag-2")
            .onChildren()
            .assertCountEquals(0)
    }

    @Test
    fun defaultInteractionSource_isShared_betweenSurfaceAndClickable() {
        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconMarker(
                onClick = {},
                modifier = Modifier.testTag("marker"),
                contentDescription = "test",
            ) {
                Icon(FavoriteIcon, contentDescription = null)
            }
        }

        val imageBefore = rule.onNodeWithTag("marker").captureToImage()

        rule.onNodeWithTag("marker").requestFocus()
        rule.waitForIdle()

        val imageAfter = rule.onNodeWithTag("marker").captureToImage()

        val result =
            MSSIMMatcher(threshold = 0.85)
                .compareBitmaps(
                    imageBefore.toIntArray(),
                    imageAfter.toIntArray(),
                    imageBefore.width,
                    imageBefore.height,
                )

        assertThat(result.matches).isFalse()
    }

    @Test
    fun customInteractionSource_isPropagated() {
        val interactionSource = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()
        rule.setGlimmerThemeContent {
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interactions.add(it) }
            }
            IconMarker(
                onClick = {},
                contentDescription = "test",
                modifier = Modifier.testTag("marker"),
                interactionSource = interactionSource,
            ) {
                Icon(FavoriteIcon, contentDescription = null)
            }
        }

        rule.onNodeWithTag("marker").performClick()
        rule.runOnIdle {
            assertThat(interactions).isNotEmpty()
            assertThat(interactions.any { it is PressInteraction.Press }).isTrue()
        }
    }

    @Test
    fun customIcon_nonClickable_setsContentColor() {
        var actualColor: Color = Color.Unspecified
        val expectedColor = Color.Red

        rule.setGlimmerThemeContent {
            IconMarker(contentDescription = "test", contentColor = expectedColor) {
                Box(
                    DelegatableNodeProviderElement {
                        actualColor = it?.currentContentColor() ?: Color.Unspecified
                    }
                )
            }
        }

        rule.runOnIdle { assertThat(actualColor).isEqualTo(expectedColor) }
    }

    @Test
    fun customIcon_clickable_color() {
        val expectedColor = Color.Red

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconMarker(
                onClick = {},
                contentDescription = "test",
                color = expectedColor,
                modifier = Modifier.testTag("marker"),
            ) {
                // empty to not cover background
            }
        }

        val image = rule.onNodeWithTag("marker").captureToImage()
        val centerColor = image.toPixelMap().run { get(width / 2, height / 2) }
        assertThat(centerColor).isEqualTo(expectedColor)
    }

    @Test
    fun customIcon_clickable_setsContentColor() {
        var actualColor: Color = Color.Unspecified
        val expectedColor = Color.Red

        rule.setGlimmerThemeContent {
            IconMarker(onClick = {}, contentDescription = "test", contentColor = expectedColor) {
                Box(
                    DelegatableNodeProviderElement {
                        actualColor = it?.currentContentColor() ?: Color.Unspecified
                    }
                )
            }
        }

        rule.runOnIdle { assertThat(actualColor).isEqualTo(expectedColor) }
    }

    @Test
    fun customIcon_clickable_contentColor() {
        val expectedColor = Color.Red

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconMarker(
                onClick = {},
                contentDescription = "test",
                color = expectedColor,
                modifier = Modifier.testTag("marker"),
            ) {
                // empty to not cover background
            }
        }

        val image = rule.onNodeWithTag("marker").captureToImage()
        val centerColor = image.toPixelMap().run { get(width / 2, height / 2) }
        assertThat(centerColor).isEqualTo(expectedColor)
    }

    @Test
    fun defaultDot_nonClickable_contentColor() {
        val expectedColor = Color.Red

        rule.setGlimmerThemeContent {
            IconMarker(
                contentDescription = "test",
                contentColor = expectedColor,
                modifier = Modifier.testTag("marker"),
            )
        }

        val image = rule.onNodeWithTag("marker").captureToImage()
        val centerColor = image.toPixelMap().run { get(width / 2, height / 2) }
        assertThat(centerColor).isEqualTo(expectedColor)
    }

    @Test
    fun defaultDot_clickable_contentColor() {
        val expectedColor = Color.Red

        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconMarker(
                onClick = {},
                contentDescription = "test",
                contentColor = expectedColor,
                modifier = Modifier.testTag("marker"),
            )
        }

        val image = rule.onNodeWithTag("marker").captureToImage()
        val centerColor = image.toPixelMap().run { get(width / 2, height / 2) }
        assertThat(centerColor).isEqualTo(expectedColor)
    }

    @Test
    fun iconMarkerDefaults_contentPadding_unknownSize_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContent { IconMarkerDefaults.contentPadding(IconMarkerSize(3)) }
        }
    }

    @Test
    fun iconMarkerDefaults_iconSize_unknownSize_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.setContent { IconMarkerDefaults.iconSize(IconMarkerSize(3)) }
        }
    }
}
