/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class IconMarkerScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Before
    fun setUp() {
        rule.mainClock.autoAdvance = false
    }

    @After
    fun tearDown() {
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun iconMarkersGrid() {
        rule.setGlimmerThemeContent { IconMarkersGrid() }
        // Advance past focus/press animations
        rule.mainClock.advanceTimeBy(10_000)
        rule.assertRootAgainstGolden("icon_markers_grid", screenshotRule)
    }

    @Composable
    private fun IconMarkersGrid() {
        Grid(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            config = {
                repeat(4) { column(1.fr) }
                rowGap(24.dp)
            },
        ) {
            // Header Row
            HeaderCell(text = "Non-Interactive")
            HeaderCell(text = "Idle")
            HeaderCell(text = "Focused")
            HeaderCell(text = "Pressed")

            // Row 1: Custom Icon Medium
            MarkerCell {
                IconMarker(
                    size = IconMarkerSize.Medium,
                    contentDescription = "screenshot test",
                    content = { Icon(FavoriteIcon, contentDescription = null) },
                )
            }
            MarkerCell {
                IconMarker(
                    size = IconMarkerSize.Medium,
                    contentDescription = "screenshot test",
                    content = { Icon(FavoriteIcon, contentDescription = null) },
                    onClick = {},
                )
            }
            MarkerCell {
                IconMarker(
                    size = IconMarkerSize.Medium,
                    contentDescription = "screenshot test",
                    content = { Icon(FavoriteIcon, contentDescription = null) },
                    onClick = {},
                    interactionSource = AlwaysFocusedInteractionSource,
                )
            }
            MarkerCell {
                IconMarker(
                    size = IconMarkerSize.Medium,
                    contentDescription = "screenshot test",
                    content = { Icon(FavoriteIcon, contentDescription = null) },
                    onClick = {},
                    interactionSource = AlwaysPressedInteractionSource,
                )
            }

            // Row 2: Default Dot Medium
            MarkerCell {
                IconMarker(
                    size = IconMarkerSize.Medium,
                    contentDescription = "screenshot test",
                )
            }
            MarkerCell {
                IconMarker(
                    size = IconMarkerSize.Medium,
                    onClick = {},
                    contentDescription = "screenshot test",
                )
            }
            MarkerCell {
                IconMarker(
                    size = IconMarkerSize.Medium,
                    onClick = {},
                    interactionSource = AlwaysFocusedInteractionSource,
                    contentDescription = "screenshot test",
                )
            }
            MarkerCell {
                IconMarker(
                    size = IconMarkerSize.Medium,
                    onClick = {},
                    interactionSource = AlwaysPressedInteractionSource,
                    contentDescription = "screenshot test",
                )
            }

            // Row 3: Default Dot Small
            MarkerCell {
                IconMarker(size = IconMarkerSize.Small, contentDescription = "screenshot test")
            }
            MarkerCell {
                IconMarker(
                    size = IconMarkerSize.Small,
                    onClick = {},
                    contentDescription = "screenshot test",
                )
            }
            MarkerCell {
                IconMarker(
                    size = IconMarkerSize.Small,
                    onClick = {},
                    interactionSource = AlwaysFocusedInteractionSource,
                    contentDescription = "screenshot test",
                )
            }
            MarkerCell {
                IconMarker(
                    size = IconMarkerSize.Small,
                    onClick = {},
                    interactionSource = AlwaysPressedInteractionSource,
                    contentDescription = "screenshot test",
                )
            }
        }
    }

    @Composable
    private fun GridScope.HeaderCell(text: String) {
        Box(
            modifier = Modifier.gridItem(alignment = Alignment.Center),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = GlimmerTheme.typography.caption,
                fontSize = 12.sp,
            )
        }
    }

    @Composable
    private fun GridScope.MarkerCell(content: @Composable () -> Unit) {
        Box(
            modifier = Modifier.gridItem(alignment = Alignment.Center),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }
}
