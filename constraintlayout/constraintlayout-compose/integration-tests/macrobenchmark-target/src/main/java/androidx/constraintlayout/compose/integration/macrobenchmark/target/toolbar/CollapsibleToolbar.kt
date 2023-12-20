/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose.integration.macrobenchmark.target.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.integration.macrobenchmark.target.common.components.CardSample
import androidx.constraintlayout.compose.integration.macrobenchmark.target.common.components.OutlinedSearchBar
import kotlin.math.absoluteValue

@Preview
@Composable
fun MotionCollapseToolbarPreview() {
    MotionCollapseToolbar(
        Modifier
            .fillMaxSize()
    )
}

@Composable
fun MotionCollapseToolbar(
    modifier: Modifier = Modifier,
) {
    val collapsibleToolbarState = rememberCollapsibleToolbarState()
    Box(modifier.nestedScroll(collapsibleToolbarState)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("LazyColumn"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = collapsibleToolbarState.currentHeight)
        ) {
            items(count = 20) {
                CardSample(
                    Modifier
                        .width(250.dp)
                        .height(80.dp)
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .padding(4.dp)
                )
            }
        }
        CollapsibleToolbar(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            toolbarState = collapsibleToolbarState
        )
    }
}

private val expandedCSet = ConstraintSet {
    val title = createRefFor("title")
    val toolbar = createRefFor("toolbar")
    val container = createRefFor("container")

    constrain(container) {
        width = Dimension.fillToConstraints
        height = Dimension.value(200.dp)

        start.linkTo(parent.start)
        end.linkTo(parent.end)
        top.linkTo(parent.top)
    }

    constrain(title) {
        centerHorizontallyTo(container)
        top.linkTo(container.top)

        scaleX = 1.5f
        scaleY = 1.5f
    }
    constrain(toolbar) {
        width = Dimension.fillToConstraints
        centerHorizontallyTo(container)
        bottom.linkTo(container.bottom)
    }
}

private val collapsedCSet = ConstraintSet(expandedCSet) {
    val title = createRefFor("title")
    val toolbar = createRefFor("toolbar")
    val container = createRefFor("container")

    constrain(container) {
        height = Dimension.value(70.dp)
    }

    constrain(title) {
        clearConstraints()
        resetTransforms()

        top.linkTo(toolbar.top)
        start.linkTo(container.start)
        bottom.linkTo(toolbar.bottom)
    }

    constrain(toolbar) {
        clearHorizontal()

        top.linkTo(container.top)
        end.linkTo(container.end)
        start.linkTo(title.end, 12.dp)
    }
}

@Composable
fun rememberCollapsibleToolbarState(): CollapsibleToolbarState {
    val density = LocalDensity.current
    return remember { CollapsibleToolbarState(density) }
}

class CollapsibleToolbarState internal constructor(
    private val density: Density
) : NestedScrollConnection {
    private val maximumHeight = 200.dp
    private var minimumHeight = 70.dp

    private val consumableHeight = maximumHeight - minimumHeight

    internal val currentHeight: Dp
        get() = maximumHeight - consumedHeight

    internal val progress: Float
        get() = (consumedHeight.value / consumableHeight.value)

    private var consumedHeight by mutableStateOf(0.dp)

    private fun consumeDeltaScroll(scrollDelta: Float): Float {
        val remainingHeight = consumableHeight - consumedHeight
        val remainingPx = with(density) { remainingHeight.toPx() }
        val maxHeightPx = with(density) { consumableHeight.toPx() }

        // TODO: Simplify, no need to differentiate positive/negative scroll
        if (scrollDelta < 0) {
            // Going down
            val diff = remainingPx + scrollDelta
            if (diff > 0) {
                consumedHeight += with(density) { scrollDelta.absoluteValue.toDp() }
                return scrollDelta
            } else {
                consumedHeight = consumableHeight
                return remainingPx * -1f
            }
        } else if (scrollDelta > 0) {
            // Going up
            val diff = remainingPx + scrollDelta
            if (diff < maxHeightPx) {
                consumedHeight -= with(density) { scrollDelta.absoluteValue.toDp() }
                return scrollDelta
            } else {
                val toConsume = with(density) { consumedHeight.toPx() }
                consumedHeight = 0.dp
                return toConsume
            }
        } else {
            return 0f
        }
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        consumeDeltaScroll(available.y)
        return Offset.Zero
    }
}

@OptIn(ExperimentalMotionApi::class)
@Composable
fun CollapsibleToolbar(
    modifier: Modifier = Modifier,
    toolbarState: CollapsibleToolbarState
) {
    MotionLayout(
        modifier = modifier,
        start = expandedCSet,
        end = collapsedCSet,
        progress = toolbarState.progress
    ) {
        Box(
            Modifier
                .layoutId("container")
                .background(Color.White)
        )
        Text(
            modifier = Modifier.layoutId("title"),
            text = "MotionLayout",
            maxLines = 1,
            fontSize = 18.sp
        )
        OutlinedSearchBar(modifier = Modifier.layoutId("toolbar"))
    }
}
