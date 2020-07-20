/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test.assertions

import androidx.test.filters.MediumTest
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.compose.foundation.Box
import androidx.compose.foundation.background
import androidx.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.ui.test.assertHeightIsAtLeast
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertIsEqualTo
import androidx.ui.test.assertIsNotEqualTo
import androidx.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.ui.test.assertPositionInRootIsEqualTo
import androidx.ui.test.assertTopPositionInRootIsEqualTo
import androidx.ui.test.assertWidthIsAtLeast
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.util.expectError
import androidx.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class BoundsAssertionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    val tag = "box"

    private fun composeBox() {
        composeTestRule.setContent {
            Box(modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.TopStart)
            ) {
                Box(modifier = Modifier.padding(start = 50.dp, top = 100.dp)) {
                    Box(modifier = Modifier
                        .testTag(tag)
                        .size(80.dp, 100.dp)
                        .background(color = Color.Black)
                    )
                }
            }
        }
    }

    @Test
    fun dp_assertEquals() {
        5.dp.assertIsEqualTo(5.dp)
        5.dp.assertIsEqualTo(4.6.dp)
        5.dp.assertIsEqualTo(5.4.dp)
    }

    @Test
    fun dp_assertNotEquals() {
        5.dp.assertIsNotEqualTo(6.dp)
    }

    @Test
    fun dp_assertEquals_fail() {
        expectError<AssertionError> {
            5.dp.assertIsEqualTo(6.dp)
        }
    }

    @Test
    fun dp_assertNotEquals_fail() {
        expectError<AssertionError> {
            5.dp.assertIsNotEqualTo(5.dp)
            5.dp.assertIsNotEqualTo(5.4.dp)
        }
    }

    @Test
    fun assertEquals() {
        composeBox()

        onNodeWithTag(tag)
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun assertAtLeast() {
        composeBox()

        onNodeWithTag(tag)
            .assertWidthIsAtLeast(80.dp)
            .assertWidthIsAtLeast(79.dp)
            .assertHeightIsAtLeast(100.dp)
            .assertHeightIsAtLeast(99.dp)
    }

    @Test
    fun assertEquals_fail() {
        composeBox()

        expectError<AssertionError> {
            onNodeWithTag(tag)
                .assertWidthIsEqualTo(70.dp)
        }

        expectError<AssertionError> {
            onNodeWithTag(tag)
                .assertHeightIsEqualTo(90.dp)
        }
    }

    @Test
    fun assertAtLeast_fail() {
        composeBox()

        expectError<AssertionError> {
            onNodeWithTag(tag)
                .assertWidthIsAtLeast(81.dp)
        }

        expectError<AssertionError> {
            onNodeWithTag(tag)
                .assertHeightIsAtLeast(101.dp)
        }
    }

    @Test
    fun assertPosition() {
        composeBox()

        onNodeWithTag(tag)
            .assertPositionInRootIsEqualTo(expectedLeft = 50.dp, expectedTop = 100.dp)
            .assertLeftPositionInRootIsEqualTo(50.dp)
            .assertTopPositionInRootIsEqualTo(100.dp)
    }

    @Test
    fun assertPosition_fail() {
        composeBox()

        expectError<AssertionError> {
            onNodeWithTag(tag)
                .assertPositionInRootIsEqualTo(expectedLeft = 51.dp, expectedTop = 101.dp)
        }

        expectError<AssertionError> {
            onNodeWithTag(tag)
                .assertPositionInRootIsEqualTo(expectedLeft = 49.dp, expectedTop = 99.dp)
        }
    }
}