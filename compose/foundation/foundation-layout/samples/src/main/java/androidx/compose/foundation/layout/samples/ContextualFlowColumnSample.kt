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
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ContextualFlowColumn
import androidx.compose.foundation.layout.ContextualFlowColumnOverflow
import androidx.compose.foundation.layout.ContextualFlowColumnOverflowScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Sampled
@Composable
fun ContextualFlowColMaxLineDynamicSeeMore() {
    val totalCount = 300
    var maxLines by remember { mutableStateOf(2) }

    Text(
        modifier = Modifier.fillMaxWidth(1f).padding(20.dp),
        text =
            "ContextualFlowColumn (based on Subcompose)" +
                " is great for Large Items & +N dynamic labels",
        fontWeight = FontWeight.Bold
    )

    val moreOrCollapseIndicator =
        @Composable { scope: ContextualFlowColumnOverflowScope ->
            val remainingItems = totalCount - scope.shownItemCount
            DynamicSeeMore(isHorizontal = true, remainingItems = remainingItems) {
                if (remainingItems == 0) {
                    maxLines = 2
                } else {
                    maxLines += 2
                }
            }
        }
    ContextualFlowColumn(
        modifier =
            Modifier.fillMaxWidth(1f)
                .horizontalScroll(rememberScrollState())
                .padding(20.dp)
                .height(200.dp)
                .wrapContentHeight(align = Alignment.Top),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        maxLines = maxLines,
        overflow =
            ContextualFlowColumnOverflow.expandOrCollapseIndicator(
                minColumnsToShowCollapse = 4,
                expandIndicator = moreOrCollapseIndicator,
                collapseIndicator = moreOrCollapseIndicator
            ),
        itemCount = totalCount
    ) { index ->
        Box(
            modifier =
                Modifier.align(Alignment.CenterHorizontally)
                    .height(50.dp)
                    .width(50.dp)
                    .background(Color.Green)
        ) {
            Text(
                text = index.toString(),
                fontSize = 18.sp,
                modifier = Modifier.padding(3.dp).align(Alignment.Center)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Sampled
@Composable
fun ContextualFlowColumn_ItemPosition() {
    Text("Ln: Line No\nPs: Position No. in Line", modifier = Modifier.padding(20.dp))
    ContextualFlowColumn(
        modifier = Modifier.fillMaxHeight(1f).width(210.dp).padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        maxItemsInEachColumn = 4,
        itemCount = 12
    ) {
        val width = 50.dp.coerceAtMost(maxWidth)
        val height = Random.nextInt(80, 100).dp.coerceAtMost(maxHeightInLine)
        Box(
            Modifier.width(width)
                .height(height)
                .background(MatchingColors.getByIndex(indexInLine)!!.color)
        ) {
            Text(
                text =
                    "Ln: ${this@ContextualFlowColumn.lineIndex}" +
                        "\nPs: ${this@ContextualFlowColumn.indexInLine}",
                fontSize = 18.sp,
                modifier = Modifier.padding(3.dp)
            )
        }
    }
}
