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

package androidx.navigation3

import android.os.Build
import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.RequiresApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SinglePaneNavDisplayScreenshotTest {
    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule("navigation3/navigation3")

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testNavDisplayPredictiveBackAnimations() {
        lateinit var backStack: MutableList<Any>
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            SinglePaneNavDisplay(
                backStack = backStack,
                enterTransition = slideInHorizontally { it / 2 },
                exitTransition = slideOutHorizontally { -it / 2 },
                popEnterTransition = slideInHorizontally { -it / 2 },
                popExitTransition = slideOutHorizontally { it / 2 }
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second ->
                        NavEntry(second) {
                            Box(Modifier.fillMaxSize().background(Color.Blue)) {
                                Text(second, Modifier.size(50.dp))
                            }
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle { backStack.add(second) }

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
            .onNodeWithText(second)
            .onParent()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "testNavDisplayPredictiveBackAnimations")
    }
}

private const val first = "first"
private const val second = "second"
