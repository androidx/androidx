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

package androidx.compose.material3

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScrimTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())

    private val defaultTag = "scrimTag"

    @Test
    fun scrim_onClick_callsOnDismissRequest() {
        var dismissCount = 0
        rule.setContent {
            Scrim(
                contentDescription = "Close",
                modifier = Modifier.testTag(defaultTag),
                onClick = { dismissCount++ },
            )
        }

        // Verify click action exists via semantics
        rule.onNodeWithTag(defaultTag).assertHasClickAction().performClick()

        // Verify callback invoked
        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun scrim_tapGesture_callsOnDismissRequest() {
        // While semantics handle accessibility clicks, we also need to verify
        // the pointerInput (tap) logic for touch users.
        var dismissCount = 0
        rule.setContent {
            Box(Modifier.size(100.dp)) {
                Scrim(
                    contentDescription = "Close",
                    modifier = Modifier.testTag(defaultTag),
                    onClick = { dismissCount++ },
                )
            }
        }

        rule.onNodeWithTag(defaultTag).performTouchInput { click() }

        assertThat(dismissCount).isEqualTo(1)
    }

    @Test
    fun scrim_semantics_contentDescription_isSet() {
        val description = "Close sheet"
        rule.setContent {
            Scrim(contentDescription = description, modifier = Modifier.testTag(defaultTag))
        }

        rule.onNodeWithTag(defaultTag).assert(hasContentDescription(description))
    }

    @Test
    fun scrim_nullOnDismissRequest_noSemanticsOrClick() {
        rule.setContent {
            Scrim(
                contentDescription = "Decor",
                modifier = Modifier.testTag(defaultTag),
                onClick = null,
            )
        }

        rule
            .onNodeWithTag(defaultTag)
            .assert(!hasClickAction())
            .assert(
                !hasContentDescription("Decor")
            ) // Description is only set inside the dismiss block
    }

    @Test
    fun scrim_unspecifiedColor_doesNotRender() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Scrim(
                    contentDescription = "Close",
                    modifier = Modifier.testTag(defaultTag),
                    color = Color.Unspecified,
                )
            }
        }

        rule.onNodeWithTag(defaultTag).assertDoesNotExist()
    }

    @Test
    fun scrim_traversalIndex_isSet() {
        rule.setContent {
            Scrim(contentDescription = "Close", modifier = Modifier.testTag(defaultTag))
        }

        rule
            .onNodeWithTag(defaultTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 1f))
    }

    @Test
    fun scrim_fillMaxSize_byDefault() {
        val containerSize = 200.dp
        rule.setContent {
            Box(Modifier.size(containerSize)) {
                Scrim(contentDescription = null, modifier = Modifier.testTag(defaultTag))
            }
        }

        rule
            .onNodeWithTag(defaultTag)
            .assertWidthIsEqualTo(containerSize)
            .assertHeightIsEqualTo(containerSize)
    }
}
