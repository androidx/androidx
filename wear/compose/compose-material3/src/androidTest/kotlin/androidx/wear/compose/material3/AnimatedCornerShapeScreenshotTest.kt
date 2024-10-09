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

package androidx.wear.compose.material3

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.AbsoluteCutCornerShape
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
class AnimatedCornerShapeScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun cutCornerShapeLtr() = verifyScreenshot {
        FilledIconButton(
            onClick = {},
            shapes =
                IconButtonDefaults.shapes(
                    CutCornerShape(
                        topStart = 2.dp,
                        topEnd = 4.dp,
                        bottomEnd = 6.dp,
                        bottomStart = 8.dp
                    )
                )
        ) {}
    }

    @Test
    fun cutCornerShapeRtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            FilledIconButton(
                onClick = {},
                shapes =
                    IconButtonDefaults.shapes(
                        CutCornerShape(
                            topStart = 2.dp,
                            topEnd = 4.dp,
                            bottomEnd = 6.dp,
                            bottomStart = 8.dp
                        )
                    )
            ) {}
        }

    @Test
    fun absoluteCutCornerShapeLtr() = verifyScreenshot {
        FilledIconButton(
            onClick = {},
            shapes =
                IconButtonDefaults.shapes(
                    AbsoluteCutCornerShape(
                        topLeft = 2.dp,
                        topRight = 4.dp,
                        bottomRight = 6.dp,
                        bottomLeft = 8.dp
                    )
                )
        ) {}
    }

    @Test
    fun absoluteCutCornerShapeRtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            FilledIconButton(
                onClick = {},
                shapes =
                    IconButtonDefaults.shapes(
                        AbsoluteCutCornerShape(
                            topLeft = 2.dp,
                            topRight = 4.dp,
                            bottomRight = 6.dp,
                            bottomLeft = 8.dp
                        )
                    )
            ) {}
        }

    @Test
    fun absoluteRoundedCornerShapeLtr() = verifyScreenshot {
        FilledIconButton(
            onClick = {},
            shapes =
                IconButtonDefaults.shapes(
                    AbsoluteRoundedCornerShape(
                        topLeft = 2.dp,
                        topRight = 4.dp,
                        bottomRight = 6.dp,
                        bottomLeft = 8.dp
                    )
                )
        ) {}
    }

    @Test
    fun absoluteRoundedCornerShapeRtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            FilledIconButton(
                onClick = {},
                shapes =
                    IconButtonDefaults.shapes(
                        AbsoluteRoundedCornerShape(
                            topLeft = 2.dp,
                            topRight = 4.dp,
                            bottomRight = 6.dp,
                            bottomLeft = 8.dp
                        )
                    )
            ) {}
        }

    private fun verifyScreenshot(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit
    ) {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(modifier = Modifier.background(Color.Black).testTag(TEST_TAG)) { content() }
            }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}
