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

package androidx.wear.compose.material3.lazy

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ResponsiveContentPaddingTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun calculatesTopPaddingCorrectlyForAllTypes() {
        val expectedValues =
            mapOf(
                ResponsiveItemType.Card to 23.dp, // 23%
                ResponsiveItemType.Button to 23.dp, // 23%
                ResponsiveItemType.ButtonGroup to 23.dp, // 23%
                ResponsiveItemType.CompactButton to 13.dp, // 13%
                ResponsiveItemType.ListHeader to 13.dp, // 13%
                ResponsiveItemType.Default to 0.dp,
            )
        val currentType = mutableStateOf(ResponsiveItemType.Default)
        lateinit var paddingValues: PaddingValues
        rule.setContentWithScreenHeight(SCREEN_HEIGHT_DP) {
            paddingValues =
                rememberResponsiveContentPadding(
                    firstItemTypeProvider = { currentType.value },
                    lastItemTypeProvider = { ResponsiveItemType.Default },
                )
        }

        expectedValues.forEach { (type, expectedDp) ->
            rule.runOnIdle { currentType.value = type }
            assertThat(paddingValues.calculateTopPadding()).isEqualTo(expectedDp)
        }
    }

    @Test
    fun calculatesBottomPaddingCorrectlyForAllTypes() {
        val expectedValues =
            mapOf(
                ResponsiveItemType.Card to 23.dp, // 23%
                ResponsiveItemType.Button to 23.dp, // 23%
                ResponsiveItemType.ButtonGroup to 23.dp, // 23%
                ResponsiveItemType.ListHeader to 23.dp, // Note: Header is 23% on bottom
                ResponsiveItemType.CompactButton to 13.dp, // 13%
                ResponsiveItemType.Default to 0.dp,
            )
        val currentType = mutableStateOf(ResponsiveItemType.Default)
        lateinit var paddingValues: PaddingValues
        rule.setContentWithScreenHeight(SCREEN_HEIGHT_DP) {
            paddingValues =
                rememberResponsiveContentPadding(
                    firstItemTypeProvider = { ResponsiveItemType.Default },
                    lastItemTypeProvider = { currentType.value },
                )
        }

        expectedValues.forEach { (type, expectedDp) ->
            rule.runOnIdle { currentType.value = type }
            assertThat(paddingValues.calculateBottomPadding()).isEqualTo(expectedDp)
        }
    }

    @Test
    fun respectsMinimumContentPaddingWhenLarger() {
        val firstType = ResponsiveItemType.CompactButton // 13% of 100dp = 13dp
        val minTopPadding = 20.dp // Larger than 13dp
        lateinit var paddingValues: PaddingValues

        rule.setContentWithScreenHeight(SCREEN_HEIGHT_DP) {
            paddingValues =
                rememberResponsiveContentPadding(
                    firstItemTypeProvider = { firstType },
                    lastItemTypeProvider = { ResponsiveItemType.Default },
                    minimumContentPadding = PaddingValues(top = minTopPadding),
                )
        }

        // Should take the max(13, 20) -> 20
        assertThat(paddingValues.calculateTopPadding()).isEqualTo(minTopPadding)
    }

    @Test
    fun ignoresMinimumContentPaddingWhenSmaller() {
        val firstType = ResponsiveItemType.Card // 23% of 100dp = 23dp
        val minTopPadding = 10.dp // Smaller than 23dp
        lateinit var paddingValues: PaddingValues

        rule.setContentWithScreenHeight(SCREEN_HEIGHT_DP) {
            paddingValues =
                rememberResponsiveContentPadding(
                    firstItemTypeProvider = { firstType },
                    lastItemTypeProvider = { ResponsiveItemType.Default },
                    minimumContentPadding = PaddingValues(top = minTopPadding),
                )
        }

        // Should take the max(23, 10) -> 23
        assertThat(paddingValues.calculateTopPadding()).isEqualTo(23.dp)
    }

    @Test
    fun passesThroughHorizontalPadding() {
        lateinit var paddingValues: PaddingValues
        val horizontalPadding = 123.dp

        rule.setContent {
            paddingValues =
                rememberResponsiveContentPadding(
                    firstItemTypeProvider = { null },
                    lastItemTypeProvider = { null },
                    minimumContentPadding = PaddingValues(horizontal = horizontalPadding),
                )
        }

        val layoutDir = LayoutDirection.Ltr
        assertThat(paddingValues.calculateLeftPadding(layoutDir)).isEqualTo(horizontalPadding)
        assertThat(paddingValues.calculateRightPadding(layoutDir)).isEqualTo(horizontalPadding)
    }

    private fun ComposeContentTestRule.setContentWithScreenHeight(
        heightDp: Int,
        content: @Composable () -> Unit,
    ) {
        this.setContent {
            val newConfiguration = Configuration(LocalConfiguration.current)
            newConfiguration.screenHeightDp = heightDp
            CompositionLocalProvider(LocalConfiguration provides newConfiguration) { content() }
        }
    }

    companion object {
        private const val SCREEN_HEIGHT_DP = 100
    }
}
