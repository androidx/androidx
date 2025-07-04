/*
 * Copyright 2025 The Android Open Source Project
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

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.testutils.assertShape
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.indirect.IndirectTouchEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performIndirectTouchEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ListItemTest {
    @get:Rule val rule = createComposeRule()

    // Enter non-touch mode for tests, so that clickables can be focused.
    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @Before
    fun enterNonTouchMode() = InstrumentationRegistry.getInstrumentation().setInTouchMode(false)

    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @After fun resetTouchMode() = InstrumentationRegistry.getInstrumentation().resetInTouchMode()

    @Test
    fun semantics() {
        rule.setGlimmerThemeContent {
            Box { ListItem(modifier = Modifier.testTag("listItem")) { Text("Primary Label") } }
        }

        rule
            .onNodeWithTag("listItem")
            .assert(isFocusable())
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @Test
    fun semantics_clickable() {
        rule.setGlimmerThemeContent {
            Box {
                ListItem(modifier = Modifier.testTag("listItem"), onClick = {}) {
                    Text("Primary Label")
                }
            }
        }

        rule
            .onNodeWithTag("listItem")
            .assert(isFocusable())
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun shapeAndColorFromThemeIsUsed() {
        lateinit var expectedShape: Shape
        val surfaceColor = Color.Blue
        rule.setGlimmerThemeContent {
            GlimmerTheme(Colors(surface = surfaceColor)) {
                expectedShape = GlimmerTheme.shapes.large
                ListItem(modifier = Modifier.testTag("listItem"), border = null) {
                    Box(Modifier.size(10.dp, 10.dp))
                }
            }
        }

        rule
            .onNodeWithTag("listItem")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = expectedShape,
                shapeColor = surfaceColor,
                backgroundColor = Color.Black,
                antiAliasingGap = with(rule.density) { 1.dp.toPx() },
            )
    }

    @Test
    fun setsLocalTextStyle() {
        lateinit var actualPrimaryLabelTextStyle: TextStyle
        lateinit var expectedPrimaryLabelTextStyle: TextStyle
        rule.setGlimmerThemeContent {
            expectedPrimaryLabelTextStyle = GlimmerTheme.typography.bodySmall
            ListItem { actualPrimaryLabelTextStyle = LocalTextStyle.current }
        }

        rule.runOnIdle {
            assertThat(actualPrimaryLabelTextStyle).isEqualTo(expectedPrimaryLabelTextStyle)
        }
    }

    @Test
    fun setsLocalTextStyle_withSupportingLabel() {
        lateinit var actualPrimaryLabelTextStyle: TextStyle
        lateinit var actualSupportingLabelTextStyle: TextStyle
        lateinit var expectedPrimaryLabelTextStyle: TextStyle
        lateinit var expectedSupportingLabelTextStyle: TextStyle
        rule.setGlimmerThemeContent {
            expectedPrimaryLabelTextStyle = GlimmerTheme.typography.titleSmall
            expectedSupportingLabelTextStyle = GlimmerTheme.typography.bodySmall
            ListItem(
                supportingLabel = { actualSupportingLabelTextStyle = LocalTextStyle.current }
            ) {
                actualPrimaryLabelTextStyle = LocalTextStyle.current
            }
        }

        rule.runOnIdle {
            assertThat(actualPrimaryLabelTextStyle).isEqualTo(expectedPrimaryLabelTextStyle)
            assertThat(actualSupportingLabelTextStyle).isEqualTo(expectedSupportingLabelTextStyle)
        }
    }

    @Test
    fun setsContentColor() {
        var primary = Color.Unspecified
        var leadingIconContentColor = Color.Unspecified
        var trailingIconContentColor = Color.Unspecified
        var primaryLabelContentColor = Color.Unspecified
        var supportingLabelContentColor = Color.Unspecified
        rule.setGlimmerThemeContent {
            primary = GlimmerTheme.colors.primary
            ListItem(
                supportingLabel = {
                    Box(
                        DelegatableNodeProviderElement {
                            supportingLabelContentColor =
                                it?.currentContentColor() ?: Color.Unspecified
                        }
                    )
                },
                leadingIcon = {
                    Box(
                        DelegatableNodeProviderElement {
                            leadingIconContentColor = it?.currentContentColor() ?: Color.Unspecified
                        }
                    )
                },
                trailingIcon = {
                    Box(
                        DelegatableNodeProviderElement {
                            trailingIconContentColor =
                                it?.currentContentColor() ?: Color.Unspecified
                        }
                    )
                },
            ) {
                Box(
                    DelegatableNodeProviderElement {
                        primaryLabelContentColor = it?.currentContentColor() ?: Color.Unspecified
                    }
                )
            }
        }

        rule.runOnIdle {
            assertThat(leadingIconContentColor).isEqualTo(primary)
            assertThat(trailingIconContentColor).isEqualTo(primary)
            assertThat(primaryLabelContentColor).isEqualTo(Color.White)
            assertThat(supportingLabelContentColor).isEqualTo(Color.White)
        }
    }

    @Test
    fun setsLocalIconSize() {
        var actualLeadingIconSize: Dp? = null
        var actualTrailingIconSize: Dp? = null
        var expectedIconSize: Dp? = null
        rule.setGlimmerThemeContent {
            expectedIconSize = GlimmerTheme.iconSizes.large
            ListItem(
                leadingIcon = { actualLeadingIconSize = LocalIconSize.current },
                trailingIcon = { actualTrailingIconSize = LocalIconSize.current },
            ) {}
        }

        rule.runOnIdle {
            assertThat(actualLeadingIconSize!!).isEqualTo(expectedIconSize!!)
            assertThat(actualTrailingIconSize!!).isEqualTo(expectedIconSize)
        }
    }

    @Test
    fun emitsFocusInteractions() {
        val interactionSource = MutableInteractionSource()
        val (focusRequester, otherFocusRequester) = FocusRequester.createRefs()

        lateinit var scope: CoroutineScope

        rule.setGlimmerThemeContent {
            scope = rememberCoroutineScope()
            Box {
                ListItem(
                    modifier = Modifier.testTag("listItem").focusRequester(focusRequester),
                    interactionSource = interactionSource,
                ) {
                    Text("Primary Label")
                }
                Box(Modifier.size(100.dp).focusRequester(otherFocusRequester).focusTarget())
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        rule.runOnIdle { otherFocusRequester.requestFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalIndirectTouchTypeApi::class)
    @Test
    fun emitsPressInteractions_clickable() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.setGlimmerThemeContent {
            scope = rememberCoroutineScope()
            Box {
                ListItem(
                    modifier = Modifier.testTag("listItem").focusRequester(focusRequester),
                    interactionSource = interactionSource,
                    onClick = {},
                ) {
                    Text("Primary Label")
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { focusRequester.requestFocus() }

        rule.runOnIdle { interactions.clear() }

        val currentTime = SystemClock.uptimeMillis()

        val down =
            MotionEvent.obtain(
                currentTime, // downTime,
                currentTime, // eventTime,
                MotionEvent.ACTION_DOWN,
                0f,
                0f,
                0,
            )
        down.source = SOURCE_TOUCH_NAVIGATION
        rule.onNodeWithTag("listItem").performIndirectTouchEvent(IndirectTouchEvent(down))

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        val up =
            MotionEvent.obtain(
                currentTime + 200L, // downTime,
                currentTime + 200L, // eventTime,
                MotionEvent.ACTION_UP,
                0f,
                0f,
                0,
            )
        up.source = SOURCE_TOUCH_NAVIGATION
        rule.onNodeWithTag("listItem").performIndirectTouchEvent(IndirectTouchEvent(up))

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun positioning() {
        rule.setGlimmerThemeContent {
            Column {
                Spacer(Modifier.height(10.dp).fillMaxWidth().testTag("spacer"))
                ListItem(modifier = Modifier.testTag("listItem")) {
                    Text("Primary Label", modifier = Modifier.testTag("primaryLabel"))
                }
            }
        }

        val spacerBounds =
            rule.onNodeWithTag("spacer", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val primaryLabelBounds =
            rule.onNodeWithTag("primaryLabel", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val listItemBounds =
            rule.onNodeWithTag("listItem", useUnmergedTree = true).getUnclippedBoundsInRoot()

        // Label should typically be center aligned for list items without a supporting label, since
        // the minimum height of the item should be larger than the height of the labels
        (((listItemBounds.height - primaryLabelBounds.height) / 2f) + listItemBounds.top)
            .assertIsEqualTo(
                primaryLabelBounds.top,
                "Padding between top of list item and top of primary label.",
            )

        (primaryLabelBounds.left - listItemBounds.left).assertIsEqualTo(
            24.dp,
            "Padding between the start of the list item and the start of the primary label.",
        )

        // The width should fill the max width, like with the spacer
        listItemBounds.width.assertIsEqualTo(spacerBounds.width, "width of list item.")
        listItemBounds.height.assertIsEqualTo(72.dp, "height of list item.")
    }

    @Test
    fun positioning_supportingLabel() {
        rule.setGlimmerThemeContent {
            Column {
                Spacer(Modifier.height(10.dp).fillMaxWidth().testTag("spacer"))
                ListItem(
                    modifier = Modifier.testTag("listItem"),
                    supportingLabel = {
                        Text("Supporting Label", modifier = Modifier.testTag("supportingLabel"))
                    },
                ) {
                    Text("Primary Label", modifier = Modifier.testTag("primaryLabel"))
                }
            }
        }

        val spacerBounds =
            rule.onNodeWithTag("spacer", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val primaryLabelBounds =
            rule.onNodeWithTag("primaryLabel", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val supportingLabelBounds =
            rule.onNodeWithTag("supportingLabel", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val listItemBounds =
            rule.onNodeWithTag("listItem", useUnmergedTree = true).getUnclippedBoundsInRoot()

        // Label should be top aligned when the height of the primary and supporting labels is
        // greater than minimum list item height
        (primaryLabelBounds.top - listItemBounds.top).assertIsEqualTo(
            20.dp,
            "Padding between top of list item and top of primary label.",
        )

        (primaryLabelBounds.left - listItemBounds.left).assertIsEqualTo(
            24.dp,
            "Padding between the start of the list item and the start of the primary label.",
        )

        (supportingLabelBounds.left - listItemBounds.left).assertIsEqualTo(
            24.dp,
            "Padding between the start of the list item and the start of the supporting label.",
        )

        primaryLabelBounds.bottom.assertIsEqualTo(
            supportingLabelBounds.top,
            "Padding between the bottom of the primary label and the top of the supporting label.",
        )

        (listItemBounds.bottom - supportingLabelBounds.bottom).assertIsEqualTo(
            20.dp,
            "Padding between bottom of list item and bottom of supporting label.",
        )

        // The width should fill the max width, like with the spacer
        listItemBounds.width.assertIsEqualTo(spacerBounds.width, "width of list item.")
        // Supporting label will likely make the item taller than the minimum height, so just assert
        // we are at least the minimum height
        assertThat(listItemBounds.height.value).isAtLeast(72)
    }

    @Test
    fun positioning_withIcons() {
        rule.setGlimmerThemeContent {
            Column {
                Spacer(Modifier.height(10.dp).fillMaxWidth().testTag("spacer"))
                ListItem(
                    modifier = Modifier.testTag("listItem"),
                    leadingIcon = {
                        Icon(
                            FavoriteIcon,
                            contentDescription = "Localized description",
                            modifier = Modifier.testTag("leadingIcon"),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            FavoriteIcon,
                            contentDescription = "Localized description",
                            modifier = Modifier.testTag("trailingIcon"),
                        )
                    },
                ) {
                    Text("Primary Label", modifier = Modifier.testTag("primaryLabel"))
                }
            }
        }

        val spacerBounds =
            rule.onNodeWithTag("spacer", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val primaryLabelBounds =
            rule.onNodeWithTag("primaryLabel", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val leadingIconBounds =
            rule.onNodeWithTag("leadingIcon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingIconBounds =
            rule.onNodeWithTag("trailingIcon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val listItemBounds =
            rule.onNodeWithTag("listItem", useUnmergedTree = true).getUnclippedBoundsInRoot()

        (leadingIconBounds.top - listItemBounds.top).assertIsEqualTo(
            20.dp,
            "Padding between top of list item and top of leading icon.",
        )

        (leadingIconBounds.left - listItemBounds.left).assertIsEqualTo(
            24.dp,
            "Padding between start of list item and start of leading icon.",
        )

        // Label should typically be center aligned for list items without a supporting label, since
        // the minimum height of the item should be larger than the height of the labels
        (((listItemBounds.height - primaryLabelBounds.height) / 2f) + listItemBounds.top)
            .assertIsEqualTo(
                primaryLabelBounds.top,
                "Padding between top of list item and top of primary label.",
            )

        (primaryLabelBounds.left - leadingIconBounds.right).assertIsEqualTo(
            12.dp,
            "Padding between end of leading icon and start of primary label.",
        )

        (trailingIconBounds.top - listItemBounds.top).assertIsEqualTo(
            20.dp,
            "Padding between top of list item and top of trailing icon.",
        )

        (listItemBounds.right - trailingIconBounds.right).assertIsEqualTo(
            24.dp,
            "Padding between end of leading icon and end of list item.",
        )

        // The width should fill the max width, like with the spacer
        listItemBounds.width.assertIsEqualTo(spacerBounds.width, "width of list item.")
        listItemBounds.height.assertIsEqualTo(
            /* vertical padding * 2 + icon height*/ (20 + 20 + 56).dp,
            "height of list item.",
        )
    }

    @Test
    fun positioning_supportingLabel_withIcons() {
        rule.setGlimmerThemeContent {
            Column {
                Spacer(Modifier.height(10.dp).fillMaxWidth().testTag("spacer"))
                ListItem(
                    modifier = Modifier.testTag("listItem"),
                    supportingLabel = {
                        Text("Supporting Label", modifier = Modifier.testTag("supportingLabel"))
                    },
                    leadingIcon = {
                        Icon(
                            FavoriteIcon,
                            contentDescription = "Localized description",
                            modifier = Modifier.testTag("leadingIcon"),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            FavoriteIcon,
                            contentDescription = "Localized description",
                            modifier = Modifier.testTag("trailingIcon"),
                        )
                    },
                ) {
                    Text("Primary Label", modifier = Modifier.testTag("primaryLabel"))
                }
            }
        }

        val spacerBounds =
            rule.onNodeWithTag("spacer", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val primaryLabelBounds =
            rule.onNodeWithTag("primaryLabel", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val supportingLabelBounds =
            rule.onNodeWithTag("supportingLabel", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val leadingIconBounds =
            rule.onNodeWithTag("leadingIcon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingIconBounds =
            rule.onNodeWithTag("trailingIcon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val listItemBounds =
            rule.onNodeWithTag("listItem", useUnmergedTree = true).getUnclippedBoundsInRoot()

        (leadingIconBounds.top - listItemBounds.top).assertIsEqualTo(
            20.dp,
            "Padding between top of list item and top of leading icon.",
        )

        (leadingIconBounds.left - listItemBounds.left).assertIsEqualTo(
            24.dp,
            "Padding between start of list item and start of leading icon.",
        )

        // Label should be top aligned when the height of the primary and supporting labels is
        // greater than minimum list item height
        (primaryLabelBounds.top - listItemBounds.top).assertIsEqualTo(
            20.dp,
            "Padding between top of list item and top of primary label.",
        )

        (primaryLabelBounds.left - leadingIconBounds.right).assertIsEqualTo(
            12.dp,
            "Padding between end of leading icon and start of primary label.",
        )

        (supportingLabelBounds.left - leadingIconBounds.right).assertIsEqualTo(
            12.dp,
            "Padding between end of leading icon and start of supporting label.",
        )

        primaryLabelBounds.bottom.assertIsEqualTo(
            supportingLabelBounds.top,
            "Padding between the bottom of the primary label and the top of the supporting label.",
        )

        (listItemBounds.bottom - supportingLabelBounds.bottom).assertIsEqualTo(
            20.dp,
            "Padding between bottom of list item and bottom of supporting label.",
        )

        (trailingIconBounds.top - listItemBounds.top).assertIsEqualTo(
            20.dp,
            "Padding between top of list item and top of trailing icon.",
        )

        (listItemBounds.right - trailingIconBounds.right).assertIsEqualTo(
            24.dp,
            "Padding between end of leading icon and end of list item.",
        )

        // The width should fill the max width, like with the spacer
        listItemBounds.width.assertIsEqualTo(spacerBounds.width, "width of list item.")
        // Supporting label will likely make the item taller than the minimum height, so just assert
        // we are at least the minimum height
        assertThat(listItemBounds.height.value).isAtLeast(72)
    }

    @Test
    fun positioning_supportingLabel_withIcons_longText() {
        rule.setGlimmerThemeContent {
            Column {
                Spacer(Modifier.height(10.dp).fillMaxWidth().testTag("spacer"))
                ListItem(
                    modifier = Modifier.testTag("listItem"),
                    supportingLabel = {
                        Text("Supporting Label", modifier = Modifier.testTag("supportingLabel"))
                    },
                    leadingIcon = {
                        Icon(
                            FavoriteIcon,
                            contentDescription = "Localized description",
                            modifier = Modifier.testTag("leadingIcon"),
                        )
                    },
                    trailingIcon = {
                        Icon(
                            FavoriteIcon,
                            contentDescription = "Localized description",
                            modifier = Modifier.testTag("trailingIcon"),
                        )
                    },
                ) {
                    Text(
                        "Primary label with some very long text that will wrap to multiple lines",
                        modifier = Modifier.testTag("primaryLabel"),
                    )
                }
            }
        }

        val spacerBounds =
            rule.onNodeWithTag("spacer", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val primaryLabelBounds =
            rule.onNodeWithTag("primaryLabel", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val supportingLabelBounds =
            rule.onNodeWithTag("supportingLabel", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val leadingIconBounds =
            rule.onNodeWithTag("leadingIcon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trailingIconBounds =
            rule.onNodeWithTag("trailingIcon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val listItemBounds =
            rule.onNodeWithTag("listItem", useUnmergedTree = true).getUnclippedBoundsInRoot()

        (leadingIconBounds.top - listItemBounds.top).assertIsEqualTo(
            20.dp,
            "Padding between top of list item and top of leading icon.",
        )

        (leadingIconBounds.left - listItemBounds.left).assertIsEqualTo(
            24.dp,
            "Padding between start of list item and start of leading icon.",
        )

        // Label should be top aligned when the height of the primary and supporting labels is
        // greater than minimum list item height
        (primaryLabelBounds.top - listItemBounds.top).assertIsEqualTo(
            20.dp,
            "Padding between top of list item and top of primary label.",
        )

        (primaryLabelBounds.left - leadingIconBounds.right).assertIsEqualTo(
            12.dp,
            "Padding between end of leading icon and start of primary label.",
        )

        (supportingLabelBounds.left - leadingIconBounds.right).assertIsEqualTo(
            12.dp,
            "Padding between end of leading icon and start of supporting label.",
        )

        primaryLabelBounds.bottom.assertIsEqualTo(
            supportingLabelBounds.top,
            "Padding between the bottom of the primary label and the top of the supporting label.",
        )

        (listItemBounds.bottom - supportingLabelBounds.bottom).assertIsEqualTo(
            20.dp,
            "Padding between bottom of list item and bottom of supporting label.",
        )

        (trailingIconBounds.top - listItemBounds.top).assertIsEqualTo(
            20.dp,
            "Padding between top of list item and top of trailing icon.",
        )

        (listItemBounds.right - trailingIconBounds.right).assertIsEqualTo(
            24.dp,
            "Padding between end of leading icon and end of list item.",
        )

        // The width should fill the max width, like with the spacer
        listItemBounds.width.assertIsEqualTo(spacerBounds.width, "width of list item.")
        assertThat(listItemBounds.height.value).isAtLeast(72)
    }

    @Test
    fun minHeightCanBeOverridden() {
        rule.setGlimmerThemeContent {
            ListItem(
                contentPadding = PaddingValues(),
                modifier = Modifier.requiredHeightIn(15.dp).testTag("listItem"),
            ) {
                Spacer(Modifier.requiredSize(10.dp))
            }
        }

        rule.onNodeWithTag("listItem").apply {
            with(getBoundsInRoot()) { height.assertIsEqualTo(15.dp, "height") }
        }
    }
}
