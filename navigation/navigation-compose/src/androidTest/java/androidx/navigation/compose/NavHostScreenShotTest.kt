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

package androidx.navigation.compose

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavHostScreenShotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule("navigation/navigation-compose")

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Test
    fun testNavHostAnimationsZIndex() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = FIRST,
                route = "start",
                enterTransition = { slideInHorizontally { it / 2 } },
                exitTransition = { slideOutHorizontally { - it / 2 } }
            ) {
                composable(FIRST) { BasicText(FIRST) }
                composable(SECOND) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Blue)) {
                        BasicText(SECOND, Modifier.size(50.dp))
                    }
                }
                composable(THIRD) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Red)) {
                        BasicText(THIRD, Modifier.size(50.dp))
                    }
                }
            }
        }

        composeTestRule.runOnIdle {
            navController.navigate(SECOND)
        }

        // don't start drawing third yet
        composeTestRule.runOnIdle {
            composeTestRule.mainClock.autoAdvance = false
            navController.navigate(THIRD) { popUpTo(FIRST) { inclusive = true } }
        }

        composeTestRule.waitForIdle()
        // the image should show third destination covering half the screen (covering half of
        // second destination) as its slideIn animation starts at half screen
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.onNodeWithText(THIRD).onParent()
            .captureToImage().assertAgainstGolden(
                screenshotRule,
                "testNavHostAnimationsZIndex"
            )
    }
}

private const val FIRST = "first"
private const val SECOND = "second"
private const val THIRD = "third"
