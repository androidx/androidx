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

@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package androidx.compose.material3.adaptive.navigation3

import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class ListDetailSceneStrategyScreenshotTest {
    @Suppress("ComposeTestRuleDispatcher") @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3_ADAPTIVE_NAV3)

    @Test
    fun dualPane_backstackWithListDetail_navigate_showsNewDetail() {
        val backStack = mutableStateListOf(HomeKey, ListKey, DetailKey("abc"))
        composeTestRule.setContent {
            NavScreen(backStack = backStack, directive = MockDualPaneScaffoldDirective)
        }

        backStack.add(DetailKey("def"))

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(NavDisplayTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "dualPane_backstackWithListDetail_navigate_showsNewDetail",
            )
    }

    @Test
    fun dualPane_backstackWithListDetail_onBack_popLatest_removesDetail() {
        val backStack = mutableStateListOf(HomeKey, ListKey, DetailKey("abc"))
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        composeTestRule.setContent {
            NavScreen(
                backStack = backStack,
                backNavigationBehavior = BackNavigationBehavior.PopLatest,
                directive = MockDualPaneScaffoldDirective,
            )
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }

        composeTestRule.runOnIdle { backPressedDispatcher.onBackPressed() }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(NavDisplayTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "dualPane_backstackWithListDetail_onBack_popLatest_removesDetail",
            )
    }

    @Test
    fun dualPane_backstackWithListDetailExtra_onBack_removesExtra() {
        val backStack = mutableStateListOf(HomeKey, ListKey, DetailKey("abc"), ExtraKey("abc"))
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        composeTestRule.setContent {
            NavScreen(backStack = backStack, directive = MockDualPaneScaffoldDirective)
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }

        composeTestRule.runOnIdle { backPressedDispatcher.onBackPressed() }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(NavDisplayTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "dualPane_backstackWithListDetailExtra_onBack_removesExtra",
            )
    }

    @Test
    fun dualPane_backstackWithListDetailExtra_gestureBack_removesExtra() {
        val backStack = mutableStateListOf(HomeKey, ListKey, DetailKey("abc"), ExtraKey("abc"))
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        composeTestRule.setContent {
            NavScreen(backStack = backStack, directive = MockDualPaneScaffoldDirective)
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
        }

        composeTestRule.runOnIdle {
            backPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
            )
            backPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1F, 0.1F, 0.5F, BackEvent.EDGE_LEFT)
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            backPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1F, 0.1F, 0.5F, BackEvent.EDGE_LEFT)
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(NavDisplayTestTag)
            .captureToImage()
            .assertAgainstGolden(
                screenshotRule,
                "dualPane_backstackWithListDetailExtra_gestureBack_removesExtra",
            )

        composeTestRule.waitForIdle()
    }
}

private const val GOLDEN_MATERIAL3_ADAPTIVE_NAV3 = "compose/material3/adaptive/adaptive-navigation3"
