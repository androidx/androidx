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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PlacementScopeMotionFrameOfReferenceTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testLazyList() {
        var offset by mutableStateOf(IntOffset(0, 0))
        val coords = arrayOfNulls<LayoutCoordinates>(30)
        var rootCoords: LayoutCoordinates? = null
        val state = LazyListState()
        val offsets =
            listOf(
                IntOffset(0, 0),
                IntOffset(5, 20),
                IntOffset(25, 0),
                IntOffset(100, 10),
            )
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    Modifier.layout { m, c ->
                            m.measure(c).run {
                                layout(width, height) {
                                    rootCoords = coordinates
                                    place(0, 0)
                                }
                            }
                        }
                        .offset { offset }
                ) {
                    LazyColumn(state = state, modifier = Modifier.requiredHeight(100.dp)) {
                        items(30) { index ->
                            Box(Modifier.size(20.dp).onGloballyPositioned { coords[index] = it })
                        }
                    }
                }
            }
        }
        repeat(4) {
            val itemId = it * 5
            offset = offsets[it]
            rule.runOnIdle { runBlocking { state.scrollToItem(itemId) } }
            repeat(5) {
                assertEquals(
                    offset,
                    coords[itemId + it]!!
                        .let {
                            rootCoords!!.localPositionOf(it, includeMotionFrameOfReference = false)
                        }
                        .round()
                )
                assertEquals(
                    offset + IntOffset(0, it * 20),
                    coords[itemId + it]!!
                        .let {
                            rootCoords!!.localPositionOf(it, includeMotionFrameOfReference = true)
                        }
                        .round()
                )
            }
        }
    }

    @Test
    fun testLazyGrid() {
        var offset by mutableStateOf(IntOffset(0, 0))
        val coords = arrayOfNulls<LayoutCoordinates>(60)
        var rootCoords: LayoutCoordinates? = null
        val state = LazyGridState()
        val offsets =
            listOf(
                IntOffset(0, 0),
                IntOffset(5, 20),
                IntOffset(25, 0),
                IntOffset(100, 10),
            )
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    Modifier.layout { m, c ->
                            m.measure(c).run {
                                layout(width, height) {
                                    rootCoords = coordinates
                                    place(0, 0)
                                }
                            }
                        }
                        .offset { offset }
                ) {
                    LazyVerticalGrid(
                        GridCells.Fixed(2),
                        modifier = Modifier.requiredHeight(100.dp).requiredWidth(40.dp),
                        state = state
                    ) {
                        items(60) { index ->
                            Box(Modifier.size(20.dp).onGloballyPositioned { coords[index] = it })
                        }
                    }
                }
            }
        }
        repeat(4) {
            val itemId = it * 5 * 2
            offset = offsets[it]
            rule.runOnIdle { runBlocking { state.scrollToItem(itemId) } }
            rule.waitForIdle()
            repeat(5) {
                assertEquals(
                    offset,
                    coords[itemId + it]!!
                        .let {
                            rootCoords!!.localPositionOf(it, includeMotionFrameOfReference = false)
                        }
                        .round()
                )
                assertEquals(
                    offset + IntOffset(0 + it % 2 * 20, it / 2 * 20),
                    coords[itemId + it]!!
                        .let {
                            rootCoords!!.localPositionOf(it, includeMotionFrameOfReference = true)
                        }
                        .round()
                )
            }
        }
    }

    @Test
    fun testLazyStaggeredGrid() {
        var offset by mutableStateOf(IntOffset(0, 0))
        val coords = arrayOfNulls<LayoutCoordinates>(60)
        var rootCoords: LayoutCoordinates? = null
        val state = LazyStaggeredGridState()
        val offsets =
            listOf(
                IntOffset(0, 0),
                IntOffset(5, 20),
                IntOffset(25, 0),
                IntOffset(100, 10),
            )
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    Modifier.layout { m, c ->
                            m.measure(c).run {
                                layout(width, height) {
                                    rootCoords = coordinates
                                    place(0, 0)
                                }
                            }
                        }
                        .offset { offset }
                ) {
                    LazyVerticalStaggeredGrid(
                        state = state,
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.requiredHeight(100.dp).requiredWidth(40.dp)
                    ) {
                        items(60) { index ->
                            Box(
                                Modifier.size(20.dp, ((index % 2) * 5).dp + 15.dp)
                                    .onGloballyPositioned { coords[index] = it }
                            )
                        }
                    }
                }
            }
        }
        repeat(4) {
            val itemId = it * 10
            offset = offsets[it]
            rule.runOnIdle { runBlocking { state.scrollToItem(itemId) } }
            repeat(5) {
                assertEquals(
                    offset,
                    coords[itemId + it]!!
                        .let {
                            rootCoords!!.localPositionOf(it, includeMotionFrameOfReference = false)
                        }
                        .round()
                )
            }
        }
    }

    @Test
    fun testPager() {
        var offset by mutableStateOf(IntOffset(0, 0))
        val coords = arrayOfNulls<LayoutCoordinates>(30)
        var rootCoords: LayoutCoordinates? = null
        val state = PagerState { 30 }
        val offsets =
            listOf(
                IntOffset(0, 0),
                IntOffset(5, 20),
                IntOffset(25, 0),
                IntOffset(100, 10),
            )
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    Modifier.layout { m, c ->
                            m.measure(c).run {
                                layout(width, height) {
                                    rootCoords = coordinates
                                    place(0, 0)
                                }
                            }
                        }
                        .offset { offset }
                ) {
                    HorizontalPager(
                        state,
                        pageSize = PageSize.Fixed(20.dp),
                        modifier = Modifier.requiredHeight(20.dp).requiredWidth(100.dp)
                    ) { index ->
                        Box(Modifier.size(20.dp, 20.dp).onGloballyPositioned { coords[index] = it })
                    }
                }
            }
        }
        repeat(4) {
            val itemId = it * 5
            offset = offsets[it]
            rule.runOnIdle { runBlocking { state.scrollToPage(itemId) } }
            repeat(5) {
                assertEquals(
                    offset,
                    requireNotNull(coords[itemId + it]) { "item $itemId, it = $it" }
                        .let {
                            rootCoords!!.localPositionOf(it, includeMotionFrameOfReference = false)
                        }
                        .round(),
                )
            }
        }
    }
}
