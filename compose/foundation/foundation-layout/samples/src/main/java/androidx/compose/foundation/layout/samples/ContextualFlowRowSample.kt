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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ContextualFlowRowOverflow
import androidx.compose.foundation.layout.ContextualFlowRowOverflowScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
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
fun ContextualFlowRowMaxLineDynamicSeeMore() {
    val totalCount = 300
    var maxLines by remember {
        mutableStateOf(2)
    }

    Text(modifier = Modifier
        .fillMaxWidth(1f)
        .padding(20.dp)
        .wrapContentHeight(align = Alignment.Top),
        text = "ContextualFlowRow (based on Subcompose)" +
            " is great for Large Items & +N dynamic labels",
        fontWeight = FontWeight.Bold
    )
    val moreOrCollapseIndicator = @Composable { scope: ContextualFlowRowOverflowScope ->
        val remainingItems = totalCount - scope.shownItemCount
        DynamicSeeMore(
            isHorizontal = true,
            remainingItems = remainingItems
        ) {
            if (remainingItems == 0) {
                maxLines = 2
            } else {
                maxLines += 5
            }
        }
    }
    ContextualFlowRow(
        modifier = Modifier
            .fillMaxWidth(1f)
            .padding(20.dp)
            .wrapContentHeight(align = Alignment.Top),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        maxLines = maxLines,
        overflow = ContextualFlowRowOverflow.expandOrCollapseIndicator(
            minRowsToShowCollapse = 4,
            expandIndicator = moreOrCollapseIndicator,
            collapseIndicator = moreOrCollapseIndicator
        ),
        itemCount = totalCount
    ) {
        Box(
            Modifier
                .align(Alignment.CenterVertically)
                .width(50.dp)
                .height(50.dp)
                .background(Color.Green)
        ) {
            Text(text = it.toString(), fontSize = 18.sp, modifier =
            Modifier
                .padding(3.dp)
                .align(Alignment.Center))
        }
    }
}

@Composable
internal fun DynamicSeeMore(
    isHorizontal: Boolean,
    remainingItems: Int,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .clickable(onClick = onClick)
            .wrapContentWidth()
            .height(50.dp)
            .background(Color.Green)
    ) {
        val collapseText = if (isHorizontal) "^" else "<"
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(3.dp),
            text = if (remainingItems == 0) collapseText else "+$remainingItems",
            fontSize = 18.sp
        )
    }
}
