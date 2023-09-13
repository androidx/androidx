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
package androidx.compose.ui.input.pointer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalPointerIconService
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class PointerIconTest {
    @get:Rule
    val rule = createComposeRule()
    private val parentIconTag = "myParentIcon"
    private val childIconTag = "myChildIcon"
    private val grandchildIconTag = "myGrandchildIcon"
    private val desiredParentIcon = PointerIcon.Crosshair // AndroidPointerIcon(type=1007)
    private val desiredChildIcon = PointerIcon.Text // AndroidPointerIcon(type=1008)
    private val desiredGrandchildIcon = PointerIcon.Hand // AndroidPointerIcon(type=1002)
    private val desiredDefaultIcon = PointerIcon.Default // AndroidPointerIcon(type=1000)
    private lateinit var iconService: PointerIconService

    @Before
    fun setup() {
        iconService = object : PointerIconService {
            private var currentIcon: PointerIcon = PointerIcon.Default
            override fun getIcon(): PointerIcon {
                return currentIcon
            }

            override fun setIcon(value: PointerIcon?) {
                currentIcon = value ?: PointerIcon.Default
            }
        }
    }

    @Test
    fun testInspectorValue() {
        isDebugInspectorInfoEnabled = true
        rule.setContent {
            val modifier = Modifier.pointerHoverIcon(
                PointerIcon.Hand,
                overrideDescendants = false
            ) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("pointerHoverIcon")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "icon",
                "overrideDescendants",
            )
        }
        isDebugInspectorInfoEnabled = false
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Because we don't move the cursor, the icon will be the default [PointerIcon.Default]. We
     *  also want to check that when using a .pointerHoverIcon modifier with a composable,
     *  composition only happens once (per composable).
     */
    @Test
    fun parentChildFullOverlap_noOverrideDescendants_checkNumberOfCompositions() {

        var numberOfCompositions = 0

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {

                    numberOfCompositions++

                    Box(
                        Modifier
                            .requiredSize(200.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    ) {
                        numberOfCompositions++
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
            assertThat(numberOfCompositions).isEqualTo(2)
        }
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Child Box’s [PointerIcon.Text] wins for the entire Box area because it’s lower in
     *  the hierarchy than Parent Box. If the Parent Box's overrideDescendants = false, the Child
     *  Box takes priority.
     */
    @Test
    fun parentChildFullOverlap_noOverrideDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Box(
                        Modifier
                            .requiredSize(200.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    )
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify Parent Box is respecting Child Box's icon
        verifyIconOnHover(parentIconTag, desiredChildIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Parent Box’s [PointerIcon.Crosshair] wins for the entire Box area because it’s higher in
     *  the hierarchy than Child Box. Also the Parent Box's overrideDescendants value is TRUE, so
     *  as the topmost parent in the hierarchy with overrideDescendants = true, all its children
     *  must respect it.
     */
    @Test
    fun parentChildFullOverlap_parentOverridesDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = true)
                ) {
                    Box(
                        Modifier
                            .requiredSize(200.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    )
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
        // Verify Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Child Box’s [PointerIcon.Text] wins for the entire Box area because its lower in priority
     *  than Parent Box. If the Parent Box's overrideDescendants = false, the Child Box takes
     *  priority.
     */
    @Test
    fun parentChildFullOverlap_childOverridesDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Box(
                        Modifier
                            .requiredSize(200.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    )
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify Parent Box is respecting Child Box's icon
        verifyIconOnHover(parentIconTag, desiredChildIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Parent Box’s [PointerIcon.Crosshair] wins for the entire Box area because its
     *  overrideDescendants = true. The Parent Box takes precedence because it is the topmost parent
     *  in the hierarchy with overrideDescendants = true.
     */
    @Test
    fun parentChildFullOverlap_bothOverrideDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = true)
                ) {
                    Box(
                        Modifier
                            .requiredSize(200.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    )
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
        // Verify Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Child Box’s [PointerIcon.Hand] wins for the entire Child Box surface area because there's
     *  no parent in its hierarchy that has overrideDescendants = true. Parent Box's
     *  [PointerIcon.Crosshair] wins for all remaining surface area of its Box that doesn't overlap
     *  with Child Box.
     */
    @Test
    fun parentChildPartialOverlap_noOverrideDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(100.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    )
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify remaining Parent Box's area is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Parent Box’s [PointerIcon.Hand] wins for the entire Box area because its
     *  overrideDescendants = true, so every child underneath it in the hierarchy must respect its
     *  pointer icon since it's the topmost parent in the hierarchy with overrideDescendants = true.
     */
    @Test
    fun parentChildPartialOverlap_parentOverridesDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = true)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(100.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    )
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
        // Verify Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Child Box’s [PointerIcon.Hand] wins for the entire Child Box surface area because it’s lower
     *  in the hierarchy than Parent Box. If Parent Box's overrideDescendants = false, the Child
     *  Box takes priority.
     */
    @Test
    fun parentChildPartialOverlap_childOverridesDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(100.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    )
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify remaining Parent Box's area is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Parent Box’s [PointerIcon.Crosshair] wins for the entire Box area because its
     *  overrideDescendants = true. If multiple locations in the hierarchy set
     *  overrideDescendants = true, the highest parent in the hierarchy takes precedence (in this
     *  example, it was Parent Box).
     */
    @Test
    fun parentChildPartialOverlap_bothOverrideDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = true)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(100.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    )
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
        // Verify Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (no custom icon)
     *      ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Initially, the Child Box's [PointerIcon.Text] should win for its entire surface area
     *  because it has no competition in the hierarchy for any other custom icons. After the Parent
     *  Box dynamically has the pointerHoverIcon Modifier added to it, the Parent Box's
     *  [PointerIcon.Crosshair] should win for the entire surface area of the Parent Box and Child
     *  Box because the Parent Box has overrideDescendants = true.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Crosshair])
     */
    @Test
    fun parentChildPartialOverlap_parentModifierDynamicallyAdded() {
        val isVisible = mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .then(
                            if (isVisible.value) Modifier.pointerHoverIcon(
                                desiredParentIcon,
                                overrideDescendants = true
                            ) else Modifier
                        )

                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    )
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Child Box is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify Parent Box's icon is the desired default icon
        verifyIconOnHover(parentIconTag, desiredDefaultIcon)
        // Dynamically add the pointerHoverIcon Modifier to the Parent Box
        rule.runOnIdle {
            isVisible.value = true
        }
        // Verify Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
        // Verify Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (no custom icon)
     *      ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Initially, the Child Box's [PointerIcon.Text] should win for its entire surface area
     *  because it has no competition in the hierarchy for any other custom icons. After the Parent
     *  Box dynamically has the pointerHoverIcon Modifier added to it, the Parent Box's
     *  [PointerIcon.Crosshair] should win for the entire surface area of the Parent Box and Child
     *  Box because the Parent Box has overrideDescendants = true.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Crosshair])
     */
    @Ignore("b/299482894 - not yet implemented")
    @Test
    fun parentChildPartialOverlap_parentModifierDynamicallyAddedWithMoveEvents() {
        val isVisible = mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .then(
                            if (isVisible.value) Modifier.pointerHoverIcon(
                                desiredParentIcon,
                                overrideDescendants = true
                            ) else Modifier
                        )

                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    )
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over Child Box and verify it has the desired child icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            enter(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move to Parent Box and verify its icon is the desired default icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Move back to the Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(center)
        }
        // Dynamically add the pointerHoverIcon Modifier to the Parent Box
        rule.runOnIdle {
            isVisible.value = true
        }
        // Verify the Child Box has updated to respect the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move within the Child Box and verify it is still respecting the desired parent icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        // Move to the Parent Box and verify it also has the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Exit hovering over Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *      ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  After several assertions, it reverts back to false in the parent:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *
     *  Expected Output:
     *  Initially, the Child Box's [PointerIcon.Text] should win for its entire surface area
     *  because the parent does not override descendants. After the Parent Box dynamically changes
     *  overrideDescendants to true, the Parent Box's [PointerIcon.Crosshair] should win for the
     *  entire surface area of the Parent Box and Child Box because the Parent Box has
     *  overrideDescendants = true.
     *
     *  It should then revert back to Child Box's [PointerIcon.Text] after the Parent Box's
     *  overrideDescendants is set back to false.
     *
     */
    @Test
    fun parentChildPartialOverlap_parentModifierDynamicallyChangedToOverrideWithMoveEvents() {
        var parentOverrideDescendants by mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .then(
                            Modifier.pointerHoverIcon(
                                desiredParentIcon,
                                overrideDescendants = parentOverrideDescendants
                            )
                        )

                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    )
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over Child Box and verify it has the desired child icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            enter(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move to Parent Box and verify its icon is the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move back to the Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(center)
        }

        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }

        // Dynamically change the pointerHoverIcon Modifier to the Parent Box to
        // override descendants.
        rule.runOnIdle {
            parentOverrideDescendants = true
        }

        // Verify the Child Box has updated to respect the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }

        // Move within the Child Box and verify it is still respecting the desired parent icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomRight)
        }

        // Verify the Child Box has updated to respect the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }

        // Move to the Parent Box and verify it also has the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }

        // Move within the Child Box and verify it is still respecting the desired parent icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomRight)
        }

        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }

        // Dynamically change the pointerHoverIcon Modifier to the Parent Box to NOT
        // override descendants.
        rule.runOnIdle {
            parentOverrideDescendants = false
        }

        // Verify it's changed to child icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }

        // Move to Parent Box and verify its icon is the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move back to the Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(center)
        }

        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }

        // Exit hovering over Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     *  The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *      ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Initially, Child Box’s [PointerIcon.Hand] wins for the entire Child Box surface area because
     *  there's no parent in its hierarchy that has overrideDescendants = true. Additionally, Parent
     *  Box's [PointerIcon.Crosshair] would initially win for all remaining surface area of its Box
     *  that doesn't overlap with Child Box. Once Parent Box's overrideDescendants parameter is
     *  dynamically updated to true, the Parent Box's icon should win for its entire surface area,
     *  including within Child Box.
     */
    @Test
    fun parentChildPartialOverlap_parentOverrideDescendantsDynamicallyUpdated() {
        val parentOverrideState = mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(
                            desiredParentIcon,
                            overrideDescendants = parentOverrideState.value
                        )
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    )
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify remaining Parent Box's area is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
        rule.runOnIdle {
            parentOverrideState.value = true
        }
        // Verify Child Box's icon is the desired parent icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
        // Verify Parent Box also has the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *      ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  After hovering over various parts of the screen and verify the results, we update the
     *  parent's overrideDescendants to true:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  After several assertions, it reverts back to false in the parent:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *
     *  Expected Output:
     *  Initially, the Child Box's [PointerIcon.Text] should win for its entire surface area
     *  because the parent does not override descendants. After the Parent Box dynamically changes
     *  overrideDescendants to true, the Parent Box's [PointerIcon.Crosshair] should win for the
     *  child's surface area within the Parent Box BUT NOT the portion of the Child Box that is
     *  outside the Parent Box.
     *
     *  It should then revert back to Child Box's [PointerIcon.Text] (in all scenarios) after the
     *  Parent Box's overrideDescendants is set back to false.
     *
     */
    @Test
    fun parentChildPartialOverlapAndExtendsBeyondParent_dynamicOverrideDescendants() {
        var parentOverrideDescendants by mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {

                Box(
                    modifier = Modifier
                        .requiredSize(300.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Green)))

                ) {
                    // This child extends beyond the borders of the parent (enabling this test)
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .testTag(parentIconTag)
                            .then(
                                Modifier.pointerHoverIcon(
                                    desiredParentIcon,
                                    overrideDescendants = parentOverrideDescendants
                                )
                            )

                    ) {
                        Box(
                            Modifier
                                .padding(20.dp)
                                .offset(100.dp)
                                .width(300.dp)
                                .height(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over Child Box and verify it has the desired child icon (outside parent)
        rule.onNodeWithTag(childIconTag).performMouseInput {
            enter(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }

        // Hover over Child Box and verify it has the desired child icon (inside parent)
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomLeft)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }

        // Move to Parent Box and verify its icon is the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move back to the Child Box (portion inside parent)
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomLeft)
        }

        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }

        // Dynamically change the pointerHoverIcon Modifier of the Parent Box to
        // override descendants.
        rule.runOnIdle {
            parentOverrideDescendants = true
        }

        // Verify the Child Box has updated to respect the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }

        // Hover over Child Box and verify it has the desired child icon (outside parent)
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomRight)
        }

        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }

        // Move to the Parent Box and verify it also has the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }

        // Move within the Child Box (portion inside parent) and verify it is still
        // respecting the desired parent icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomLeft)
        }

        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }

        // Dynamically change the pointerHoverIcon Modifier of the Parent Box to NOT
        // override descendants.
        rule.runOnIdle {
            parentOverrideDescendants = false
        }

        // Verify it's changed to child icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }

        // Move to Parent Box and verify its icon is the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move back to the Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomLeft)
        }

        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }

        // Exit hovering over Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *    ⤷ ChildB Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  ChildA Box’s [PointerIcon.Text] wins for the entire surface area of ChildA's Box. ChildB
     *  Box's [PointerIcon.Hand] wins for the entire surface area of ChildB's Box.
     *  [PointerIcon.Default] wins for the remainder of the surface area of Parent Box that's not
     *  covered by ChildA Box or ChildB Box. In this example, there's no competition for pointer
     *  icons because the parent has no icon set and neither ChildA or ChildB Boxes overlap.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *    ⤷ Child Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun NonOverlappingSiblings_noOverrideDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Column {
                        Box(
                            Modifier
                                .padding(20.dp)
                                .requiredSize(50.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                        )
                        // Referencing grandchild tag/icon for ChildB in this test
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(50.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = false
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify ChildA Box's icon is the desired ChildA icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify ChildB Box's icon is the desired ChildB icon
        verifyIconOnHover(grandchildIconTag, desiredGrandchildIcon)
        // Verify remaining Parent Box's icon is the default icon
        verifyIconOnHover(parentIconTag, desiredDefaultIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *    ⤷ ChildB Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  ChildA Box’s [PointerIcon.Text] wins for the entire surface area of ChildA's Box. ChildB
     *  Box's [PointerIcon.Hand] wins for the entire surface area of ChildB's Box.
     *  [PointerIcon.Default] wins for the remainder of the surface area of Parent Box that's not
     *  covered by ChildA Box or ChildB Box. In this example, it doesn't matter whether ChildA Box's
     *  overrideDescendants = true or false because there's no competition for pointer icons in
     *  this example.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *    ⤷ Child Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun NonOverlappingSiblings_firstChildOverridesDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Column {
                        Box(
                            Modifier
                                .padding(20.dp)
                                .requiredSize(50.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                        )
                        // Referencing grandchild tag/icon for ChildB in this test
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(50.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = false
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify ChildA Box's icon is the desired ChildA icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify ChildB Box's icon is the desired ChildB icon
        verifyIconOnHover(grandchildIconTag, desiredGrandchildIcon)
        // Verify remaining Parent Box's icon is the default icon
        verifyIconOnHover(parentIconTag, desiredDefaultIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *    ⤷ ChildB Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  ChildA Box’s [PointerIcon.Text] wins for the entire surface area of ChildA's Box. ChildB
     *  Box's [PointerIcon.Hand] wins for the entire surface area of ChildB's Box.
     *  [PointerIcon.Default] wins for the remainder of the surface area of Parent Box that's not
     *  covered by ChildA Box or ChildB Box. In this example, it doesn't matter whether ChildB Box's
     *  overrideDescendants = true or false because there's no competition for pointer icons in
     *  this example.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *    ⤷ Child Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun NonOverlappingSiblings_secondChildOverridesDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Column {
                        Box(
                            Modifier
                                .padding(20.dp)
                                .requiredSize(50.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                        )
                        // Referencing grandchild tag/icon for ChildB in this test
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(50.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(desiredGrandchildIcon, overrideDescendants = true)
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify ChildA Box's icon is the desired ChildA icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify ChildB Box's icon is the desired ChildB icon
        verifyIconOnHover(grandchildIconTag, desiredGrandchildIcon)
        // Verify remaining Parent Box's icon is the default icon
        verifyIconOnHover(parentIconTag, desiredDefaultIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *    ⤷ ChildB Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  ChildA Box’s [PointerIcon.Text] wins for the entire surface area of ChildA's Box. ChildB
     *  Box's [PointerIcon.Hand] wins for the entire surface area of ChildB's Box.
     *  [PointerIcon.Default] wins for the remainder of the surface area of Parent Box that's not
     *  covered by ChildA Box or ChildB Box. In this example, it doesn't matter whether ChildA Box
     *  and ChildB Box's overrideDescendants = true or false because there's no competition for
     *  pointer icons in this example.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ ChildA Box (output icon = [PointerIcon.Text])
     *    ⤷ ChildB Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun NonOverlappingSiblings_bothOverrideDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Column {
                        Box(
                            Modifier
                                .padding(20.dp)
                                .requiredSize(50.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                        )
                        // Referencing grandchild tag/icon for ChildB in this test
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(50.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(desiredGrandchildIcon, overrideDescendants = true)
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify ChildA Box's icon is the desired ChildA icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify ChildB Box's icon is the desired ChildB icon
        verifyIconOnHover(grandchildIconTag, desiredGrandchildIcon)
        // Verify remaining Parent Box's icon is the default icon
        verifyIconOnHover(parentIconTag, desiredDefaultIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *    ⤷ ChildB Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE) where
     *        ChildB Box's surface area overlaps with its sibling, ChildA, within the Parent Box
     *
     *  Expected Output:
     *  ChildB Box's [PointerIcon.Hand] wins for the entire surface area of ChildB's Box.
     *  ChildA Box's [PointerIcon.Text] wins for the remaining surface area of ChildA Box not
     *  covered by ChildB Box. [PointerIcon.Default] wins for the remainder of the surface area of
     *  Parent Box that's not covered by ChildA Box or ChildB Box.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ ChildA Box (output icon = [PointerIcon.Text])
     *    ⤷ ChildB Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun OverlappingSiblings_noOverrideDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(120.dp, 60.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    )
                    // Referencing grandchild tag/icon for ChildB in this test
                    Box(
                        Modifier
                            .padding(horizontal = 100.dp, vertical = 40.dp)
                            .requiredSize(120.dp, 20.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                            .testTag(grandchildIconTag)
                            .pointerHoverIcon(desiredGrandchildIcon, overrideDescendants = false)
                    )
                }
            }
        }

        verifyOverlappingSiblings()
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *    ⤷ ChildB Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE) where
     *        ChildB Box's surface area overlaps with its sibling, ChildA, within the Parent Box
     *
     *  Expected Output:
     *  ChildB Box's [PointerIcon.Hand] wins for the entire surface area of ChildB's Box.
     *  ChildA Box's [PointerIcon.Text] wins for the remaining surface area of ChildA Box not
     *  covered by ChildB Box. [PointerIcon.Default] wins for the remainder of the surface area of
     *  Parent Box that's not covered by ChildA Box or ChildB Box. The overrideDescendants param
     *  only affects that element's children. So in this example, it doesn't matter whether ChildA
     *  Box's overrideDescendants = true because ChildB is its sibling and is therefore unaffected
     *  by this param.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ ChildA Box (output icon = [PointerIcon.Text])
     *    ⤷ ChildB Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun OverlappingSiblings_childAOverridesDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(120.dp, 60.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    )
                    // Referencing grandchild tag/icon for ChildB in this test
                    Box(
                        Modifier
                            .padding(horizontal = 100.dp, vertical = 40.dp)
                            .requiredSize(120.dp, 20.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                            .testTag(grandchildIconTag)
                            .pointerHoverIcon(desiredGrandchildIcon, overrideDescendants = false)
                    )
                }
            }
        }

        verifyOverlappingSiblings()
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *    ⤷ ChildB Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE) where
     *        ChildB Box's surface area overlaps with its sibling, ChildA, within the Parent Box
     *
     *  Expected Output:
     *  ChildB Box's [PointerIcon.Hand] wins for the entire surface area of ChildB's Box.
     *  ChildA Box's [PointerIcon.Text] wins for the remaining surface area of ChildA Box not
     *  covered by ChildB Box. [PointerIcon.Default] wins for the remainder of the surface area of
     *  Parent Box that's not covered by ChildA Box or ChildB Box. The overrideDescendants param
     *  only affects that element's children. So in this example, it doesn't matter whether ChildB
     *  Box's overrideDescendants = true because ChildA is its sibling and is therefore unaffected
     *  by this param.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ ChildA Box (output icon = [PointerIcon.Text])
     *    ⤷ ChildB Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun OverlappingSiblings_childBOverridesDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(120.dp, 60.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    )
                    // Referencing grandchild tag/icon for ChildB in this test
                    Box(
                        Modifier
                            .padding(horizontal = 100.dp, vertical = 40.dp)
                            .requiredSize(120.dp, 20.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                            .testTag(grandchildIconTag)
                            .pointerHoverIcon(desiredGrandchildIcon, overrideDescendants = true)
                    )
                }
            }
        }

        verifyOverlappingSiblings()
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *    ⤷ ChildB Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE) where
     *        ChildB Box's surface area overlaps with its sibling, ChildA, within the Parent Box
     *
     *  Expected Output:
     *  ChildB Box's [PointerIcon.Hand] wins for the entire surface area of ChildB's Box.
     *  ChildA Box's [PointerIcon.Text] wins for the remaining surface area of ChildA Box not
     *  covered by ChildB Box. [PointerIcon.Default] wins for the remainder of the surface area of
     *  Parent Box that's not covered by ChildA Box or ChildB Box. The overrideDescendants param
     *  only affects that element's children. So in this example, it doesn't matter whether ChildA
     *  Box or ChildB Box's overrideDescendants = true because ChildA and ChildB Boxes are siblings
     *  and are unaffected by each other's overrideDescendants param.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ ChildA Box (output icon = [PointerIcon.Text])
     *    ⤷ ChildB Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun OverlappingSiblings_bothOverrideDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(120.dp, 60.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    )
                    // Referencing grandchild tag/icon for ChildB in this test
                    Box(
                        Modifier
                            .padding(horizontal = 100.dp, vertical = 40.dp)
                            .requiredSize(120.dp, 20.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                            .testTag(grandchildIconTag)
                            .pointerHoverIcon(desiredGrandchildIcon, overrideDescendants = true)
                    )
                }
            }
        }

        verifyOverlappingSiblings()
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *    ⤷ ChildB Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE) where
     *        ChildB Box's surface area overlaps with its sibling, ChildA, within the Parent Box
     *
     *  Expected Output:
     *  Parent Box's [PointerIcon.Crosshair] wins for the entire surface area of its box, including
     *  the surface area within ChildA Box and ChildB Box. Parent Box has overrideDescendants =
     *  true, which takes priority over any custom icon set by its children.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ ChildA Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ ChildB Box (output icon = [PointerIcon.Crosshair])
     */
    @Test
    fun OverlappingSiblings_parentOverridesDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = true)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(120.dp, 60.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    )
                    // Referencing grandchild tag/icon for ChildB in this test
                    Box(
                        Modifier
                            .padding(horizontal = 100.dp, vertical = 40.dp)
                            .requiredSize(120.dp, 20.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                            .testTag(grandchildIconTag)
                            .pointerHoverIcon(desiredGrandchildIcon, overrideDescendants = true)
                    )
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over ChildB (bottom right corner) and verify desired Parent icon
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            enter(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Then hover to parent (bottom right corner) and icon hasn't changed
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Then hover to ChildA (bottom left corner) and verify icon hasn't changed
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomLeft)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Exit hovering
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ Child Box (no custom icon set)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Grandchild Box’s [PointerIcon.Hand] wins for the entire surface area of Grandchild's Box.
     *  [PointerIcon.Default] wins for the remainder of the surface area of Parent Box that isn't
     *  covered by Grandchild Box.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ Child Box (output icon = [PointerIcon.Default])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun multiLayeredNesting_grandchildCustomIconNoOverride() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = false
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Grandchild Box's icon is the desired grandchild icon
        verifyIconOnHover(grandchildIconTag, desiredGrandchildIcon)
        // Verify remaining Child Box's area is the default arrow icon
        verifyIconOnHover(childIconTag, desiredDefaultIcon)
        // Verify remaining Parent Box's area is the default arrow icon
        verifyIconOnHover(parentIconTag, desiredDefaultIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ Child Box (no custom icon set)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Grandchild Box’s [PointerIcon.Hand] wins for the entire surface area of Grandchild's Box.
     *  [PointerIcon.Default] wins for the remainder of the surface area of Parent Box that isn't
     *  covered by Grandchild Box.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ Child Box (output icon = [PointerIcon.Default])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun multiLayeredNesting_grandchildCustomIconHasOverride() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = true
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Grandchild Box's icon is the desired grandchild icon
        verifyIconOnHover(grandchildIconTag, desiredGrandchildIcon)
        // Verify remaining Child Box's area is the default arrow icon
        verifyIconOnHover(childIconTag, desiredDefaultIcon)
        // Verify remaining Parent Box's area is the default arrow icon
        verifyIconOnHover(parentIconTag, desiredDefaultIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Grandchild Box's [PointerIcon.Hand] wins for the entire surface area of the Grandchild Box.
     *  Child Box’s [PointerIcon.Text] wins for remaining surface area of its Box not covered by the
     *  Grandchild Box. [PointerIcon.Default] wins for the remainder of the surface area of Parent
     *  Box that isn't covered by Child Box.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun multiLayeredNesting_childAndGrandchildCustomIconsNoOverrides() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = false
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Grandchild Box's icon is the desired grandchild icon
        verifyIconOnHover(grandchildIconTag, desiredGrandchildIcon)
        // Verify remaining Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify remaining Parent Box's area is the default arrow icon
        verifyIconOnHover(parentIconTag, desiredDefaultIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Grandchild Box's [PointerIcon.Hand] wins for the entire surface area of the Grandchild Box.
     *  Child Box’s [PointerIcon.Text] wins for the remainder of the Child Box's surface area
     *  that's not covered by the Grandchild box. [PointerIcon.Default] wins for the remainder of
     *  the surface area of Parent Box that isn't covered by Child Box.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun multiLayeredNesting_childCustomIconGrandchildHasOverride() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = true
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Grandchild Box's icon is the desired grandchild icon
        verifyIconOnHover(grandchildIconTag, desiredGrandchildIcon)
        // Verify remaining Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify remaining Parent Box's area is the default arrow icon
        verifyIconOnHover(parentIconTag, desiredDefaultIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Child Box’s [PointerIcon.Text] wins for the entire surface area of its Box (including all
     *  of the Grandchild Box since it is contained within Child Box's surface area).
     *  [PointerIcon.Default] wins for the remainder of the surface area of Parent Box that isn't
     *  covered by Child Box.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Text])
     */
    @Test
    fun multiLayeredNesting_grandchildCustomIconChildHasOverride() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = false
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify Grandchild Box is respecting Child Box's icon
        verifyIconOnHover(grandchildIconTag, desiredChildIcon)
        // Verify remaining Parent Box's area is the default arrow icon
        verifyIconOnHover(parentIconTag, desiredDefaultIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (no custom icon set)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Child Box’s [PointerIcon.Text] wins for the entire surface area of its Box (including all
     *  of the Grandchild Box since it is contained within Child Box's surface area).
     *  [PointerIcon.Default] wins for the remainder of the surface area of Parent Box that isn't
     *  covered by Child Box.
     *
     *  Parent Box (output icon = [PointerIcon.Default])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Text])
     */
    @Test
    fun multiLayeredNesting_childAndGrandchildOverrideDescendants() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = true
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify Grandchild Box is respecting Child Box's icon
        verifyIconOnHover(grandchildIconTag, desiredChildIcon)
        // Verify remaining Parent Box's area is the default arrow icon
        verifyIconOnHover(parentIconTag, desiredDefaultIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (no icon set)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Grandchild Box's [PointerIcon.Hand] wins for the entire surface area of the Grandchild Box.
     *  Parent Box’s [PointerIcon.Crosshair] wins for the remaining surface area of the Pare Box
     *  that's not covered by the Grandchild Box.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Crosshair])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun multiLayeredNesting_parentAndGrandchildCustomIconNoOverrides() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = false
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Grandchild Box's icon is the desired grandchild icon
        verifyIconOnHover(grandchildIconTag, desiredGrandchildIcon)
        // Verify remaining Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
        // Verify remaining Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (no icon set)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Grandchild Box's [PointerIcon.Hand] wins for the entire surface area of its Box. Parent
     *  Box’s [PointerIcon.Crosshair] wins for the remaining surface area of the Pare Box that's
     *  not covered by the Grandchild Box.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Crosshair])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun multiLayeredNesting_parentCustomIconGrandchildOverrides() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = true
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Grandchild Box's icon is the desired grandchild icon
        verifyIconOnHover(grandchildIconTag, desiredGrandchildIcon)
        // Verify remaining Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
        // Verify remaining Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Grandchild Box's [PointerIcon.Hand] wins for the entire surface area of the Grandchild Box.
     *  Child Box's [PointerIcon.Text] wins for the remaining surface area of the Child Box not
     *  covered by the Grandchild Box. Parent Box’s [PointerIcon.Crosshair] wins for the remaining
     *  surface area not covered by the Child Box.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun multiLayeredNesting_allCustomIconsNoOverrides() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = false
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Grandchild Box's icon is the desired grandchild icon
        verifyIconOnHover(grandchildIconTag, desiredGrandchildIcon)
        // Verify remaining Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify remaining Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Grandchild Box's [PointerIcon.Hand] wins for the entire surface area of the Grandchild Box.
     *  Child Box's [PointerIcon.Text] wins for the remaining surface area of the Child Box not
     *  covered by the Grandchild Box. Parent Box’s [PointerIcon.Crosshair] wins for the remaining
     *  surface area not covered by the Child Box.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun multiLayeredNesting_allCustomIconsGrandchildOverrides() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = true
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Grandchild Box's icon is the desired grandchild icon
        verifyIconOnHover(grandchildIconTag, desiredGrandchildIcon)
        // Verify remaining Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify remaining Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Child Box's [PointerIcon.Hand] wins for the entire surface area of its Box. Parent
     *  Box’s [PointerIcon.Crosshair] wins for the remaining surface area of its Box.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Text])
     */
    @Test
    fun multiLayeredNesting_allCustomIconsChildOverrides() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = false
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify Grandchild Box is respecting Child Box's icon
        verifyIconOnHover(grandchildIconTag, desiredChildIcon)
        // Verify remaining Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Child Box's [PointerIcon.Hand] wins for the entire surface area of its Box. Parent
     *  Box’s [PointerIcon.Crosshair] wins for the remaining surface area of its Box. The addition
     *  of Grandchild Box’s overrideDescendants = true in this test doesn’t impact the outcome; this
     *  is because Child Box is Grandchild Box's parent in the hierarchy and it already has
     *  overrideDescendants = true, which takes priority over anything Grandchild Box sets.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Text])
     */
    @Test
    fun multiLayeredNesting_allCustomIconsChildAndGrandchildOverrides() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = true
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Child Box's icon is the desired child icon
        verifyIconOnHover(childIconTag, desiredChildIcon)
        // Verify Grandchild Box is respecting Child Box's icon
        verifyIconOnHover(grandchildIconTag, desiredChildIcon)
        // Verify remaining Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (no icon set)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Parent Box’s [PointerIcon.Crosshair] wins for the entire surface area of its Box. Even
     *  though the Grandchild Box’s icon was set, the Parent Box will always take priority because
     *  it's the highestmost level in the hierarchy where overrideDescendants = true.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Crosshair])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Crosshair])
     */
    @Test
    fun multiLayeredNesting_parentGrandChildCustomIconsParentOverrides() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = true)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = false
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
        // Verify Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
        // Verify Grandchild Box is respecting Parent Box's icon
        verifyIconOnHover(grandchildIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (no icon set)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Parent Box’s [PointerIcon.Crosshair] wins for the entire surface area of its Box. Even
     *  though the Grandchild Box’s icon was set, the Parent Box will always take priority because
     *  it's the highestmost level in the hierarchy where overrideDescendants = true.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Crosshair])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Crosshair])
     */
    @Test
    fun multiLayeredNesting_parentGrandChildCustomIconsBothOverride() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = true)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = true
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
        // Verify Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
        // Verify Grandchild Box is respecting Parent Box's icon
        verifyIconOnHover(grandchildIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Parent Box’s [PointerIcon.Crosshair] wins for the entire surface area of its Box. Even
     *  though the Child and Grandchild Box’s icons were set, the Parent Box will always take
     *  priority because it's the highestmost level in the hierarchy where overrideDescendants =
     *  true.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Crosshair])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Crosshair])
     */
    @Test
    fun multiLayeredNesting_allCustomIconsParentOverrides() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = true)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = false
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
        // Verify Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
        // Verify Grandchild Box is respecting Parent Box's icon
        verifyIconOnHover(grandchildIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Parent Box’s [PointerIcon.Crosshair] wins for the entire surface area of its Box. Even
     *  though the Child and Grandchild Box’s icons were set, the Parent Box will always take
     *  priority because it's the highestmost level in the hierarchy where overrideDescendants =
     *  true.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Crosshair])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Crosshair])
     */
    @Test
    fun multiLayeredNesting_allCustomIconsParentAndGrandchildOverride() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = true)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = true
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
        // Verify Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
        // Verify Grandchild Box is respecting Parent Box's icon
        verifyIconOnHover(grandchildIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Parent Box’s [PointerIcon.Crosshair] wins for the entire surface area of its Box. Even
     *  though the Child and Grandchild Box’s icons were set, the Parent Box will always take
     *  priority because it's the highestmost level in the hierarchy where overrideDescendants =
     *  true.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Crosshair])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Crosshair])
     */
    @Test
    fun multiLayeredNesting_allCustomIconsParentAndChildOverride() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = true)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = false
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
        // Verify Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
        // Verify Grandchild Box is respecting Parent Box's icon
        verifyIconOnHover(grandchildIconTag, desiredParentIcon)
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *        ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Parent Box’s [PointerIcon.Crosshair] wins for the entire surface area of its Box. Even
     *  though the Child and Grandchild Box’s icons were set, the Parent Box will always take
     *  priority because it's the highestmost level in the hierarchy where overrideDescendants =
     *  true.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Crosshair])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Crosshair])
     */
    @Test
    fun multiLayeredNesting_allIconsOverride() {
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = true)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(desiredChildIcon, overrideDescendants = true)
                    ) {
                        Box(
                            Modifier
                                .padding(40.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(grandchildIconTag)
                                .pointerHoverIcon(
                                    desiredGrandchildIcon,
                                    overrideDescendants = true
                                )
                        )
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Verify Parent Box's icon is the desired parent icon
        verifyIconOnHover(parentIconTag, desiredParentIcon)
        // Verify Child Box is respecting Parent Box's icon
        verifyIconOnHover(childIconTag, desiredParentIcon)
        // Verify Grandchild Box is respecting Parent Box's icon
        verifyIconOnHover(grandchildIconTag, desiredParentIcon)
    }

    /**
     * This test takes an existing Box with a custom icon and changes the custom icon to a different
     * custom icon while the cursor is hovered over the box.
     */
    @Test
    fun dynamicallyUpdatedIcon() {
        val icon = mutableStateOf(desiredChildIcon)

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Box(
                        Modifier
                            .padding(20.dp)
                            .requiredSize(100.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(icon.value, overrideDescendants = false)
                    )
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            enter(bottomRight)
        }
        // Verify Child Box has the desired child icon and dynamically update the icon assigned to
        // the Child Box while hovering over Child Box
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
            icon.value = desiredGrandchildIcon
        }
        // Verify the icon has been updated to the desired grandchild icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move cursor within Child Box and verify it still has the updated icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Exit hovering over Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Initially, the Parent Box's [PointerIcon.Crosshair] should win for its entire surface area
     *  because it has no competition in the hierarchy for any other custom icons. After the Child
     *  Box is dynamically added under the cursor, the Child Box's [PointerIcon.Text] should win
     *  for the entire surface area of the Child Box. This also requires updating the user facing
     *  cursor icon to reflect the Child Box that was added under the cursor.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     */
    @Test
    fun dynamicallyAddAndRemoveChild_noOverrideDescendants() {
        val isChildVisible = mutableStateOf(false)

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChildVisible.value) {
                        Box(
                            modifier = Modifier
                                .padding(20.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                        )
                    }
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over center of Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            enter(center)
        }
        // Verify Parent Box has the desired parent icon and dynamically add the Child Box under the
        // cursor
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
            isChildVisible.value = true
        }
        // Verify the icon has been updated to the desired child icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor within Child Box and verify it still has the updated icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor outside Child Box and verify the icon is updated to the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomCenter)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor back to the center of the Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(center)
        }
        // Dynamically remove the Child Box
        rule.runOnIdle {
            isChildVisible.value = false
        }
        // Verify the icon has been updated to the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Exit hovering over Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  The Parent Box's [PointerIcon.Crosshair] should win for its entire surface area regardless
     *  of whether the Child Box is visible or not. This is because the Parent Box's
     *  overrideDescendants = true, so its children should always respect Parent Box's custom icon.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Crosshair])
     */
    @Test
    fun dynamicallyAddAndRemoveChild_parentOverridesDescendants() {
        val isChildVisible = mutableStateOf(false)

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = true),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChildVisible.value) {
                        Box(
                            modifier = Modifier
                                .padding(20.dp)
                                .requiredSize(100.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                        )
                    }
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over center of Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            enter(center)
        }
        // Verify Parent Box has the desired parent icon and dynamically add the Child Box under the
        // cursor
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
            isChildVisible.value = true
        }
        // Verify the icon stays as the parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor within Child Box and verify it still is the parent icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor outside Child Box and verify the icon is updated to the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomCenter)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor back to the center of the Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(center)
        }
        // Dynamically remove the Child Box
        rule.runOnIdle {
            isChildVisible.value = false
        }
        // Verify the icon still the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Exit hovering over Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *      ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Initially, the Parent Box's [PointerIcon.Crosshair] should win for its entire surface area
     *  because it has no competition in the hierarchy for any other custom icons. After the Child
     *  Box and the Grandchild Box are dynamically added under the cursor, the Grandchild Box's
     *  [PointerIcon.Hand] should win for the entire surface area of the Grandchild Box. The Child
     *  Box's [PointerIcon.Text] should win for the remaining surface area of the Child Box not
     *  covered by the Grandchild Box. This also requires updating the user facing cursor icon to
     *  reflect the Child Box and Grandchild Box that were added under the cursor.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *      ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun dynamicallyAddAndRemoveChildAndGrandchild_noOverrideDescendants() {
        val areDescendantsVisible = mutableStateOf(false)

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false),
                    contentAlignment = Alignment.Center
                ) {
                    if (areDescendantsVisible.value) {
                        Box(
                            modifier = Modifier
                                .padding(20.dp)
                                .requiredSize(150.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = false),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(40.dp)
                                    .requiredSize(100.dp)
                                    .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                    .testTag(grandchildIconTag)
                                    .pointerHoverIcon(
                                        desiredGrandchildIcon,
                                        overrideDescendants = false
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over center of Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            enter(center)
        }
        // Verify Parent Box has the desired parent icon and dynamically add the Child Box and
        // Grandchild Box under the cursor
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
            areDescendantsVisible.value = true
        }
        // Verify the icon has been updated to the desired grandchild icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move cursor within Grandchild Box and verify it still has the grandchild icon
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move cursor outside Grandchild Box within Child Box and verify it has the child icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor outside Child Box and verify the icon is updated to the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomCenter)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor back to the center of the Grandchild Box
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(center)
        }
        // Dynamically remove the Child Box and Grandchild Box
        rule.runOnIdle {
            areDescendantsVisible.value = false
        }
        // Verify the icon has been updated to the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Exit hovering over Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *      ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = TRUE)
     *
     *  Expected Output:
     *  Initially, the Parent Box's [PointerIcon.Crosshair] should win for its entire surface area
     *  because it has no competition in the hierarchy for any other custom icons. After the Child
     *  Box and the Grandchild Box are dynamically added under the cursor, the Grandchild Box's
     *  [PointerIcon.Hand] should win for the entire surface area of the Grandchild Box. Because the
     *  Grandchild Box is the lowest level in the hierarchy, the outcome doesn't change whether it
     *  has overrideDescendants = true or not. The Child Box's [PointerIcon.Text] should win for the
     *  remaining surface area of the Child Box not covered by the Grandchild Box. This also
     *  requires updating the user facing cursor icon to reflect the Child Box and Grandchild Box
     *  that were added under the cursor.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *      ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun dynamicallyAddAndRemoveChildAndGrandchild_grandchildOverridesDescendants() {
        val areDescendantsVisible = mutableStateOf(false)

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false),
                    contentAlignment = Alignment.Center
                ) {
                    if (areDescendantsVisible.value) {
                        Box(
                            modifier = Modifier
                                .padding(20.dp)
                                .requiredSize(150.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = false),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(40.dp)
                                    .requiredSize(100.dp)
                                    .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                    .testTag(grandchildIconTag)
                                    .pointerHoverIcon(
                                        desiredGrandchildIcon,
                                        overrideDescendants = true
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over center of Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            enter(center)
        }
        // Verify Parent Box has the desired parent icon and dynamically add the Child Box and
        // Grandchild Box under the cursor
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
            areDescendantsVisible.value = true
        }
        // Verify the icon has been updated to the desired grandchild icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move cursor within Grandchild Box and verify it still has the grandchild icon
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move cursor outside Grandchild Box within Child Box and verify it has the child icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor outside Child Box and verify the icon is updated to the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomCenter)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor back to the center of the Grandchild Box
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(center)
        }
        // Dynamically remove the Child Box and Grandchild Box
        rule.runOnIdle {
            areDescendantsVisible.value = false
        }
        // Verify the icon has been updated to the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Exit hovering over Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *      ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Initially, the Parent Box's [PointerIcon.Crosshair] should win for its entire surface area
     *  because it has no competition in the hierarchy for any other custom icons. After the Child
     *  Box and the Grandchild Box are dynamically added under the cursor, the Child Box's
     *  [PointerIcon.Text] should win for the entire surface area of the Child Box. This includes
     *  the Grandchild Box's [PointerIcon.Text] should win for the remaining surface area of the
     *  Child Box not covered by the Grandchild Box. This also requires updating the user facing
     *  cursor icon to reflect the Child Box and Grandchild Box that were added under the cursor.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *      ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun dynamicallyAddAndRemoveChildAndGrandchild_childOverridesDescendants() {
        val areDescendantsVisible = mutableStateOf(false)

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false),
                    contentAlignment = Alignment.Center
                ) {
                    if (areDescendantsVisible.value) {
                        Box(
                            modifier = Modifier
                                .padding(20.dp)
                                .requiredSize(150.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = true),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(40.dp)
                                    .requiredSize(100.dp)
                                    .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                    .testTag(grandchildIconTag)
                                    .pointerHoverIcon(
                                        desiredGrandchildIcon,
                                        overrideDescendants = false
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over center of Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            enter(center)
        }
        // Verify Parent Box has the desired parent icon, then dynamically add the Child Box and
        // Grandchild Box under the cursor
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
            areDescendantsVisible.value = true
        }
        // Verify the icon has been updated to the desired child icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor within Grandchild Box and verify it still has the child icon
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor outside Grandchild Box within Child Box to verify it still has the child icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor outside Child Box and verify the icon is updated to the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomCenter)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor back to the center of the Grandchild Box
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(center)
        }
        // Dynamically remove the Child Box and Grandchild Box
        rule.runOnIdle {
            areDescendantsVisible.value = false
        }
        // Verify the icon has been updated to the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Exit hovering over Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  The Child Box's [PointerIcon.Text] should win for its entire surface area regardless of
     *  whether there's a Parent Box present or not. This is because the Parent Box has
     *  overrideDescendants = false and should therefore not have its custom icon take priority over
     *  the Child Box's custom icon. The Parent Box's [PointerIcon.Crosshair] should win for its
     *  remaining surface area not covered by the Child Box.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     */
    @Test
    fun dynamicallyAddAndRemoveParent_noOverrideDescendants() {
        val isParentVisible = mutableStateOf(false)
        val child = movableContentOf {
            Box(
                modifier = Modifier
                    .requiredSize(150.dp)
                    .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                    .testTag(childIconTag)
                    .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
            )
        }

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                if (isParentVisible.value) {
                    Box(
                        modifier = Modifier
                            .requiredSize(200.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .testTag(parentIconTag)
                            .pointerHoverIcon(desiredParentIcon, overrideDescendants = false),
                        contentAlignment = Alignment.Center
                    ) {
                        child()
                    }
                } else {
                    child()
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over center of Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            enter(center)
        }
        // Verify Child Box has the desired child icon and dynamically add the Parent Box under the
        // cursor
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
            isParentVisible.value = true
        }
        // Verify the icon stays as the desired child icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor within Child Box and verify it still has the child icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor outside Child Box and verify the icon is updated to the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomCenter)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor back to the center of the Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(center)
        }
        // Dynamically remove the Parent Box
        rule.runOnIdle {
            isParentVisible.value = false
        }
        // Verify the icon stays as the desired child icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Exit hovering over Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = TRUE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  The Child Box's [PointerIcon.Text] should win for its entire surface area when the Parent
     *  Box isn't present. Once the Parent Box becomes visible, the Parent Box's
     *  [PointerIcon.Crosshair] should win for its entire surface area. This is because the Parent
     *  Box's overrideDescendants = true, so its children should always respect Parent Box's custom
     *  icon. This also requires updating the user facing cursor icon to reflect the Parent Box that
     *  was added under the cursor.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Crosshair])
     */
    @Test
    fun dynamicallyAddAndRemoveParent_parentOverridesDescendants() {
        val isParentVisible = mutableStateOf(false)
        val child = movableContentOf {
            Box(
                modifier = Modifier
                    .requiredSize(150.dp)
                    .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                    .testTag(childIconTag)
                    .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
            )
        }

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                if (isParentVisible.value) {
                    Box(
                        modifier = Modifier
                            .requiredSize(200.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .testTag(parentIconTag)
                            .pointerHoverIcon(desiredParentIcon, overrideDescendants = true),
                        contentAlignment = Alignment.Center
                    ) {
                        child()
                    }
                } else {
                    child()
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over center of Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            enter(center)
        }
        // Verify Child Box has the desired child icon and dynamically add the Parent Box under the
        // cursor
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
            isParentVisible.value = true
        }
        // Verify the icon has been updated to the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor within Child Box and verify it still has the parent icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor outside Child Box and verify the icon is still the parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomCenter)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor back to the center of the Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(center)
        }
        // Dynamically remove the Parent Box
        rule.runOnIdle {
            isParentVisible.value = false
        }
        // Verify the icon has been updated to the desired child icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Exit hovering over Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *      ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  The Grandchild Box's [PointerIcon.Hand] should win for its entire surface area regardless of
     *  whether there's a Child Box or Parent Box present. This is because the Parent Box and Child
     *  Box have overrideDescendants = false and should therefore not have their custom icons take
     *  priority over the Grandchild Box's custom icon. The Child Box should win for its remaining
     *  surface area not covered by the Grandchild Box. The Parent Box's [PointerIcon.Crosshair]
     *  should win for its remaining surface area not covered by the Child Box.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *      ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun dynamicallyAddAndRemoveNestedChild_noOverrideDescendants() {
        val isChildVisible = mutableStateOf(false)
        val grandchild = movableContentOf {
            Box(
                modifier = Modifier
                    .requiredSize(100.dp)
                    .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                    .testTag(grandchildIconTag)
                    .pointerHoverIcon(desiredGrandchildIcon, overrideDescendants = false)
            )
        }

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChildVisible.value) {
                        Box(
                            modifier = Modifier
                                .requiredSize(150.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = false),
                            contentAlignment = Alignment.Center
                        ) {
                            grandchild()
                        }
                    } else {
                        grandchild()
                    }
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over center of Grandchild Box
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            enter(center)
        }
        // Verify Grandchild Box has the desired grandchild icon and dynamically add the Child Box
        // under the cursor
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
            isChildVisible.value = true
        }
        // Verify the icon stays as the desired grandchild icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move cursor within Grandchild Box and verify it still has the grandchild icon
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move cursor outside Grandchild Box within Child Box to verify icon is now the child icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor outside Child Box and verify the icon is updated to the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor back to the center of the Grandchild Box
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(center)
        }
        // Dynamically remove the Child Box
        rule.runOnIdle {
            isChildVisible.value = false
        }
        // Verify the icon has been updated to the desired grandchild icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Exit hovering over Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  After hovering over the center of the screen, the hierarchy under the cursor updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = TRUE)
     *      ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  The Grandchild Box's [PointerIcon.Hand] should win for its entire surface area regardless of
     *  whether there's a Child Box or Parent Box present. This is because the Parent Box and Child
     *  Box have overrideDescendants = false and should therefore not have thei custom icona take
     *  priority over the Grandchild Box's custom icon. The Child Box should win for its remaining
     *  surface area not covered by the Grandchild Box. The Parent Box's [PointerIcon.Crosshair]
     *  should win for its remaining surface area not covered by the Child Box.
     *  Initially, the Parent Box's [PointerIcon.Crosshair] should win for its entire surface area
     *  because it has no competition in the hierarchy for any other custom icons. After the Child
     *  Box and the Grandchild Box are dynamically added under the cursor, the Child Box's
     *  [PointerIcon.Text] should win for the entire surface area of the Child Box. This includes
     *  the Grandchild Box's [PointerIcon.Text] should win for the remaining surface area of the Child Box not
     *  covered by the Grandchild Box. This also requires updating the user facing cursor icon to
     *  reflect the Child Box and Grandchild Box that were added under the cursor.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *      ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun dynamicallyAddAndRemoveNestedChild_ChildOverridesDescendants() {
        val isChildVisible = mutableStateOf(false)
        val grandchild = movableContentOf {
            Box(
                modifier = Modifier
                    .requiredSize(100.dp)
                    .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                    .testTag(grandchildIconTag)
                    .pointerHoverIcon(desiredGrandchildIcon, overrideDescendants = false)
            )
        }

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChildVisible.value) {
                        Box(
                            modifier = Modifier
                                .requiredSize(150.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = true),
                            contentAlignment = Alignment.Center
                        ) {
                            grandchild()
                        }
                    } else {
                        grandchild()
                    }
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over center of Grandchild Box
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            enter(center)
        }
        // Verify Grandchild Box has the desired grandchild icon and dynamically add the Child Box
        // under the cursor
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
            isChildVisible.value = true
        }
        // Verify the icon has been updated to the desired child icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor within Grandchild Box and verify it still has the child icon
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor outside Grandchild Box within Child Box to verify it still has the child icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor outside Child Box and verify the icon is updated to the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomCenter)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor back to center of the Grandchild Box
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(center)
        }
        // Dynamically remove the Child Box
        rule.runOnIdle {
            isChildVisible.value = false
        }
        // Verify the icon has been updated to the desired grandchild icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Exit hovering over Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Grandparent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *      ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  After hovering over the corner of the Grandparent Box that doesn't overlap with any
     *  descendant, the hierarchy of the screen updates to:
     *  Grandparent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *      ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *         ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  The Grandchild Box's [PointerIcon.Hand] should win for its entire surface area regardless of
     *  whether there's a Child, Parent, or Grandparent Box present. This is because the
     *  Grandparent, Parent, and Child Boxes have overrideDescendants = false and should therefore
     *  not have their custom icons take priority over the Grandchild Box's custom icon. The Child
     *  Box should win for its remaining surface area not covered by the Grandchild Box. The Parent
     *  Box's [PointerIcon.Crosshair] should win for its remaining surface area not covered by the
     *  Child Box. And the Grandparent Box should win for its remaining surface area not covered by
     *  the Parent Box.
     *
     *  Grandparent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Parent Box (output icon = [PointerIcon.Crosshair])
     *      ⤷ Child Box (output icon = [PointerIcon.Text])
     *        ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun dynamicallyAddAndRemoveNestedChild_notHoveredOverChild() {
        val grandparentIconTag = "myGrandparentIcon"
        val desiredGrandparentIcon = desiredParentIcon
        val isChildVisible = mutableStateOf(false)
        val grandchild = movableContentOf {
            Box(
                modifier = Modifier
                    .requiredSize(100.dp)
                    .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                    .testTag(grandchildIconTag)
                    .pointerHoverIcon(desiredGrandchildIcon, overrideDescendants = false)
            )
        }

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(grandparentIconTag)
                        .pointerHoverIcon(desiredGrandparentIcon, overrideDescendants = false),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .requiredSize(175.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .testTag(parentIconTag)
                            .pointerHoverIcon(desiredParentIcon, overrideDescendants = false),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChildVisible.value) {
                            Box(
                                modifier = Modifier
                                    .requiredSize(150.dp)
                                    .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                    .testTag(childIconTag)
                                    .pointerHoverIcon(
                                        desiredChildIcon,
                                        overrideDescendants = false
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                grandchild()
                            }
                        } else {
                            grandchild()
                        }
                    }
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over center of Grandchild Box
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            enter(center)
        }
        // Verify Grandchild Box has the desired grandchild icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move to corner of Grandparent Box where no descendants are under the cursor
        rule.onNodeWithTag(grandparentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        // Verify the icon is the desired grandparent icon and dynamically add the Child Box
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandparentIcon)
            isChildVisible.value = true
        }
        // Verify the icon stays as the desired grandparent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandparentIcon)
        }
        // Move cursor within Grandparent Box and verify it still has the grandparent icon
        rule.onNodeWithTag(grandparentIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandparentIcon)
        }
        // Move cursor outside Grandparent Box to Parent Box to verify icon is now the parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor back to corner of Grandparent Box where no descendants are under the cursor
        rule.onNodeWithTag(grandparentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        // Dynamically remove the Child Box
        rule.runOnIdle {
            isChildVisible.value = false
        }
        // Verify the icon stays as the grandparent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandparentIcon)
        }
        // Exit hovering over Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  After hovering over the corner of the Parent Box that doesn't overlap with any descendant,
     *  the hierarchy of the screen updates to:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *      ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  The Grandchild Box's [PointerIcon.Hand] should win for its entire surface area regardless of
     *  whether there's a Child or Parent Box present. This is because the Parent and Child Boxes
     *  have overrideDescendants = false and should therefore not have their custom icons take
     *  priority over the Grandchild Box's custom icon. The Child Box should win for its remaining
     *  surface area not covered by the Grandchild Box. The Parent Box's [PointerIcon.Crosshair]
     *  should win for its remaining surface area not covered by the Child Box.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *      ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun dynamicallyAddAndRemoveGrandchild_notHoveredOverGrandchild() {
        val isGrandchildVisible = mutableStateOf(false)

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .requiredSize(150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                            .testTag(childIconTag)
                            .pointerHoverIcon(
                                desiredChildIcon,
                                overrideDescendants = false
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGrandchildVisible.value) {
                            Box(
                                modifier = Modifier
                                    .requiredSize(100.dp)
                                    .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                    .testTag(grandchildIconTag)
                                    .pointerHoverIcon(
                                        desiredGrandchildIcon,
                                        overrideDescendants = false
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over center of Child Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            enter(center)
        }
        // Verify Child Box has the desired child icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move to corner of Parent Box where no descendants are under the cursor
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        // Verify the icon is the desired parent icon and dynamically add the Grandchild Box
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
            isGrandchildVisible.value = true
        }
        // Verify the icon stays as the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor within Parent Box and verify it still has the grandparent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move cursor outside Parent Box to Child Box to verify icon is now the child icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor back to corner of Parent Box where no descendants are under the cursor
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        // Dynamically remove the Grandchild Box
        rule.runOnIdle {
            isGrandchildVisible.value = false
        }
        // Verify the icon stays as the parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Exit hovering over Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  After hovering over the area where ChildB will be, the hierarchy of the screen updates to:
     *  Parent Box (no custom icon set)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *    ⤷ ChildB Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Regardless of the presence of ChildB Box, ChildA Box's [PointerIcon.Text] should win for
     *  its entire surface area. Once ChildB Box appears, ChildB Box's [PointerIcon.Hand] should
     *  win for its entire surface area. Initially, Parent Box's [PointerIcon.Crosshair] should win
     *  for its entire surface area not covered by ChildA Box. Once ChildA Box appears, Parent Box
     *  should win for its entire surface not covered by either ChildA or ChildB Boxes.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *    ⤷ Child Box (output icon = [PointerIcon.Hand])
     */
    @Ignore("b/271277248 - Remove Ignore annotation once input event bug is fixed")
    @Test
    fun dynamicallyAddAndRemoveSibling_hoveredOverAppearingSibling() {
        val isChildBVisible = mutableStateOf(false)

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(150.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Column {
                        Box(
                            Modifier
                                .requiredSize(50.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(
                                    desiredChildIcon,
                                    overrideDescendants = false
                                )
                        )
                        if (isChildBVisible.value) {
                            // Referencing grandchild tag/icon for ChildB in this test
                            Box(
                                Modifier
                                    .requiredSize(50.dp)
                                    .offset(y = 100.dp)
                                    .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                    .testTag(grandchildIconTag)
                                    .pointerHoverIcon(
                                        desiredGrandchildIcon,
                                        overrideDescendants = false
                                    )
                            )
                        }
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over corner of Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            enter(bottomRight)
        }
        // Verify Parent Box has the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move to center of ChildA Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(center)
        }
        // Verify ChildA Box has the desired child icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move to left corner of Parent Box where ChildB will be added
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomLeft)
        }
        // Dynamically add the ChildB Box
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
            isChildBVisible.value = true
        }
        // Verify the icon is updated to the desired ChildB icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move to corner of ChildB Box
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        // Verify ChildB Box has the desired grandchild icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move cursor back to the center of ChildA Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(center)
        }
        // Verify that icon is updated to the desired ChildA icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor back to the location of ChildB
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(center)
        }
        // Dynamically remove the ChildB Box
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
            isChildBVisible.value = false
        }
        // Verify the icon updates to the parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Exit hovering over ChildA Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for the initial setup of this test is:
     *  Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *
     *  After hovering over ChildA, the hierarchy of the screen updates to:
     *  Parent Box (no custom icon set)
     *    ⤷ ChildA Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *    ⤷ ChildB Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Regardless of the presence of ChildB Box, ChildA Box's [PointerIcon.Text] should win for
     *  its entire surface area. Once ChildB Box appears, ChildB Box's [PointerIcon.Hand] should
     *  win for its entire surface area. Initially, Parent Box's [PointerIcon.Crosshair] should win
     *  for its entire surface area not covered by ChildA Box. Once ChildA Box appears, Parent Box
     *  should win for its entire surface not covered by either ChildA or ChildB Boxes.
     *
     *  Parent Box (output icon = [PointerIcon.Crosshair])
     *    ⤷ Child Box (output icon = [PointerIcon.Text])
     *    ⤷ Child Box (output icon = [PointerIcon.Hand])
     */
    @Ignore("b/271277248 - Remove Ignore annotation once input event bug is fixed")
    @Test
    fun dynamicallyAddAndRemoveSibling_notHoveredOverAppearingSibling() {
        val isChildBVisible = mutableStateOf(false)

        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .requiredSize(200.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                        .testTag(parentIconTag)
                        .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                ) {
                    Column {
                        Box(
                            Modifier
                                .requiredSize(50.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(
                                    desiredChildIcon,
                                    overrideDescendants = false
                                )
                        )
                        if (isChildBVisible.value) {
                            // Referencing grandchild tag/icon for ChildB in this test
                            Box(
                                Modifier
                                    .requiredSize(50.dp)
                                    .offset(y = 100.dp)
                                    .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                    .testTag(grandchildIconTag)
                                    .pointerHoverIcon(
                                        desiredGrandchildIcon,
                                        overrideDescendants = false
                                    )
                            )
                        }
                    }
                }
            }
        }
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over corner of Parent Box
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            enter(bottomRight)
        }
        // Verify Parent Box has the desired parent icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
        // Move to center of ChildA Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(center)
        }
        // Verify ChildA Box has the desired child icon and dynamically add the ChildB Box
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
            isChildBVisible.value = true
        }
        // Verify the icon stays as the desired child icon since the cursor hasn't moved
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move to corner of ChildB Box
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        // Verify ChildB Box has the desired grandchild icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move cursor back to the center of ChildA Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(center)
        }
        // Dynamically remove the ChildB Box
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
            isChildBVisible.value = false
        }
        // Verify the icon stays as the desired child icon since the cursor hasn't moved
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Exit hovering over ChildA Box
        rule.onNodeWithTag(childIconTag).performMouseInput {
            exit()
        }
    }

    /**
     * Setup:
     * The hierarchy for this test is setup as:
     *  Default Box (no custom icon set)
     *    ⤷ Parent Box (custom icon = [PointerIcon.Crosshair], overrideDescendants = FALSE)
     *        ⤷ Child Box (custom icon = [PointerIcon.Text], overrideDescendants = FALSE)
     *            ⤷ Grandchild Box (custom icon = [PointerIcon.Hand], overrideDescendants = FALSE)
     *
     *  Expected Output:
     *  Grandchild Box's [PointerIcon.Hand] wins for the entire surface area of the Grandchild Box.
     *  Child Box's [PointerIcon.Text] wins for the remaining surface area of the Child Box not
     *  covered by the Grandchild Box. Parent Box’s [PointerIcon.Crosshair] wins for the remaining
     *  surface area not covered by the Child Box. [PointerIcon.Default] wins for the remaining
     *  surface area of
     *
     *  Default Box (output icon = [PointerIcon.Default]
     *    ⤷ Parent Box (output icon = [PointerIcon.Crosshair])
     *        ⤷ Child Box (output icon = [PointerIcon.Text])
     *            ⤷ Grandchild Box (output icon = [PointerIcon.Hand])
     */
    @Test
    fun childNotFullyContainedInParent_noOverrideDescendants() {
        val defaultIconTag = "myDefaultWrapper"
        rule.setContent {
            CompositionLocalProvider(LocalPointerIconService provides iconService) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(BorderStroke(2.dp, SolidColor(Color.Yellow)))
                        .testTag(defaultIconTag)
                ) {
                    Box(
                        modifier = Modifier
                            .requiredSize(width = 200.dp, height = 150.dp)
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .testTag(parentIconTag)
                            .pointerHoverIcon(desiredParentIcon, overrideDescendants = false)
                    ) {
                        Box(
                            Modifier
                                .requiredSize(width = 150.dp, height = 125.dp)
                                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                                .testTag(childIconTag)
                                .pointerHoverIcon(desiredChildIcon, overrideDescendants = false)
                        ) {
                            Box(
                                Modifier
                                    .requiredSize(width = 300.dp, height = 100.dp)
                                    .offset(x = 100.dp)
                                    .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                                    .testTag(grandchildIconTag)
                                    .pointerHoverIcon(
                                        desiredGrandchildIcon,
                                        overrideDescendants = false
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over the default wrapping box and verify the cursor is still the default icon
        rule.onNodeWithTag(defaultIconTag).performMouseInput {
            enter(center)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Move cursor to the corner of the Grandchild Box and verify it has the desired grandchild
        // icon
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move cursor to the center right of the Child Box and verify it still has the desired
        // grandchild icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move cursor to the corner of the Child Box and verify it has updated to the desired child
        // icon
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }
        // Move cursor to the center right of the Parent Box and verify it has the desired
        // grandchild icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(centerRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }
        // Move cursor to the corner of the Parent Box and verify it has the desired parent icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredParentIcon)
        }
    }

    @Test
    fun resetPointerIconWhenChildRemoved_parentDoesSetIcon_iconIsHand() {
        val defaultIconTag = "myDefaultWrapper"
        var show by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(
                LocalPointerIconService provides iconService
            ) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .pointerHoverIcon(PointerIcon.Hand)
                    .testTag(defaultIconTag)
                ) {
                    if (show) {
                        Box(
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Text)
                                .size(10.dp, 10.dp)
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            // No mouse movement yet, should be default
            assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Default)
        }

        rule.onNodeWithTag(defaultIconTag).performMouseInput {
            moveTo(Offset(x = 5f, y = 5f))
        }

        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Text)
        }

        show = false

        rule.onNodeWithTag(defaultIconTag).performMouseInput {
            moveTo(Offset(x = 6f, y = 6f))
        }

        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Hand)
        }
    }

    @Test
    fun resetPointerIconWhenChildRemoved_parentDoesNotSetIcon_iconIsDefault() {
        val defaultIconTag = "myDefaultWrapper"
        var show by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(
                LocalPointerIconService provides iconService
            ) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .testTag(defaultIconTag)
                ) {
                    if (show) {
                        Box(
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Text)
                                .size(10.dp, 10.dp)
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            // No mouse movement yet, should be default
            assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Default)
        }

        rule.onNodeWithTag(defaultIconTag).performMouseInput {
            moveTo(Offset(x = 5f, y = 5f))
        }

        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Text)
        }

        show = false

        rule.onNodeWithTag(defaultIconTag).performMouseInput {
            moveTo(Offset(x = 6f, y = 6f))
        }

        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(PointerIcon.Default)
        }
    }

    private fun verifyIconOnHover(tag: String, expectedIcon: PointerIcon) {
        // Hover over element with specified tag
        rule.onNodeWithTag(tag).performMouseInput {
            enter(bottomRight)
        }
        // Verify the current icon is the expected icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(expectedIcon)
        }
        // Exit hovering over element
        rule.onNodeWithTag(tag).performMouseInput {
            exit()
        }
    }

    private fun verifyOverlappingSiblings() {
        // Verify initial state of pointer icon
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }
        // Hover over ChildB (bottom right corner) and verify desired ChildB icon
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            enter(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }

        // Then hover to parent (bottom right corner) and verify default arrow icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomRight)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }

        // Then hover back over ChildB in area that overlaps with sibling (bottom left corner) and
        // verify desired ChildB icon
        rule.onNodeWithTag(grandchildIconTag).performMouseInput {
            moveTo(bottomLeft)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredGrandchildIcon)
        }

        // Then hover to ChildA (bottom left corner) and verify desired ChildA icon (hand)
        rule.onNodeWithTag(childIconTag).performMouseInput {
            moveTo(bottomLeft)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredChildIcon)
        }

        // Then hover over parent (bottom left corner) and verify default arrow icon
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            moveTo(bottomLeft)
        }
        rule.runOnIdle {
            assertThat(iconService.getIcon()).isEqualTo(desiredDefaultIcon)
        }

        // Exit hovering
        rule.onNodeWithTag(parentIconTag).performMouseInput {
            exit()
        }
    }
}
