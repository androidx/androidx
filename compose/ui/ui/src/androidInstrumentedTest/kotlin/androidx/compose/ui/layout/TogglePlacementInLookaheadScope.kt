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

package androidx.compose.ui.layout

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TogglePlacementInLookaheadScope {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    @Test
    fun togglePlacement() {
        var place by mutableStateOf(true)
        var newChildAdded by mutableStateOf(false)
        val placed = mutableListOf(false, false)
        rule.setContent {
            LookaheadScope {
                Layout(content = { TestItem(newChildAdded, placed = placed) }) { list, constraints
                    ->
                    val placeables = list.map { it.measure(constraints) }
                    layout(placeables[0].width, placeables[0].height) {
                        if (place) {
                            placeables.forEach { it.place(0, 0) }
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            // Avoid placing the children and at the same time add a new child to the subtree
            newChildAdded = true
            place = false
        }
        rule.waitForIdle()
        assertFalse(placed[0])
        assertFalse(placed[1])
        rule.runOnIdle { place = true }
        rule.waitForIdle()
        assertTrue(placed[0])
        assertTrue(placed[1])
    }

    @Test
    fun togglePlacementInModifier() {
        var place by mutableStateOf(true)
        var newChildAdded by mutableStateOf(false)
        rule.setContent {
            LookaheadScope {
                TestItem(
                    newChildAdded,
                    Modifier.layout { m, constraints ->
                        val p = m.measure(constraints)
                        layout(p.width, p.height) {
                            if (place) {
                                p.place(0, 0)
                            }
                        }
                    },
                )
            }
        }
        rule.runOnIdle {
            // Avoid placing the children and at the same time add a new child to the subtree
            newChildAdded = true
            place = false
        }
        rule.waitForIdle()
        rule.runOnIdle { place = true }
        rule.waitForIdle()
    }

    @Test
    fun reusableContentInLookahead() {
        var place by mutableStateOf(true)
        var reusableContentKey by mutableStateOf(1)
        val placed = mutableListOf(false, false)
        rule.setContent {
            LookaheadScope {
                Layout(
                    measurePolicy = { list, constraints ->
                        val placeables = list.map { it.measure(constraints) }
                        layout(placeables[0].width, placeables[0].height) {
                            if (place) {
                                placeables.forEach { it.place(0, 0) }
                            }
                        }
                    },
                    content = {
                        Card {
                            Column {
                                Image(
                                    painter = ColorPainter(Color.Blue),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                                )

                                ReusableContent(reusableContentKey) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier =
                                            Modifier.padding(
                                                    start = 12.dp,
                                                    top = 8.dp,
                                                    end = 12.dp,
                                                    bottom = 12.dp
                                                )
                                                .fillMaxWidth()
                                                .layout { measurable, constraints ->
                                                    measurable.measure(constraints).run {
                                                        layout(width, height) {
                                                            if (isLookingAhead) {
                                                                placed[0] = true
                                                            } else {
                                                                placed[1] = true
                                                            }
                                                            @Suppress("UNUSED_EXPRESSION")
                                                            reusableContentKey // force a read
                                                            place(0, 0)
                                                        }
                                                    }
                                                }
                                    ) {
                                        Text(
                                            text = "Static text",
                                            color = Color.White,
                                            modifier =
                                                Modifier.background(
                                                        Color.Gray,
                                                        RoundedCornerShape(2.dp)
                                                    )
                                                    .padding(
                                                        start = 3.dp,
                                                        end = 3.dp,
                                                        top = 0.5.dp,
                                                        bottom = 1.dp
                                                    )
                                        )

                                        val badgeModifier =
                                            Modifier.border(
                                                    0.5f.dp,
                                                    Color.Black,
                                                    RoundedCornerShape(2.dp)
                                                )
                                                .padding(
                                                    start = 3.dp,
                                                    end = 3.dp,
                                                    top = 0.5.dp,
                                                    bottom = 1.dp
                                                )

                                        Text(
                                            text = "Updated",
                                            modifier = badgeModifier,
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
        assertTrue(placed[0])
        assertTrue(placed[1])
        placed[0] = false
        placed[1] = false

        rule.runOnIdle { place = false }
        rule.runOnIdle { reusableContentKey++ }
        assertFalse(placed[0])
        assertFalse(placed[1])

        rule.runOnIdle { place = true }
        rule.waitForIdle()
        assertTrue(placed[0])
        assertTrue(placed[1])
        rule.waitForIdle()
    }

    @Composable
    private fun TestItem(
        showNewText: Boolean,
        modifier: Modifier = Modifier,
        placed: MutableList<Boolean> = mutableListOf(false, false)
    ) {
        Card(modifier) {
            Column {
                Image(
                    painter = ColorPainter(Color.Blue),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 12.dp)
                            .fillMaxWidth()
                ) {
                    Text(
                        text = "Static text",
                        color = Color.White,
                        modifier =
                            Modifier.background(Color.Gray, RoundedCornerShape(2.dp))
                                .padding(start = 3.dp, end = 3.dp, top = 0.5.dp, bottom = 1.dp)
                    )

                    val badgeModifier =
                        Modifier.border(0.5f.dp, Color.Black, RoundedCornerShape(2.dp))
                            .padding(start = 3.dp, end = 3.dp, top = 0.5.dp, bottom = 1.dp)

                    if (showNewText) {
                        Text(
                            text = "New",
                            modifier =
                                badgeModifier.layout { measurable, constraints ->
                                    measurable.measure(constraints).run {
                                        layout(width, height) {
                                            if (isLookingAhead) {
                                                placed[0] = true
                                            } else {
                                                placed[1] = true
                                            }
                                            place(0, 0)
                                        }
                                    }
                                },
                        )
                    }

                    Text(
                        text = "Updated",
                        modifier = badgeModifier,
                    )
                }
            }
        }
    }
}
