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

package androidx.wear.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.wear.compose.material3.test.R
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test

class NonClickableCardTest {
    @get:Rule val rule: ComposeContentTestRule = createComposeRule(StandardTestDispatcher())

    @Test
    fun non_clickable_card_has_no_click_action() {
        rule.setContentWithTheme { Card(modifier = Modifier.testTag(TEST_TAG)) { TestImage() } }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @Test
    fun non_clickable_card_with_image_container_has_no_click_action() {
        rule.setContentWithTheme {
            Card(containerPainter = testContainerPainter(), modifier = Modifier.testTag(TEST_TAG)) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @Test
    fun non_clickable_app_card_has_no_click_action() {
        rule.setContentWithTheme {
            AppCard(appName = {}, title = {}, modifier = Modifier.testTag(TEST_TAG)) { TestImage() }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @Test
    fun non_clickable_title_card_has_no_click_action() {
        rule.setContentWithTheme {
            TitleCard(title = {}, modifier = Modifier.testTag(TEST_TAG)) { TestImage() }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @Test
    fun non_clickable_title_card_with_image_container_has_no_click_action() {
        rule.setContentWithTheme {
            TitleCard(
                containerPainter = testContainerPainter(),
                title = {},
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                TestImage()
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @Test
    fun non_clickable_outlined_card_no_click_action() {
        rule.setContentWithTheme {
            OutlinedCard(modifier = Modifier.testTag(TEST_TAG)) { TestImage() }
        }

        rule.onNodeWithTag(TEST_TAG).assertHasNoClickAction()
    }

    @Composable
    fun testContainerPainter(): Painter = painterResource(id = R.drawable.backgroundimage1)
}
