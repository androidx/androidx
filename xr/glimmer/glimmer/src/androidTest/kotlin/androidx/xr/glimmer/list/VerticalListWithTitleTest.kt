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

package androidx.xr.glimmer.list

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.setGlimmerThemeContent
import androidx.xr.glimmer.testutils.createGlimmerRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class VerticalListWithTitleTest {

    @get:Rule(0) val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun listTop_isPositionedAt_titleCenter() {
        rule.setGlimmerThemeContent {
            VerticalList(
                title = { TitleChip(Modifier.testTag("title")) { Text("Two lines\ntitle") } }
            ) {
                items(100) { ListItem { Text("Item-$it") } }
            }
        }

        val title = rule.onNodeWithTag("title").fetchSemanticsNode()
        val list = rule.onListNode().fetchSemanticsNode()

        val centerOfTitle = title.boundsInRoot.center.y.toInt()
        val topOfList = list.boundsInRoot.top.toInt()
        assertThat(centerOfTitle).isEqualTo(topOfList)
    }

    @Test
    fun listTop_isPositionedAt_theTallestTitleCenter() {
        rule.setGlimmerThemeContent(density = Density(1f)) {
            VerticalList(
                modifier = Modifier.size(400.dp),
                title = {
                    Box(Modifier.size(100.dp, 10.dp))
                    Box(Modifier.size(100.dp, 20.dp))
                    Box(Modifier.size(100.dp, 30.dp))
                },
            ) {
                items(100) { ListItem { Text("Item-$it") } }
            }
        }

        val list = rule.onListNode().fetchSemanticsNode()

        // The height of the tallest title is 30px (density = 1f).
        // List must start at the vertical center of it.
        assertThat(list.boundsInRoot.top).isEqualTo(15)
    }

    @Test
    fun title_isWider_thanList() {
        rule.setGlimmerThemeContent(density = Density(1f)) {
            VerticalList(
                contentPadding = PaddingValues(0.dp),
                title = { Box(Modifier.testTag("title").size(300.dp, 100.dp)) },
            ) {
                item { Box(Modifier.testTag("item").size(100.dp, 100.dp)) }
            }
        }

        val title = rule.onNodeWithTag("title").getBoundsInRoot()
        val item = rule.onNodeWithTag("item").getBoundsInRoot()

        assertThat(title)
            .isEqualTo(DpRect(left = 0.dp, top = 0.dp, right = 300.dp, bottom = 100.dp))
        assertThat(item)
            .isEqualTo(DpRect(left = 100.dp, top = 50.dp, right = 200.dp, bottom = 150.dp))
    }

    @Test
    fun list_occupies_restOfAvailableSpace() {
        rule.setGlimmerThemeContent(density = Density(1f)) {
            Box(Modifier.height(325.dp)) {
                VerticalList(
                    modifier = Modifier.testTag("root"),
                    title = { Box(Modifier.size(100.dp, 50.dp)) },
                ) {
                    items(100) { ListItem { Text("Item-$it") } }
                }
            }
        }

        val root = rule.onNodeWithTag("root").fetchSemanticsNode()
        val list = rule.onListNode().fetchSemanticsNode()

        // Total height is 325, title height is 50, list height is 300.
        assertThat(root.size.height).isEqualTo(325)
        assertThat(list.size.height).isEqualTo(300)
    }

    @Test
    fun allTitles_arePositionedAt_centerTop() {
        rule.setGlimmerThemeContent(density = Density(1f)) {
            VerticalList(
                modifier = Modifier.size(400.dp),
                title = {
                    Box(Modifier.testTag("title-1").size(200.dp, 100.dp))
                    Box(Modifier.testTag("title-2").size(100.dp, 200.dp))
                },
            ) {
                items(100) { ListItem { Text("Item-$it") } }
            }
        }

        val firstTitleBounds = rule.onNodeWithTag("title-1").getBoundsInRoot()
        val secondTitleBounds = rule.onNodeWithTag("title-2").getBoundsInRoot()

        assertThat(firstTitleBounds)
            .isEqualTo(DpRect(left = 100.dp, top = 50.dp, right = 300.dp, bottom = 150.dp))
        assertThat(secondTitleBounds)
            .isEqualTo(DpRect(left = 150.dp, top = 0.dp, right = 250.dp, bottom = 200.dp))
    }

    @Test
    fun title_isEmpty() {
        rule.setGlimmerThemeContent(density = Density(1f)) {
            Box(Modifier.size(400.dp)) {
                VerticalList(
                    modifier = Modifier.testTag("root"),
                    title = { /* No composables. */ },
                ) {
                    items(100) { ListItem { Text("Item-$it") } }
                }
            }
        }

        val listBounds = rule.onNodeWithTag("root").onChildAt(0).getBoundsInRoot()

        // List takes all the available space when there is no title.
        assertThat(listBounds)
            .isEqualTo(DpRect(left = 0.dp, top = 0.dp, right = 400.dp, bottom = 400.dp))
    }

    /**
     * Since we cannot provide a modifier to the underlying [VerticalList], we have a workaround to
     * find it, as the List is expected to be the only scrollable element.
     */
    private fun SemanticsNodeInteractionsProvider.onListNode(): SemanticsNodeInteraction {
        return onNode(hasScrollAction())
    }
}
