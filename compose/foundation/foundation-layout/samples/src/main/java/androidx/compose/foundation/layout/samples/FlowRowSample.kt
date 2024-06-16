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

package androidx.compose.foundation.layout.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.FlowRowOverflowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Sampled
@Composable
fun SimpleFlowRow() {
    Text(
        modifier =
            Modifier.fillMaxWidth(1f).padding(20.dp).wrapContentHeight(align = Alignment.Top),
        text = "Flow Row with weights",
        fontWeight = FontWeight.Bold
    )

    FlowRow(
        Modifier.fillMaxWidth(1f).padding(20.dp).wrapContentHeight(align = Alignment.Top),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        maxItemsInEachRow = 3,
    ) {
        repeat(20) {
            Box(
                Modifier.align(Alignment.CenterVertically)
                    .width(50.dp)
                    .height(50.dp)
                    .weight(1f, true)
                    .background(Color.Green)
            ) {
                Text(text = it.toString(), fontSize = 18.sp, modifier = Modifier.padding(3.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Sampled
@Composable
fun SimpleFlowRowMaxLinesWithSeeMore() {
    val totalCount = 20
    var maxLines by remember { mutableStateOf(2) }

    Text(
        modifier =
            Modifier.fillMaxWidth(1f).padding(20.dp).wrapContentHeight(align = Alignment.Top),
        text =
            "Flow Row with total items of 40, " +
                "with Max Lines of 2 and See More, " +
                "which when clicked, increases max lines by two",
        fontWeight = FontWeight.Bold
    )
    FlowRow(
        Modifier.fillMaxWidth(1f).padding(20.dp).wrapContentHeight(align = Alignment.Top),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        maxLines = maxLines,
        overflow = FlowRowOverflow.expandIndicator { Ellipsis(text = "...") { maxLines += 2 } }
    ) {
        repeat(totalCount) {
            Box(
                Modifier.align(Alignment.CenterVertically)
                    .width(50.dp)
                    .height(50.dp)
                    .background(Color.Green)
            ) {
                Text(
                    text = it.toString(),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(3.dp).align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimpleFlowRowWithMaxHeight() {
    var initialHeight = 200.dp
    var height by remember { mutableStateOf(initialHeight) }

    Text(
        modifier =
            Modifier.fillMaxWidth(1f).padding(20.dp).wrapContentHeight(align = Alignment.Top),
        text =
            "Flow Row with total items of 40, " +
                "with Max height of 200.dp and See More/collapse button, " +
                "which when clicked, increases height by 200.dp",
        fontWeight = FontWeight.Bold
    )

    FlowRow(
        Modifier.fillMaxWidth(1f)
            .padding(20.dp)
            .height(height)
            .wrapContentHeight(align = Alignment.Top),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        overflow =
            FlowRowOverflow.expandOrCollapseIndicator(
                minHeightToShowCollapse = 200.dp,
                expandIndicator = { Ellipsis(text = "...") { height += 200.dp } },
                collapseIndicator = { Ellipsis(text = "^") { height = 100.dp } }
            )
    ) {
        repeat(40) {
            Box(
                Modifier.align(Alignment.CenterVertically)
                    .width(50.dp)
                    .height(50.dp)
                    .background(Color.Green)
            ) {
                Text(text = it.toString(), fontSize = 18.sp, modifier = Modifier.padding(3.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Sampled
@Composable
fun SimpleFlowRowMaxLinesDynamicSeeMore() {
    val totalCount = 40
    var maxLines by remember { mutableStateOf(2) }

    Text(
        modifier =
            Modifier.fillMaxWidth(1f).padding(20.dp).wrapContentHeight(align = Alignment.Top),
        text =
            "Flow Row with total items of 40, " +
                "with Max Lines of 2 and See More/collapse button, " +
                "which when clicked, increases max lines by two",
        fontWeight = FontWeight.Bold
    )
    val moreOrLessIndicator =
        @Composable { scope: FlowRowOverflowScope ->
            DynamicSeeMoreForDrawText(
                isHorizontal = true,
                totalCount = totalCount,
                { scope.shownItemCount },
                onExpand = { maxLines += 2 },
                onShrink = { maxLines = 2 }
            )
        }
    FlowRow(
        Modifier.fillMaxWidth(1f)
            .padding(20.dp)
            .wrapContentHeight()
            .padding(20.dp)
            .fillMaxWidth(1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        maxLines = maxLines,
        overflow =
            FlowRowOverflow.expandOrCollapseIndicator(
                minRowsToShowCollapse = 4,
                expandIndicator = moreOrLessIndicator,
                collapseIndicator = moreOrLessIndicator
            )
    ) {
        repeat(totalCount) {
            Box(
                Modifier.align(Alignment.CenterVertically)
                    .width(50.dp)
                    .height(50.dp)
                    .background(Color.Green)
            ) {
                Text(
                    text = it.toString(),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(3.dp).align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
internal fun DynamicSeeMoreForDrawText(
    isHorizontal: Boolean,
    totalCount: Int,
    shownItemCount: () -> Int?,
    onExpand: () -> Unit,
    onShrink: () -> Unit,
) {
    Box(
        Modifier.clickable(
                onClick = {
                    val remainingItems = shownItemCount()?.let { totalCount - it }
                    if (remainingItems == 0) {
                        onShrink()
                    } else {
                        onExpand()
                    }
                }
            )
            .width(50.dp)
            .height(50.dp)
            .background(Color.Green)
    ) {
        val textMeasurer = rememberTextMeasurer()
        Canvas(
            Modifier.fillMaxSize().layout { measurable, constraints ->
                // TextLayout can be done any time prior to its use in draw, including in a
                // background thread.
                // In this sample, text layout is measured in layout modifier. This way the layout
                // call can be restarted when async font loading completes due to the fact that
                // `.measure` call is executed in `.layout`.
                val result =
                    textMeasurer.measure(
                        text = "+2000",
                        style = TextStyle(fontSize = 24.sp),
                        constraints = constraints
                    )
                val placeable =
                    measurable.measure(
                        Constraints(
                            minWidth = result.size.width,
                            maxWidth = result.size.width,
                            minHeight = result.size.height,
                            maxHeight = result.size.height
                        )
                    )
                layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
            }
        ) {
            // This happens during draw phase.
            val collapseText = if (isHorizontal) "^" else "<"
            val remainingItems = shownItemCount()?.let { totalCount - it }
            var textLayoutResult: TextLayoutResult =
                textMeasurer.measure(
                    text = if (remainingItems == 0) collapseText else "+$remainingItems",
                    style = TextStyle(fontSize = 18.sp)
                )
            drawText(
                textLayoutResult,
                topLeft =
                    Offset(
                        (size.width - textLayoutResult.size.width) / 2,
                        (size.height - textLayoutResult.size.height) / 2,
                    )
            )
        }
    }
}

@Composable
internal fun Ellipsis(text: String, onClick: () -> Unit) {
    Box(Modifier.clickable(onClick = onClick).width(50.dp).height(50.dp).background(Color.Green)) {
        Text(
            modifier = Modifier.align(Alignment.Center).padding(3.dp),
            text = text,
            fontSize = 18.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Sampled
@Composable
fun SimpleFlowRow_EqualHeight() {
    FlowRow(
        Modifier.fillMaxWidth(1f).padding(20.dp).wrapContentHeight(align = Alignment.Top),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        maxItemsInEachRow = 3,
    ) {
        repeat(9) {
            Box(Modifier.width(100.dp).background(Color.Green).fillMaxRowHeight(1f)) {
                val text = generateRandomString(IntRange(10, 50).random())
                Text(text = text, fontSize = 18.sp, modifier = Modifier.padding(3.dp))
            }
        }
    }
}

fun generateRandomString(length: Int): String {
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    val random = Random.Default

    val randomString = StringBuilder(length)
    repeat(length) {
        val randomIndex = random.nextInt(0, charPool.size)
        val randomChar = charPool[randomIndex]
        randomString.append(randomChar)
    }
    return randomString.toString()
}
