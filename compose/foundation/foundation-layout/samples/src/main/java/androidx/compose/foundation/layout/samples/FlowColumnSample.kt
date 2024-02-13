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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowColumnOverflow
import androidx.compose.foundation.layout.FlowColumnOverflowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Sampled
@Composable
fun SimpleFlowColumn() {

    Text(modifier = Modifier
        .fillMaxWidth(1f)
        .padding(20.dp)
        .wrapContentHeight(align = Alignment.Top),
        text = "FlowColumn with weights",
        fontWeight = FontWeight.Bold
    )

    FlowColumn(
        Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .padding(20.dp)
            .wrapContentHeight(align = Alignment.Top)
            .height(200.dp)
            .border(BorderStroke(2.dp, Color.Gray)),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        maxItemsInEachColumn = 3,
    ) {

        repeat(17) { index ->
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(50.dp)
                    .height(50.dp)
                    .weight(1f, true)
                    .background(color = Color.Green)
            ) {
                Text(text = index.toString(), fontSize = 18.sp, modifier = Modifier.padding(3.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Sampled
@Composable
fun SimpleFlowColumnMaxLinesWithSeeMore() {
    val totalCount = 20
    var maxLines by remember {
        mutableStateOf(2)
    }

    Text(modifier = Modifier
        .fillMaxWidth(1f)
        .padding(20.dp)
        .wrapContentHeight(align = Alignment.Top),
        text = "Flow Column with Max Lines and See More",
        fontWeight = FontWeight.Bold
    )

    FlowColumn(
        modifier = Modifier
            .height(200.dp)
            .horizontalScroll(rememberScrollState())
            .padding(20.dp)
            .wrapContentWidth(align = Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        maxLines = maxLines,
        overflow = FlowColumnOverflow.expandIndicator {
            Ellipsis(text = "...") {
                maxLines += 2
            }
        }
    ) {
        repeat(totalCount) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(50.dp)
                    .width(50.dp)
                    .background(Color.Green)
            ) {
                Text(text = it.toString(), fontSize = 18.sp, modifier =
                Modifier
                    .padding(3.dp)
                    .align(Alignment.Center))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimpleFlowColumnWithMaxWidth() {
    var initialWidth = 200.dp // Reversed from initialHeight
    var width by remember { mutableStateOf(initialWidth) } // Reversed from height

    Text(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(20.dp)
            .wrapContentHeight(align = Alignment.Top),
        text = "FlowColumn with MaxWidth and See More or collapse",
        fontWeight = FontWeight.Bold
    )

    FlowColumn(
        modifier = Modifier
            .height(200.dp)
            .padding(20.dp)
            .horizontalScroll(rememberScrollState())
            .width(width)
            .wrapContentWidth(align = Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        overflow = FlowColumnOverflow.expandOrCollapseIndicator(
            minWidthToShowCollapse = 200.dp,
            expandIndicator = {
                Ellipsis(text = "...") {
                    width += 200.dp
                }
            },
            collapseIndicator = {
                Ellipsis(text = "<") {
                    width = 100.dp
                }
            })
    ) {
        repeat(40) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(50.dp)
                    .width(50.dp)
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
fun SimpleFlowColumnMaxLinesDynamicSeeMore() {
    val totalCount = 20
    var maxLines by remember {
        mutableStateOf(2)
    }

    Text(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(20.dp)
            .wrapContentHeight(align = Alignment.Top),
        text = "FlowColumn with MaxLines and +N button",
        fontWeight = FontWeight.Bold
    )
    val moreOrCollapseIndicator = @Composable { scope: FlowColumnOverflowScope ->
        DynamicSeeMoreForDrawText(
            isHorizontal = false,
            totalCount = totalCount,
            { scope.shownItemCount },
            onExpand = { maxLines += 2 },
            onShrink = { maxLines = 2 }
        )
    }
    FlowColumn(
        modifier = Modifier
            .height(200.dp)
            .padding(20.dp)
            .horizontalScroll(rememberScrollState())
            .wrapContentWidth(align = Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        maxLines = maxLines,
        overflow = FlowColumnOverflow.expandOrCollapseIndicator(
            minColumnsToShowCollapse = 4,
            expandIndicator = moreOrCollapseIndicator,
            collapseIndicator = moreOrCollapseIndicator
        )
    ) {
        repeat(totalCount) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(50.dp)
                    .width(50.dp)
                    .background(Color.Green)
            ) {
                Text(text = it.toString(), fontSize = 18.sp, modifier =
                Modifier
                    .padding(3.dp)
                    .align(Alignment.Center))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Sampled
@Composable
fun SimpleFlowColumn_EqualWidth() {
    FlowColumn(
        Modifier
            .padding(20.dp)
            .wrapContentHeight(align = Alignment.Top)
            .wrapContentWidth(align = Alignment.Start),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        maxItemsInEachColumn = 3,
    ) {
        repeat(9) {
            Box(
                Modifier
                    .height(100.dp)
                    .fillMaxColumnWidth(1f)
                    .background(Color.Green)
            ) {
                val text = generateRandomString(IntRange(1, 5).random())
                Text(
                    text = text,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(3.dp)
                )
            }
        }
    }
}
