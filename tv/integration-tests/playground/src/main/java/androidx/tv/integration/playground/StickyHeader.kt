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

package androidx.tv.integration.playground

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MonthActivity(
    val month: String,
    val activities: List<String>,
)

val monthActivities =
    listOf(
        MonthActivity(
            month = "October 2022",
            activities = buildActivities(),
        ),
        MonthActivity(
            month = "September 2022",
            activities = buildActivities(),
        ),
        MonthActivity(
            month = "August 2022",
            activities = buildActivities(),
        ),
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StickyHeaderContent() {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        monthActivities.forEachIndexed { monthIndex, monthActivity ->
            val isLastMonth = monthIndex == monthActivities.lastIndex

            stickyHeader { MonthHeader(month = monthActivity.month) }

            items(monthActivity.activities.size) { activityIndex ->
                val activity = monthActivity.activities[activityIndex]
                val isLastActivity = activityIndex == monthActivity.activities.lastIndex

                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            MonthActivityComponent(this, activity)
                        }

                        if (isLastActivity && isLastMonth.not()) {
                            MonthDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthActivityComponent(boxScope: BoxScope, activity: String) {
    var isFocused by remember { mutableStateOf(false) }

    boxScope.apply {
        Box(
            modifier =
                Modifier.align(Alignment.CenterEnd)
                    .fillMaxWidth(0.5f)
                    .height(70.dp)
                    .onFocusChanged { isFocused = it.isFocused }
                    .border(
                        width = 2.dp,
                        color = if (isFocused) Color.Red else Color.White,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .focusable(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = activity, color = Color.White)
        }
    }
}

@Composable
private fun MonthHeader(month: String) {
    Text(
        text = month,
        fontSize = 20.sp,
        color = Color.White,
    )
}

@Composable
private fun MonthDivider() {
    Box(
        modifier =
            Modifier.fillMaxWidth().height(2.dp).background(Color.White, RoundedCornerShape(50))
    )
}

private fun buildActivities(
    count: Int = 10,
    buildActivity: (index: Int) -> String = { "Activity $it" }
): List<String> = (0..count).map(buildActivity)
