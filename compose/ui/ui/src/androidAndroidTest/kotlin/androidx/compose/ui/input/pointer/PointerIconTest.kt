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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
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
    private val desiredParentIcon = PointerIcon.Crosshair
    private val desiredChildIcon = PointerIcon.Text
    private val desiredGrandchildIcon = PointerIcon.Hand
    private val desiredDefaultIcon = PointerIcon.Default
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