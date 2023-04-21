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

package androidx.wear.compose.material3

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert

val BigTestMaxWidth = 5000.dp
val BigTestMaxHeight = 5000.dp

internal const val TEST_TAG = "test-item"

@Composable
fun TestImage(iconLabel: String = "TestIcon") {
    val testImage = Icons.Outlined.Add
    Image(
        testImage, iconLabel,
        modifier = Modifier
            .fillMaxSize()
            .testTag(iconLabel),
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
    )
}

@Composable
fun TestIcon(modifier: Modifier = Modifier, iconLabel: String = "TestIcon") {
    val testImage = Icons.Outlined.Add
    Icon(
        imageVector = testImage,
        contentDescription = iconLabel,
        modifier = modifier.testTag(iconLabel)
    )
}

@Composable
fun CenteredText(
    text: String
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text)
    }
}

fun ComposeContentTestRule.setContentWithThemeForSizeAssertions(
    parentMaxWidth: Dp = BigTestMaxWidth,
    parentMaxHeight: Dp = BigTestMaxHeight,
    useUnmergedTree: Boolean = false,
    content: @Composable () -> Unit
): SemanticsNodeInteraction {
    setContent {
        MaterialTheme {
            Box {
                Box(
                    Modifier
                        .sizeIn(
                            maxWidth = parentMaxWidth,
                            maxHeight = parentMaxHeight
                        )
                        .testTag("containerForSizeAssertion")
                ) {
                    content()
                }
            }
        }
    }
    return onNodeWithTag("containerForSizeAssertion", useUnmergedTree)
}

fun ComposeContentTestRule.setContentWithTheme(
    modifier: Modifier = Modifier,
    composable: @Composable BoxScope.() -> Unit
) {
    setContent {
        MaterialTheme {
            Box(modifier = modifier, content = composable)
        }
    }
}

internal fun ComposeContentTestRule.verifyTapSize(
    expectedSize: Dp,
    content: @Composable (modifier: Modifier) -> Unit
) {
    setContentWithTheme {
        content(Modifier.testTag(TEST_TAG))
    }
    waitForIdle()

    onNodeWithTag(TEST_TAG)
        .assertTouchHeightIsEqualTo(expectedSize)
        .assertTouchWidthIsEqualTo(expectedSize)
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun ComposeContentTestRule.verifyColors(
    status: Status,
    expectedContainerColor: @Composable () -> Color,
    expectedContentColor: @Composable () -> Color,
    applyAlphaForDisabled: Boolean = true,
    content: @Composable () -> Color
) {
    val testBackgroundColor = Color.White
    var finalExpectedContainerColor = Color.Transparent
    var finalExpectedContent = Color.Transparent
    var actualContentColor = Color.Transparent
    setContentWithTheme {
        finalExpectedContainerColor =
            if (status.enabled() || !applyAlphaForDisabled) {
                expectedContainerColor()
            } else {
                expectedContainerColor().copy(ContentAlpha.disabled)
            }.compositeOver(testBackgroundColor)
        finalExpectedContent =
            if (status.enabled() || !applyAlphaForDisabled) {
                expectedContentColor()
            } else {
                expectedContentColor().copy(ContentAlpha.disabled)
            }
        Box(
            Modifier
                .fillMaxSize()
                .background(testBackgroundColor)
        ) {
            actualContentColor = content()
        }
    }
    Assert.assertEquals(finalExpectedContent, actualContentColor)
    onNodeWithTag(TEST_TAG)
        .captureToImage()
        .assertContainsColor(finalExpectedContainerColor)
}

internal enum class Status {
    Enabled,
    Disabled;
    fun enabled() = this == Enabled
}
