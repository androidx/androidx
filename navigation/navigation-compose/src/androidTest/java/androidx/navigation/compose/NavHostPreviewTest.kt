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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.tooling.PreviewActivity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class NavHostPreviewTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<PreviewActivity>()

    @Test
    fun navHostPreviewTest() {
        // prevent auto synchronization so it doesn't actually animate the composable
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            NavHostPreview()
        }

        val text = composeTestRule.onNodeWithTag("text").assertExists()
        text.assertIsDisplayed()
    }

    @Test
    fun navHostWithDialogPreviewTest() {
        // prevent auto synchronization so it doesn't actually animate the composable
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            NavHostWithDialogPreview()
        }

        val text = composeTestRule.onNodeWithTag("text").assertExists()
        text.assertIsDisplayed()

        // dialogs that are shown by default on startDestination should also be shown
        // on preview
        val dialog = composeTestRule.onNodeWithTag("dialog").assertExists()
        dialog.assertIsDisplayed()

        val dialogText = composeTestRule.onNodeWithTag("dialog_text").assertExists()
        dialogText.assertIsDisplayed()
    }
}

@Preview
@Composable
fun NavHostPreview() {
    CompositionLocalProvider(
        LocalInspectionMode provides true,
    ) {
        Box(Modifier.fillMaxSize().background(Color.Red)) {
            NavHost(
                navController = rememberNavController(),
                startDestination = "home"
            ) {
                composable("home") {
                    Box(Modifier.fillMaxSize().background(Color.Blue)) {
                        Text(text = "test", modifier = Modifier.testTag("text"))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun NavHostWithDialogPreview() {
    CompositionLocalProvider(
        LocalInspectionMode provides true,
    ) {
        val navController = rememberNavController()
        Box(Modifier.fillMaxSize().background(Color.Red)) {
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    Box(Modifier.fillMaxSize().background(Color.Blue)) {
                        Text(text = "test", modifier = Modifier.testTag("text"))
                    }
                    // show dialog
                    navController.navigate("dialog")
                }
                dialog("dialog") {
                    Box(
                        Modifier.height(200.dp).background(Color.Gray).testTag("dialog")
                    ) {
                        Text(text = "test", modifier = Modifier.testTag("dialog_text"))
                    }
                }
            }
        }
    }
}
