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

package androidx.compose.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class InteractiveListScreenshotTest(private val scheme: ColorSchemeWrapper) {
    private val ListTestTag = "List"

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun clickableListItem_oneLine() {
        rule.setMaterialContent(scheme.colorScheme) {
            ListItem(
                modifier = Modifier.testTag(ListTestTag),
                onClick = {},
                content = { Text("Content") },
                leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            )
        }

        assertAgainstGolden("clickableListItem_oneLine_${scheme.name}")
    }

    @Test
    fun clickableListItem_twoLines() {
        rule.setMaterialContent(scheme.colorScheme) {
            ListItem(
                modifier = Modifier.testTag(ListTestTag),
                onClick = {},
                content = { Text("Content") },
                leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                supportingContent = { Text("Supporting") },
            )
        }

        assertAgainstGolden("clickableListItem_twoLines_${scheme.name}")
    }

    @Test
    fun clickableListItem_threeLines() {
        rule.setMaterialContent(scheme.colorScheme) {
            ListItem(
                modifier = Modifier.testTag(ListTestTag),
                onClick = {},
                content = { Text("Content") },
                leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                supportingContent = { Text("Supporting 1\nSupporting 2") },
            )
        }

        assertAgainstGolden("clickableListItem_threeLines_${scheme.name}")
    }

    @Test
    fun clickableListItem_withLeadingTrailing() {
        rule.setMaterialContent(scheme.colorScheme) {
            Column(modifier = Modifier.testTag(ListTestTag)) {
                ListItem(
                    onClick = {},
                    leadingContent = { Checkbox(true, null) },
                    trailingContent = { RadioButton(true, null) },
                    content = { Text("Content") },
                )
                HorizontalDivider()
                ListItem(
                    onClick = {},
                    leadingContent = { Checkbox(true, {}) },
                    trailingContent = { RadioButton(true, {}) },
                    content = { Text("Content") },
                )
            }
        }

        assertAgainstGolden("clickableListItem_withLeadingTrailing_${scheme.name}")
    }

    @Test
    fun clickableListItem_customColors() {
        rule.setMaterialContent(scheme.colorScheme) {
            ListItem(
                modifier = Modifier.testTag(ListTestTag),
                onClick = {},
                leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                content = { Text("Content") },
                supportingContent = { Text("Supporting") },
                colors =
                    InteractiveListItemDefaults.colors(
                        containerColor = Color.Yellow,
                        contentColor = Color.Blue,
                        leadingContentColor = Color.Green,
                        supportingContentColor = Color.Gray,
                    ),
            )
        }

        assertAgainstGolden("clickableListItem_customColors_${scheme.name}")
    }

    @Test
    fun clickableListItem_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            ListItem(
                modifier = Modifier.testTag(ListTestTag),
                onClick = {},
                enabled = false,
                leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                content = { Text("Content") },
                supportingContent = { Text("Supporting") },
            )
        }

        assertAgainstGolden("clickableListItem_disabled_${scheme.name}")
    }

    @Suppress("BanThreadSleep")
    @Test
    fun clickableListItem_pressed() {
        rule.setMaterialContent(scheme.colorScheme) {
            ListItem(
                modifier = Modifier.testTag(ListTestTag),
                onClick = {},
                leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                content = { Text("Content") },
                supportingContent = { Text("Supporting") },
            )
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(ListTestTag).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("clickableListItem_pressed_${scheme.name}")
    }

    @Test
    fun selectableListItem_selected() {
        rule.setMaterialContent(scheme.colorScheme) {
            ListItem(
                modifier = Modifier.testTag(ListTestTag),
                selected = true,
                onClick = {},
                leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                content = { Text("Content") },
                supportingContent = { Text("Supporting") },
            )
        }

        assertAgainstGolden("selectableListItem_selected_${scheme.name}")
    }

    @Test
    fun toggleableListItem_checked() {
        rule.setMaterialContent(scheme.colorScheme) {
            ListItem(
                modifier = Modifier.testTag(ListTestTag),
                checked = true,
                onCheckedChange = {},
                leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                content = { Text("Content") },
                supportingContent = { Text("Supporting") },
            )
        }

        assertAgainstGolden("toggleableListItem_checked_${scheme.name}")
    }

    @Test
    fun segmentedListItem_oneLine() {
        val count = 3
        rule.setMaterialContent(scheme.colorScheme) {
            Column(
                modifier =
                    Modifier.testTag(ListTestTag)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalArrangement = Arrangement.spacedBy(InteractiveListItemDefaults.SegmentedGap),
            ) {
                repeat(count) { idx ->
                    SegmentedListItem(
                        onClick = {},
                        shapes = InteractiveListItemDefaults.segmentedShapes(idx, count),
                        content = { Text("Content") },
                        leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                    )
                }
            }
        }

        assertAgainstGolden("segmentedListItem_oneLine_${scheme.name}")
    }

    @Test
    fun segmentedListItem_twoLines() {
        val count = 3
        rule.setMaterialContent(scheme.colorScheme) {
            Column(
                modifier =
                    Modifier.testTag(ListTestTag)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalArrangement = Arrangement.spacedBy(InteractiveListItemDefaults.SegmentedGap),
            ) {
                repeat(count) { idx ->
                    SegmentedListItem(
                        onClick = {},
                        shapes = InteractiveListItemDefaults.segmentedShapes(idx, count),
                        content = { Text("Content") },
                        leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        supportingContent = { Text("Supporting") },
                    )
                }
            }
        }

        assertAgainstGolden("segmentedListItem_twoLines_${scheme.name}")
    }

    @Test
    fun segmentedListItem_threeLines() {
        val count = 3
        rule.setMaterialContent(scheme.colorScheme) {
            Column(
                modifier =
                    Modifier.testTag(ListTestTag)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalArrangement = Arrangement.spacedBy(InteractiveListItemDefaults.SegmentedGap),
            ) {
                repeat(count) { idx ->
                    SegmentedListItem(
                        onClick = {},
                        shapes = InteractiveListItemDefaults.segmentedShapes(idx, count),
                        content = { Text("Content") },
                        leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        supportingContent = { Text("Supporting 1\nSupporting 2") },
                    )
                }
            }
        }

        assertAgainstGolden("segmentedListItem_threeLines_${scheme.name}")
    }

    @Test
    fun segmentedListItem_firstSelected() {
        val count = 3
        rule.setMaterialContent(scheme.colorScheme) {
            Column(
                modifier =
                    Modifier.testTag(ListTestTag)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalArrangement = Arrangement.spacedBy(InteractiveListItemDefaults.SegmentedGap),
            ) {
                repeat(count) { idx ->
                    SegmentedListItem(
                        selected = idx == 0,
                        onClick = {},
                        shapes = InteractiveListItemDefaults.segmentedShapes(idx, count),
                        content = { Text("Content") },
                        leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        supportingContent = { Text("Supporting") },
                    )
                }
            }
        }

        assertAgainstGolden("segmentedListItem_firstSelected_${scheme.name}")
    }

    @Test
    fun segmentedListItem_secondSelected() {
        val count = 3
        rule.setMaterialContent(scheme.colorScheme) {
            Column(
                modifier =
                    Modifier.testTag(ListTestTag)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalArrangement = Arrangement.spacedBy(InteractiveListItemDefaults.SegmentedGap),
            ) {
                repeat(count) { idx ->
                    SegmentedListItem(
                        selected = idx == 1,
                        onClick = {},
                        shapes = InteractiveListItemDefaults.segmentedShapes(idx, count),
                        content = { Text("Content") },
                        leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        supportingContent = { Text("Supporting") },
                    )
                }
            }
        }

        assertAgainstGolden("segmentedListItem_secondSelected_${scheme.name}")
    }

    @Test
    fun segmentedListItem_lastSelected() {
        val count = 3
        rule.setMaterialContent(scheme.colorScheme) {
            Column(
                modifier =
                    Modifier.testTag(ListTestTag)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalArrangement = Arrangement.spacedBy(InteractiveListItemDefaults.SegmentedGap),
            ) {
                repeat(count) { idx ->
                    SegmentedListItem(
                        selected = idx == 2,
                        onClick = {},
                        shapes = InteractiveListItemDefaults.segmentedShapes(idx, count),
                        content = { Text("Content") },
                        leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        supportingContent = { Text("Supporting") },
                    )
                }
            }
        }

        assertAgainstGolden("segmentedListItem_lastSelected_${scheme.name}")
    }

    @Test
    fun segmentedListItem_allChecked() {
        val count = 3
        rule.setMaterialContent(scheme.colorScheme) {
            Column(
                modifier =
                    Modifier.testTag(ListTestTag)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalArrangement = Arrangement.spacedBy(InteractiveListItemDefaults.SegmentedGap),
            ) {
                repeat(count) { idx ->
                    SegmentedListItem(
                        checked = true,
                        onCheckedChange = {},
                        shapes = InteractiveListItemDefaults.segmentedShapes(idx, count),
                        content = { Text("Content") },
                        leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        supportingContent = { Text("Supporting") },
                    )
                }
            }
        }

        assertAgainstGolden("segmentedListItem_allChecked_${scheme.name}")
    }

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                ColorSchemeWrapper("lightTheme", lightColorScheme()),
                ColorSchemeWrapper("darkTheme", darkColorScheme()),
            )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule
            .onNodeWithTag(ListTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
