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

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.tokens.NavigationBarTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class NavigationItemScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun navigationItem_startIcon() {
        assertNavigationItemMatches(
            iconPosition = NavigationItemIconPosition.Start,
            hasLabel = true,
            goldenIdentifier = "navigationItem_startIcon",
        )
    }

    @Test
    fun navigationItem_topIcon() {
        assertNavigationItemMatches(
            iconPosition = NavigationItemIconPosition.Top,
            hasLabel = true,
            goldenIdentifier = "navigationItem_topIcon",
        )
    }

    @Test
    fun navigationItem_iconOnly() {
        assertNavigationItemMatches(
            iconPosition = NavigationItemIconPosition.Start,
            hasLabel = false,
            goldenIdentifier = "navigationItem_iconOnly",
        )
    }

    private fun assertNavigationItemMatches(
        iconPosition: NavigationItemIconPosition,
        hasLabel: Boolean,
        goldenIdentifier: String,
    ) {
        rule.setMaterialContent(lightColorScheme()) {
            Surface(Modifier.testTag(Tag)) {
                Box(Modifier.padding(8.dp)) {
                    SampleNavigationItem(iconPosition = iconPosition, hasLabel = hasLabel)
                }
            }
        }

        rule
            .onNodeWithTag(Tag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }

    @Composable
    private fun SampleNavigationItem(iconPosition: NavigationItemIconPosition, hasLabel: Boolean) {
        val minHeight =
            if (iconPosition == NavigationItemIconPosition.Top && hasLabel) 80.dp else 64.dp
        val topIconItemPadding =
            if (hasLabel) PaddingValues(vertical = 4.dp) else PaddingValues(0.dp)

        NavigationItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            labelTextStyle = NavigationBarTokens.LabelTextFont.value,
            indicatorShape = CircleShape,
            indicatorWidth = 56.dp,
            indicatorPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            indicatorToLabelVerticalPadding = 4.dp,
            startIconToLabelHorizontalPadding = 8.dp,
            topIconItemPadding = topIconItemPadding,
            startIconItemPadding = PaddingValues(0.dp),
            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier =
                Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    .defaultMinSize(minWidth = ItemMinWidth, minHeight = minHeight),
            enabled = true,
            label = if (hasLabel) ({ Text("Home") }) else null,
            iconPosition = iconPosition,
            interactionSource = remember { MutableInteractionSource() },
        )
    }
}

private const val Tag = "NavigationItem"
private val ItemMinWidth = 160.dp
