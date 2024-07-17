/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class OpticalCenteringTest {
    private val TextTag = "text"
    private val ContainerTag = "container"
    @get:Rule val rule = createComposeRule()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun opticalCentering_contentPadding_asymmetricShape() {
        val shape =
            RoundedCornerShape(
                topStart = 20.dp,
                bottomStart = 20.dp,
                topEnd = 0.dp,
                bottomEnd = 0.dp
            )
        val baseContentPadding = PaddingValues(horizontal = 20.dp)
        val expectedStartPadding = 20.dp + (0.11f * 20f).dp
        val expectedEndPadding = 20.dp - (0.11f * 20f).dp
        rule.setContent {
            Box(modifier = Modifier.clip(shape).testTag(ContainerTag)) {
                Row(modifier = Modifier.opticalCentering(shape, baseContentPadding)) {
                    Text(text = "Test", modifier = Modifier.testTag(TextTag))
                }
            }
        }

        val containerBounds = rule.onNodeWithTag(ContainerTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(TextTag).getUnclippedBoundsInRoot()

        (textBounds.left - containerBounds.left).assertIsEqualTo(expectedStartPadding)
        (containerBounds.right - textBounds.right).assertIsEqualTo(expectedEndPadding)
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun opticalCentering_contentPadding_symmetricShape() {
        val shape = RoundedCornerShape(0.dp)
        val baseContentPadding = PaddingValues(horizontal = 20.dp)
        val expectedPadding = 20.dp
        rule.setContent {
            Box(modifier = Modifier.clip(shape).testTag(ContainerTag)) {
                Row(modifier = Modifier.opticalCentering(shape, baseContentPadding)) {
                    Text(text = "Test", modifier = Modifier.testTag(TextTag))
                }
            }
        }

        val containerBounds = rule.onNodeWithTag(ContainerTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(TextTag).getUnclippedBoundsInRoot()

        (textBounds.left - containerBounds.left).assertIsEqualTo(expectedPadding)
        (containerBounds.right - textBounds.right).assertIsEqualTo(expectedPadding)
    }
}
