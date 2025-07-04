/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.SplitButtonDefaults.OuterCornerSize
import androidx.compose.material3.SplitButtonDefaults.SmallInnerCornerSize
import androidx.compose.material3.tokens.SplitButtonSmallTokens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class SplitButtonTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun filledSplitButton_contentDisplay() {
        rule.setMaterialContent(lightColorScheme()) {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leadingButton"),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Leading Icon")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("My Button")
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        modifier = Modifier.size(34.dp).testTag("trailingButton"),
                        checked = false,
                        onCheckedChange = {},
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                },
            )
        }

        rule.onNodeWithText("My Button").assertIsDisplayed()
        rule.onNodeWithTag("leadingButton").assertHasClickAction()
        rule.onNodeWithTag("trailingButton").assertHasClickAction()
        rule.onNodeWithContentDescription("Leading Icon").assertIsDisplayed()
        rule.onNodeWithContentDescription("Trailing Icon").assertIsDisplayed()
    }

    @Test
    fun filledSplitButton_trailingButtonChecked() {
        rule.setMaterialContent(lightColorScheme()) {
            var trailingButtonChecked by remember { mutableStateOf(false) }

            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leadingButton"),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Leading Icon")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("My Button")
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        modifier = Modifier.size(34.dp).testTag("trailingButton"),
                        checked = trailingButtonChecked,
                        onCheckedChange = { trailingButtonChecked = !trailingButtonChecked },
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                },
            )
        }

        rule
            .onNode(isToggleable())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun filledSplitButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leading button"),
                    ) {
                        Text("My Button")
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        onCheckedChange = {},
                        checked = false,
                        modifier = Modifier.size(34.dp).testTag("trailing button"),
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                },
            )
        }

        rule.onNodeWithTag("leading button").apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsEnabled()
        }
        rule.onNodeWithTag("trailing button").apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsEnabled()
        }
    }

    @Test
    fun filledSplitButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leading button"),
                        enabled = false,
                    ) {
                        Text("My Button")
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        onCheckedChange = {},
                        checked = false,
                        modifier = Modifier.size(34.dp).testTag("trailing button"),
                        enabled = false,
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                },
            )
        }

        rule.onNodeWithTag("leading button").apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsNotEnabled()
        }
        rule.onNodeWithTag("trailing button").apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsNotEnabled()
        }
    }

    @Test
    fun TonalSplitButton_contentDisplay() {
        rule.setMaterialContent(lightColorScheme()) {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.TonalLeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leadingButton"),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Leading Icon")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("My Button")
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TonalTrailingButton(
                        modifier = Modifier.size(34.dp).testTag("trailingButton"),
                        checked = false,
                        onCheckedChange = {},
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                },
            )
        }

        rule.onNodeWithText("My Button").assertIsDisplayed()
        rule.onNodeWithContentDescription("Leading Icon").assertIsDisplayed()
        rule.onNodeWithContentDescription("Trailing Icon").assertIsDisplayed()
    }

    @Test
    fun ElevatedSplitButton_contentDisplay() {
        rule.setMaterialContent(lightColorScheme()) {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.ElevatedLeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leadingButton"),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Leading Icon")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("My Button")
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.ElevatedTrailingButton(
                        modifier = Modifier.size(34.dp).testTag("trailingButton"),
                        checked = false,
                        onCheckedChange = {},
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                },
            )
        }

        rule.onNodeWithText("My Button").assertIsDisplayed()
        rule.onNodeWithContentDescription("Leading Icon").assertIsDisplayed()
        rule.onNodeWithContentDescription("Trailing Icon").assertIsDisplayed()
    }

    @Test
    fun OutlinedSplitButton_contentDisplay() {
        rule.setMaterialContent(lightColorScheme()) {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.OutlinedLeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leadingButton"),
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Leading Icon")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("My Button")
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.OutlinedTrailingButton(
                        modifier = Modifier.size(34.dp).testTag("trailingButton"),
                        checked = false,
                        onCheckedChange = {},
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                },
            )
        }

        rule.onNodeWithText("My Button").assertIsDisplayed()
        rule.onNodeWithContentDescription("Leading Icon").assertIsDisplayed()
        rule.onNodeWithContentDescription("Trailing Icon").assertIsDisplayed()
    }

    @Test
    fun FilledSplitButton_contentPadding() {
        lateinit var density: Density
        var trailingButtonSize by mutableStateOf(Size(0f, 0f))

        rule.setMaterialContent(lightColorScheme()) {
            density = LocalDensity.current
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leading button"),
                        enabled = false,
                    ) {
                        Text(
                            "My Button",
                            modifier =
                                Modifier.testTag(TextTag).semantics(mergeDescendants = true) {},
                        )
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        onCheckedChange = {},
                        checked = false,
                        modifier =
                            Modifier.layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    val width = placeable.width
                                    val height = placeable.height
                                    trailingButtonSize = Size(width.toFloat(), height.toFloat())
                                    layout(width, height) { placeable.place(0, 0) }
                                }
                                .testTag("trailing button"),
                        enabled = false,
                    ) {
                        Icon(
                            Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Trailing Icon",
                            modifier =
                                Modifier.testTag(IconTag).semantics(mergeDescendants = true) {},
                        )
                    }
                },
            )
        }

        val trailingButtonTopStartPx = SmallInnerCornerSize.toPx(trailingButtonSize, density)
        val trailingButtonTopEndPx = OuterCornerSize.toPx(trailingButtonSize, density) / 2

        val paddingPxCorrection =
            CenterOpticallyCoefficient * (trailingButtonTopStartPx - trailingButtonTopEndPx)

        val expectedTrailingButtonStartPadding =
            with(density) {
                SplitButtonSmallTokens.TrailingButtonLeadingSpace + paddingPxCorrection.toDp()
            }

        val expectedTrailingButtonEndPadding =
            with(density) {
                SplitButtonSmallTokens.TrailingButtonTrailingSpace - paddingPxCorrection.toDp()
            }

        val leadingButtonBounds = rule.onNodeWithTag("leading button").getUnclippedBoundsInRoot()

        val textBounds = rule.onNodeWithTag(TextTag).getUnclippedBoundsInRoot()

        val trailingButtonBounds = rule.onNodeWithTag("trailing button").getUnclippedBoundsInRoot()

        val iconBounds = rule.onNodeWithTag(IconTag).getUnclippedBoundsInRoot()

        (textBounds.left - leadingButtonBounds.left).assertIsEqualTo(
            SplitButtonSmallTokens.LeadingButtonLeadingSpace,
            "start padding for leading button",
        )
        (leadingButtonBounds.right - textBounds.right).assertIsEqualTo(
            SplitButtonSmallTokens.LeadingButtonTrailingSpace,
            "end padding for leading button",
        )
        (iconBounds.left - trailingButtonBounds.left).assertIsEqualTo(
            expectedTrailingButtonStartPadding,
            "start padding for trailing button",
        )
        (trailingButtonBounds.right - iconBounds.right).assertIsEqualTo(
            expectedTrailingButtonEndPadding,
            "end padding for trailing button",
        )
    }

    @Test
    fun filledSplitButton_extraSmall_positioning() {
        val size = SplitButtonDefaults.ExtraSmallContainerHeight

        rule.setMaterialContent(lightColorScheme()) {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leadingButton").heightIn(size),
                        shapes = SplitButtonDefaults.leadingButtonShapesFor(size),
                        contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(size),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Leading Icon",
                            modifier =
                                Modifier.testTag("leadingIcon").semantics(
                                    mergeDescendants = true
                                ) {},
                        )
                        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                        Text(
                            "My Button",
                            modifier =
                                Modifier.testTag("text").semantics(mergeDescendants = true) {},
                        )
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        modifier = Modifier.heightIn(size).testTag("trailingButton"),
                        checked = false,
                        onCheckedChange = {},
                        shapes = SplitButtonDefaults.trailingButtonShapesFor(size),
                        contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(size),
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                },
            )
        }

        val leadingButtonBounds = rule.onNodeWithTag("leadingButton").getUnclippedBoundsInRoot()
        val leadingIconBounds = rule.onNodeWithTag("leadingIcon").getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag("text").getUnclippedBoundsInRoot()

        (leadingIconBounds.left - leadingButtonBounds.left).assertIsEqualTo(
            12.dp,
            "start padding for leading button",
        )
        (textBounds.left - leadingIconBounds.right).assertIsEqualTo(
            4.dp,
            "icon and text between spacing for leading button",
        )
        (leadingButtonBounds.right - textBounds.right).assertIsEqualTo(
            10.dp,
            "end padding for leading button",
        )
    }

    @Test
    fun filledSplitButton_small_positioning() {
        val size = SplitButtonDefaults.SmallContainerHeight

        rule.setMaterialContent(lightColorScheme()) {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leadingButton").heightIn(size),
                        shapes = SplitButtonDefaults.leadingButtonShapesFor(size),
                        contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(size),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Leading Icon",
                            modifier =
                                Modifier.testTag("leadingIcon").semantics(
                                    mergeDescendants = true
                                ) {},
                        )
                        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                        Text(
                            "My Button",
                            modifier =
                                Modifier.testTag("text").semantics(mergeDescendants = true) {},
                        )
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        modifier = Modifier.heightIn(size).testTag("trailingButton"),
                        checked = false,
                        onCheckedChange = {},
                        shapes = SplitButtonDefaults.trailingButtonShapesFor(size),
                        contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(size),
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                },
            )
        }

        val leadingButtonBounds = rule.onNodeWithTag("leadingButton").getUnclippedBoundsInRoot()
        val leadingIconBounds = rule.onNodeWithTag("leadingIcon").getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag("text").getUnclippedBoundsInRoot()

        (textBounds.left - leadingIconBounds.right).assertIsEqualTo(
            8.dp,
            "icon and text between spacing for leading button",
        )
        (leadingIconBounds.left - leadingButtonBounds.left).assertIsEqualTo(
            16.dp,
            "start padding for leading button",
        )
        (leadingButtonBounds.right - textBounds.right).assertIsEqualTo(
            12.dp,
            "end padding for leading button",
        )
    }

    @Test
    fun filledSplitButton_medium_positioning() {
        val size = SplitButtonDefaults.MediumContainerHeight

        rule.setMaterialContent(lightColorScheme()) {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leadingButton").heightIn(size),
                        shapes = SplitButtonDefaults.leadingButtonShapesFor(size),
                        contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(size),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Leading Icon",
                            modifier =
                                Modifier.testTag("leadingIcon").semantics(
                                    mergeDescendants = true
                                ) {},
                        )
                        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                        Text(
                            "My Button",
                            modifier =
                                Modifier.testTag("text").semantics(mergeDescendants = true) {},
                        )
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        modifier = Modifier.heightIn(size).testTag("trailingButton"),
                        checked = false,
                        onCheckedChange = {},
                        shapes = SplitButtonDefaults.trailingButtonShapesFor(size),
                        contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(size),
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                },
            )
        }

        val leadingButtonBounds = rule.onNodeWithTag("leadingButton").getUnclippedBoundsInRoot()
        val leadingIconBounds = rule.onNodeWithTag("leadingIcon").getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag("text").getUnclippedBoundsInRoot()

        (textBounds.left - leadingIconBounds.right).assertIsEqualTo(
            8.dp,
            "icon and text between spacing for leading button",
        )
        (leadingIconBounds.left - leadingButtonBounds.left).assertIsEqualTo(
            24.dp,
            "start padding for leading button",
        )
        (leadingButtonBounds.right - textBounds.right).assertIsEqualTo(
            24.dp,
            "end padding for leading button",
        )
    }

    @Test
    fun filledSplitButton_large_positioning() {
        val size = SplitButtonDefaults.LargeContainerHeight

        rule.setMaterialContent(lightColorScheme()) {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leadingButton").heightIn(size),
                        shapes = SplitButtonDefaults.leadingButtonShapesFor(size),
                        contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(size),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Leading Icon",
                            modifier =
                                Modifier.testTag("leadingIcon").semantics(
                                    mergeDescendants = true
                                ) {},
                        )
                        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                        Text(
                            "My Button",
                            modifier =
                                Modifier.testTag("text").semantics(mergeDescendants = true) {},
                        )
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        modifier = Modifier.heightIn(size).testTag("trailingButton"),
                        checked = false,
                        onCheckedChange = {},
                        shapes = SplitButtonDefaults.trailingButtonShapesFor(size),
                        contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(size),
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                },
            )
        }

        val leadingButtonBounds = rule.onNodeWithTag("leadingButton").getUnclippedBoundsInRoot()
        val leadingIconBounds = rule.onNodeWithTag("leadingIcon").getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag("text").getUnclippedBoundsInRoot()

        (textBounds.left - leadingIconBounds.right).assertIsEqualTo(
            12.dp,
            "icon and text between spacing for leading button",
        )
        (leadingIconBounds.left - leadingButtonBounds.left).assertIsEqualTo(
            48.dp,
            "start padding for leading button",
        )
        (leadingButtonBounds.right - textBounds.right).assertIsEqualTo(
            48.dp,
            "end padding for leading button",
        )
    }

    @Test
    fun filledSplitButton_extraLarge_positioning() {
        val size = SplitButtonDefaults.ExtraLargeContainerHeight

        rule.setMaterialContent(lightColorScheme()) {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { /* Do Nothing */ },
                        modifier = Modifier.testTag("leadingButton").heightIn(size),
                        shapes = SplitButtonDefaults.leadingButtonShapesFor(size),
                        contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(size),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Leading Icon",
                            modifier =
                                Modifier.testTag("leadingIcon").semantics(
                                    mergeDescendants = true
                                ) {},
                        )
                        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                        Text(
                            "My Button",
                            modifier =
                                Modifier.testTag("text").semantics(mergeDescendants = true) {},
                        )
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        modifier = Modifier.heightIn(size).testTag("trailingButton"),
                        checked = false,
                        onCheckedChange = {},
                        shapes = SplitButtonDefaults.trailingButtonShapesFor(size),
                        contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(size),
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Trailing Icon")
                    }
                },
            )
        }

        val leadingButtonBounds = rule.onNodeWithTag("leadingButton").getUnclippedBoundsInRoot()
        val leadingIconBounds = rule.onNodeWithTag("leadingIcon").getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag("text").getUnclippedBoundsInRoot()

        (textBounds.left - leadingIconBounds.right).assertIsEqualTo(
            16.dp,
            "icon and text between spacing for leading button",
        )
        (leadingIconBounds.left - leadingButtonBounds.left).assertIsEqualTo(
            64.dp,
            "start padding for leading button",
        )
        (leadingButtonBounds.right - textBounds.right).assertIsEqualTo(
            64.dp,
            "end padding for leading button",
        )
    }
}

private const val TextTag = "text tag"
private const val IconTag = "icon tag"
