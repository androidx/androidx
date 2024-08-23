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

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O, maxSdkVersion = 32)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ToggleButtonScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrapperTestTag = "WrapperTestTag"

    @Test
    fun toggleButton_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = false, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("toggleButton_lightTheme")
    }

    @Test
    fun toggleButton_lightTheme_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = false, onCheckedChange = {}, enabled = false) {
                    Text("Button")
                }
            }
        }
        assertAgainstGolden("toggleButton_lightTheme_disabled")
    }

    @Test
    fun toggleButton_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = false, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("toggleButton_darkTheme")
    }

    @Test
    fun toggleButton_checked_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = true, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("toggleButton_checked_lightTheme")
    }

    @Test
    fun toggleButton_checked_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = true, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("toggleButton_checked_darkTheme")
    }

    @Ignore
    @Test
    fun toggleButton_lightTheme_defaultToPressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = false, onCheckedChange = {}) { Text("Button") }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isToggleable()).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("toggleButton_lightTheme_defaultToPressed")
    }

    @Ignore
    @Test
    fun toggleButton_lightTheme_checkedToPressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = true, onCheckedChange = {}) { Text("Button") }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isToggleable()).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("toggleButton_lightTheme_checkedToPressed")
    }

    @Test
    fun elevatedToggleButton_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ElevatedToggleButton(checked = false, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("elevatedToggleButton_lightTheme")
    }

    @Test
    fun elevatedToggleButton_lightTheme_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ElevatedToggleButton(checked = false, onCheckedChange = {}, enabled = false) {
                    Text("Button")
                }
            }
        }
        assertAgainstGolden("elevatedToggleButton_lightTheme_disabled")
    }

    @Test
    fun elevatedToggleButton_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ElevatedToggleButton(checked = false, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("elevatedToggleButton_darkTheme")
    }

    @Test
    fun elevatedToggleButton_checked_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ElevatedToggleButton(checked = true, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("elevatedToggleButton_checked_lightTheme")
    }

    @Test
    fun elevatedToggleButton_checked_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ElevatedToggleButton(checked = true, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("elevatedToggleButton_checked_darkTheme")
    }

    @Ignore
    @Test
    fun elevatedToggleButton_lightTheme_defaultToPressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ElevatedToggleButton(checked = false, onCheckedChange = {}) { Text("Button") }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isToggleable()).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("elevatedToggleButton_lightTheme_defaultToPressed")
    }

    @Ignore
    @Test
    fun elevatedToggleButton_lightTheme_checkedToPressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ElevatedToggleButton(checked = true, onCheckedChange = {}) { Text("Button") }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isToggleable()).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("elevatedToggleButton_lightTheme_checkedToPressed")
    }

    @Test
    fun tonalToggleButton_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                TonalToggleButton(checked = false, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("tonalToggleButton_lightTheme")
    }

    @Test
    fun tonalToggleButton_lightTheme_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                TonalToggleButton(checked = false, onCheckedChange = {}, enabled = false) {
                    Text("Button")
                }
            }
        }
        assertAgainstGolden("tonalToggleButton_lightTheme_disabled")
    }

    @Test
    fun tonalToggleButton_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                TonalToggleButton(checked = false, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("tonalToggleButton_darkTheme")
    }

    @Test
    fun tonalToggleButton_checked_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                TonalToggleButton(checked = true, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("tonalToggleButton_checked_lightTheme")
    }

    @Test
    fun tonalToggleButton_checked_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                TonalToggleButton(checked = true, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("tonalToggleButton_checked_darkTheme")
    }

    @Ignore
    @Test
    fun tonalToggleButton_lightTheme_defaultToPressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                TonalToggleButton(checked = false, onCheckedChange = {}) { Text("Button") }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isToggleable()).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("tonalToggleButton_lightTheme_defaultToPressed")
    }

    @Ignore
    @Test
    fun tonalToggleButton_lightTheme_checkedToPressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                TonalToggleButton(checked = true, onCheckedChange = {}) { Text("Button") }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isToggleable()).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("tonalToggleButton_lightTheme_checkedToPressed")
    }

    @Test
    fun outlinedToggleButton_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                OutlinedToggleButton(checked = false, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("outlinedToggleButton_lightTheme")
    }

    @Test
    fun outlinedToggleButton_lightTheme_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                OutlinedToggleButton(checked = false, onCheckedChange = {}, enabled = false) {
                    Text("Button")
                }
            }
        }
        assertAgainstGolden("outlinedToggleButton_lightTheme_disabled")
    }

    @Test
    fun outlinedToggleButton_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                OutlinedToggleButton(checked = false, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("outlinedToggleButton_darkTheme")
    }

    @Test
    fun outlinedToggleButton_checked_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                OutlinedToggleButton(checked = true, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("outlinedToggleButton_checked_lightTheme")
    }

    @Test
    fun outlinedToggleButton_checked_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                OutlinedToggleButton(checked = true, onCheckedChange = {}) { Text("Button") }
            }
        }
        assertAgainstGolden("outlinedToggleButton_checked_darkTheme")
    }

    @Ignore
    @Test
    fun outlinedToggleButton_lightTheme_defaultToPressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                OutlinedToggleButton(checked = false, onCheckedChange = {}) { Text("Button") }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isToggleable()).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("outlinedToggleButton_lightTheme_defaultToPressed")
    }

    @Ignore
    @Test
    fun outlinedToggleButton_lightTheme_checkedToPressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                OutlinedToggleButton(checked = true, onCheckedChange = {}) { Text("Button") }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isToggleable()).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("outlinedToggleButton_lightTheme_checkedToPressed")
    }

    @Test
    fun toggleButton_withIcon_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = false, onCheckedChange = {}) {
                    Icon(
                        Icons.Outlined.Favorite,
                        contentDescription = "Localized description",
                        modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text("Like")
                }
            }
        }
        assertAgainstGolden("toggleButton_withIcon_lightTheme")
    }

    @Test
    fun toggleButton_withIcon_lightTheme_disabled() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = false, onCheckedChange = {}, enabled = false) {
                    Icon(
                        Icons.Outlined.Favorite,
                        contentDescription = "Localized description",
                        modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text("Like")
                }
            }
        }
        assertAgainstGolden("toggleButton_withIcon_lightTheme_disabled")
    }

    @Test
    fun toggleButton_withIcon_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = false, onCheckedChange = {}) {
                    Icon(
                        Icons.Outlined.Favorite,
                        contentDescription = "Localized description",
                        modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text("Like")
                }
            }
        }
        assertAgainstGolden("toggleButton_withIcon_darkTheme")
    }

    @Test
    fun toggleButton_withIcon_checked_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = true, onCheckedChange = {}) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Localized description",
                        modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text("Like")
                }
            }
        }
        assertAgainstGolden("toggleButton_withIcon_checked_lightTheme")
    }

    @Test
    fun toggleButton_withIcon_checked_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = true, onCheckedChange = {}) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Localized description",
                        modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text("Like")
                }
            }
        }
        assertAgainstGolden("toggleButton_withIcon_checked_darkTheme")
    }

    @Ignore
    @Test
    fun toggleButton_withIcon_lightTheme_defaultToPressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = false, onCheckedChange = {}) {
                    Icon(
                        Icons.Outlined.Favorite,
                        contentDescription = "Localized description",
                        modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text("Like")
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isToggleable()).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("toggleButton_withIcon_lightTheme_defaultToPressed")
    }

    @Ignore
    @Test
    fun toggleButton_withIcon_lightTheme_checkedToPressed() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ToggleButton(checked = true, onCheckedChange = {}) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Localized description",
                        modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text("Like")
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(isToggleable()).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("toggleButton_withIcon_lightTheme_checkedToPressed")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule
            .onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
