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

package androidx.compose.remote.player.compose.creation.compose.layout

import androidx.compose.remote.creation.compose.layout.Alignment
import androidx.compose.remote.creation.compose.layout.Arrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumnScope
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.player.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.player.compose.test.utils.screenshot.TargetPlayer
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteCollapsibleColumnComposeTest {
    @get:Rule
    val composeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.Compose,
        )

    @Test
    fun simpleLayout() {
        composeTestRule.simpleLayout()
    }

    @Test
    fun collapse() {
        composeTestRule.collapse()
    }
}

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteCollapsibleColumnViewTest {
    @get:Rule
    val composeTestRule =
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            targetPlayer = TargetPlayer.View,
        )

    @Test
    fun simpleLayout() {
        composeTestRule.simpleLayout()
    }

    @Test
    fun collapse() {
        composeTestRule.collapse()
    }
}

private fun RemoteComposeScreenshotTestRule.simpleLayout() = runScreenshotTest {
    RemoteRow {
        RemoteColumn {
            Container {
                // TODO(b/447100988): replace size by fillMaxSize in all those
                // RemoteCollapsibleColumn
                RemoteCollapsibleColumn(modifier = RemoteModifier.size(ContainerSize)) { Content() }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteCollapsibleColumn(
                    modifier = RemoteModifier.size(ContainerSize),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteCollapsibleColumn(
                    modifier = RemoteModifier.size(ContainerSize),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Content()
                }
            }
        }
        RemoteBox(modifier = RemoteModifier.width(Padding))
        RemoteColumn {
            Container {
                RemoteCollapsibleColumn(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteCollapsibleColumn(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteCollapsibleColumn(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Content()
                }
            }
        }
        RemoteBox(modifier = RemoteModifier.width(Padding))
        RemoteColumn {
            Container {
                RemoteCollapsibleColumn(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalAlignment = Alignment.End,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteCollapsibleColumn(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Content()
                }
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container {
                RemoteCollapsibleColumn(
                    modifier = RemoteModifier.size(ContainerSize),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Content()
                }
            }
        }
    }
}

private fun RemoteComposeScreenshotTestRule.collapse() = runScreenshotTest {
    RemoteRow {
        RemoteColumn {
            Container { TestFourSquares_displaysThree() }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container { TestSixSquaresWithPriorities_displaysThreeWithHighestPriorities() }
        }
        RemoteBox(modifier = RemoteModifier.width(Padding))
        RemoteColumn {
            Container {
                TestSingleContentInContainerWithSizeAndBackground_displaysContentAndBackground()
            }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container { TestEmptyContainerWithSizeAndBackground_displaysNothing() }
            RemoteBox(modifier = RemoteModifier.height(Padding))
            Container { TestContentBiggerThanContainerWithSizeAndBackground_displaysNothing() }
        }
    }
}

@RemoteComposable
@Composable
private fun TestFourSquares_displaysThree() {
    RemoteCollapsibleColumn(modifier = RemoteModifier.size(ContainerSize)) {
        CustomBox('A')
        CustomBox('B')
        CustomBox('C')
        CustomBox('D')
    }
}

@RemoteComposable
@Composable
private fun TestSixSquaresWithPriorities_displaysThreeWithHighestPriorities() {
    RemoteCollapsibleColumn(modifier = RemoteModifier.size(ContainerSize)) {
        CustomBox('A', priority = 1f)
        CustomBox('B', priority = 4f)
        CustomBox('C', priority = 2f)
        CustomBox('D', priority = 5f)
        CustomBox('E', priority = 3f)
        CustomBox('F', priority = 6f)
    }
}

@RemoteComposable
@Composable
private fun TestSingleContentInContainerWithSizeAndBackground_displaysContentAndBackground() {
    RemoteCollapsibleColumn(modifier = RemoteModifier.size(ContainerSize).background(Color.Red)) {
        CustomBox('A')
    }
}

@RemoteComposable
@Composable
private fun TestEmptyContainerWithSizeAndBackground_displaysNothing() {
    RemoteCollapsibleColumn(modifier = RemoteModifier.size(ContainerSize).background(Color.Red)) {}
}

@RemoteComposable
@Composable
private fun TestContentBiggerThanContainerWithSizeAndBackground_displaysNothing() {
    RemoteCollapsibleColumn(modifier = RemoteModifier.size(ContainerSize).background(Color.Red)) {
        CustomBox('A', modifier = RemoteModifier.size((ContainerSize.value + 10).dp))
    }
}

@RemoteComposable
@Composable
private fun RemoteCollapsibleColumnScope.CustomBox(
    letter: Char,
    modifier: RemoteModifier = RemoteModifier,
    priority: Float? = null,
) {
    val appliedModifier =
        modifier
            .padding(5.dp)
            .size(20.dp)
            .background(Color.Blue)
            .then(
                if (priority != null) {
                    RemoteModifier.priority(priority)
                } else {
                    RemoteModifier
                }
            )

    RemoteBox(modifier = appliedModifier) { RemoteText(letter.toString()) }
}

@Composable
@RemoteComposable
private fun Container(modifier: RemoteModifier = RemoteModifier, content: @Composable () -> Unit) {
    RemoteBox(
        modifier = modifier.size(ContainerSize).background(Color(0xFFCFD8DC)),
        horizontalAlignment = Alignment.Start,
        content = content,
    )
}

@Composable
@RemoteComposable
private fun Content(modifier: RemoteModifier = RemoteModifier) {
    RemoteBox(modifier = modifier.size(48.dp).background(Color(0xFF6200EE)))
    RemoteBox(modifier = modifier.size(24.dp).background(Color(0xFF03DAC6)))
}

private val Padding = 24.dp
private val ContainerSize = 100.dp
