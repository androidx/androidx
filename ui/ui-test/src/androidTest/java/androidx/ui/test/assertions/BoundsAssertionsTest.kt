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
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.padding
import androidx.ui.layout.size
import androidx.ui.layout.wrapContentSize
import androidx.ui.test.assertHeightIsAtLeast
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertPositionInRootIsEqualTo
import androidx.ui.test.assertWidthIsAtLeast
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
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
                        .drawBackground(Color.Black)
                    )
                }
            }
        }
    }

    @Test
    fun assertEquals() {
        composeBox()

        findByTag(tag)
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun assertAtLeast() {
        composeBox()

        findByTag(tag)
            .assertWidthIsAtLeast(80.dp)
            .assertWidthIsAtLeast(79.dp)
            .assertHeightIsAtLeast(100.dp)
            .assertHeightIsAtLeast(99.dp)
    }

    @Test
    fun assertEquals_fail() {
        composeBox()

        expectError<AssertionError> {
            findByTag(tag)
                .assertWidthIsEqualTo(70.dp)
        }

        expectError<AssertionError> {
            findByTag(tag)
                .assertHeightIsEqualTo(90.dp)
        }
    }

    @Test
    fun assertAtLeast_fail() {
        composeBox()

        expectError<AssertionError> {
            findByTag(tag)
                .assertWidthIsAtLeast(81.dp)
        }

        expectError<AssertionError> {
            findByTag(tag)
                .assertHeightIsAtLeast(101.dp)
        }
    }

    @Test
    fun assertPosition() {
        composeBox()

        findByTag(tag)
            .assertPositionInRootIsEqualTo(expectedLeft = 50.dp, expectedTop = 100.dp)
    }

    @Test
    fun assertPosition_fail() {
        composeBox()

        expectError<AssertionError> {
            findByTag(tag)
                .assertPositionInRootIsEqualTo(expectedLeft = 51.dp, expectedTop = 101.dp)
        }

        expectError<AssertionError> {
            findByTag(tag)
                .assertPositionInRootIsEqualTo(expectedLeft = 49.dp, expectedTop = 99.dp)
        }
    }
}