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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.glimmer.testutils.captureToImage
import androidx.xr.glimmer.testutils.createGlimmerRule
import androidx.xr.glimmer.testutils.toIntArray
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class IconButtonTest {

    @get:Rule(0) val rule = createComposeRule(StandardTestDispatcher())
    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun defaultSemantics() {
        rule.setGlimmerThemeContent {
            IconButton(modifier = Modifier.testTag("icon_button"), onClick = {}) {
                Icon(FavoriteIcon, null)
            }
        }

        rule
            .onNodeWithTag("icon_button")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
    }

    @Test
    fun disabledSemantics() {
        rule.setGlimmerThemeContent {
            IconButton(modifier = Modifier.testTag("icon_button"), onClick = {}, enabled = false) {
                Icon(FavoriteIcon, null)
            }
        }

        rule
            .onNodeWithTag("icon_button")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
    }

    @Test
    fun findByContentDescriptionAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        rule.setGlimmerThemeContent {
            IconButton(onClick = onClick) { Icon(FavoriteIcon, "icon_button_description") }
        }

        rule.onNodeWithContentDescription("icon_button_description").performClick()

        rule.runOnIdle { Truth.assertThat(counter).isEqualTo(1) }
    }

    @Test
    fun canBeDisabled() {
        rule.setGlimmerThemeContent {
            var enabled by remember { mutableStateOf(true) }
            val onClick = { enabled = false }
            IconButton(
                modifier = Modifier.testTag("icon_button"),
                onClick = onClick,
                enabled = enabled,
            ) {
                Icon(FavoriteIcon, null)
            }
        }

        rule
            .onNodeWithTag("icon_button")
            // Confirm the button starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .performClick()
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun containerShapeAndColor() {
        val expectedShape = RoundedCornerShape(42.dp)
        val expectedColor = Color.Red

        rule.setGlimmerThemeContent {
            IconButton(
                onClick = {},
                shape = expectedShape,
                color = expectedColor,
                modifier = Modifier.testTag("icon_button"),
                border = null,
            ) {
                Box(Modifier.size(100.dp))
            }
        }

        rule
            .onNodeWithTag("icon_button")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = expectedShape,
                shapeColor = expectedColor,
                backgroundColor = Color.Black,
                antiAliasingGap = with(rule.density) { 1.dp.toPx() },
            )
    }

    @Test
    fun setsContentColorProvider() {
        var actualColor: Color = Color.Unspecified
        val expectedColor = Color.Red

        rule.setGlimmerThemeContent {
            IconButton(onClick = {}, contentColor = expectedColor) {
                Box(
                    DelegatableNodeProviderElement {
                        actualColor = it?.currentContentColor() ?: Color.Unspecified
                    }
                )
            }
        }

        rule.runOnIdle { Truth.assertThat(actualColor).isEqualTo(expectedColor) }
    }

    @Test
    fun defaultInteractionSource_isShared_betweenSurfaceAndClickable() {
        rule.setGlimmerThemeContent(addInitialFocusInterceptor = true) {
            IconButton(onClick = {}, modifier = Modifier.testTag("icon_button")) {
                Icon(FavoriteIcon, null)
            }
        }

        val imageBefore = rule.onNodeWithTag("icon_button").captureToImage()

        rule.onNodeWithTag("icon_button").requestFocus()
        rule.waitForIdle()

        val imageAfter = rule.onNodeWithTag("icon_button").captureToImage()

        val result =
            // Expect similarity < 0.80 due to focused border.
            MSSIMMatcher(threshold = 0.80)
                .compareBitmaps(
                    imageBefore.toIntArray(),
                    imageAfter.toIntArray(),
                    imageBefore.width,
                    imageBefore.height,
                )

        assertThat(result.matches).isFalse()
    }

    @Test
    fun setsLocalIconSize() {
        var actualIconSize: Dp? = null
        val expectedIconSize = 32.dp

        rule.setGlimmerThemeContent {
            IconButton(onClick = {}) { actualIconSize = LocalIconSize.current }
        }

        rule.runOnIdle { actualIconSize!!.assertIsEqualTo(expectedIconSize, "icon size") }
    }
}
