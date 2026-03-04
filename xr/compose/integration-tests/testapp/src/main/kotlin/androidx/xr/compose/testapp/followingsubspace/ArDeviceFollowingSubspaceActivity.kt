/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.xr.compose.testapp.followingsubspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi
import androidx.xr.compose.spatial.FollowingSubspace
import androidx.xr.compose.subspace.FollowBehavior
import androidx.xr.compose.subspace.FollowTarget
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.TrackedDimensions
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.components.TopBarWithBackArrow
import androidx.xr.runtime.DeviceTrackingMode
import java.time.LocalDate
import java.time.format.TextStyle

data class TodoItem(val description: String, val isCompleted: Boolean)

class FollowingSubspaceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MainContent() }
    }

    @OptIn(ExperimentalFollowingSubspaceApi::class, ExperimentalMaterial3Api::class)
    @Composable
    private fun MainContent() {
        val session = checkNotNull(LocalSession.current) { "session must be initialized" }
        session.configure(
            config = session.config.copy(deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN)
        )

        val todoItems = remember {
            mutableStateListOf(
                TodoItem("Buy groceries", true),
                TodoItem("Finish report", false),
                TodoItem("Review PRs", false),
            )
        }
        // State for the soft follow duration slider
        var softFollowDuration by remember { mutableIntStateOf(1000) }

        FollowingSubspace(
            target = FollowTarget.ArDevice(session),
            behavior = FollowBehavior.Static,
            modifier = SubspaceModifier.offset(z = (-200).dp),
        ) {
            SpatialPanel(SubspaceModifier.height(400.dp).width(600.dp)) {
                Column(
                    Modifier.fillMaxWidth()
                        .fillMaxHeight()
                        .background(Color.White)
                        .padding(all = 32.dp)
                ) {
                    Text(
                        text =
                            buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("Duration")
                                }
                                append(
                                    " - Adjusts the time, in milliseconds, it takes for the " +
                                        "content to catch up to the user.\n"
                                )
                            },
                        fontSize = 16.sp,
                    )
                }
            }
        }

        FollowingSubspace(
            target = FollowTarget.ArDevice(session),
            dimensions =
                TrackedDimensions(
                    isTranslationXTracked = true,
                    isTranslationYTracked = true,
                    isTranslationZTracked = true,
                    isRotationXTracked = true,
                    isRotationYTracked = true,
                    isRotationZTracked = false,
                ),
            behavior = FollowBehavior.Soft(durationMs = softFollowDuration),
        ) {
            SpatialPanel(SubspaceModifier.height(200.dp).width(450.dp).offset(y = (-50).dp)) {
                Box(Modifier.fillMaxSize().background(Color.Cyan)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TopBarWithBackArrow(
                            scrollBehavior = null,
                            title = "",
                            onClick = { this@FollowingSubspaceActivity.finish() },
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(top = 50.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PanelHeader("HEAD LOCKED")
                        SoftFollowSlider(
                            duration = softFollowDuration,
                            onDurationChange = { softFollowDuration = it.toInt() },
                        )
                    }
                }
            }
        }
        FollowingSubspace(
            target = FollowTarget.ArDevice(session),
            dimensions =
                TrackedDimensions(
                    isTranslationXTracked = true,
                    isTranslationYTracked = true,
                    isTranslationZTracked = true,
                ),
            behavior = FollowBehavior.Soft(durationMs = softFollowDuration),
        ) {
            SpatialCurvedRow(SubspaceModifier.width(1000.dp).height(300.dp), curveRadius = 500.dp) {
                // To-Do List Card
                SpatialColumn(SubspaceModifier.width(250.dp)) {
                    TodoListCard(todoItems) { updatedItem ->
                        val index =
                            todoItems.indexOfFirst { it.description == updatedItem.description }
                        if (index != -1) {
                            todoItems[index] =
                                updatedItem.copy(isCompleted = !updatedItem.isCompleted)
                        }
                    }
                }
                // Empty spacer
                SpatialColumn(SubspaceModifier.width(500.dp)) {}
                // Calendar Card
                SpatialColumn(SubspaceModifier.width(250.dp)) { CalendarCard() }
            }
        }
    }

    @Composable
    private fun SoftFollowSlider(duration: Int, onDurationChange: (Float) -> Unit) {
        Column(
            modifier = Modifier.width(400.dp).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Duration: $duration milliseconds",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            val min = 1f
            val max = 3000f
            val increment = 100f
            val totalValues = ((max - min) / increment) + 1
            val steps = (totalValues - 1).toInt()
            Slider(
                value = duration.toFloat(),
                onValueChange = onDurationChange,
                valueRange = min..max,
                steps = steps,
            )
        }
    }

    @Composable
    private fun TodoListCard(todoItems: List<TodoItem>, onItemClick: (TodoItem) -> Unit) {
        SpatialPanel {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F0EA)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PanelHeader("TODO LIST - BODY LOCKED")
                    todoItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onItemClick(item) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (item.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Circle,
                                    contentDescription = "Incomplete",
                                    tint = Color(0xFF757575),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                item.description,
                                fontSize = 16.sp,
                                color = if (item.isCompleted) Color.Gray else Color.Black,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun CalendarCard() {
        val currentDate = LocalDate.now()
        val currentMonth =
            currentDate.month.getDisplayName(TextStyle.FULL, LocalLocale.current.platformLocale)
        val currentYear = currentDate.year
        val currentDay = currentDate.dayOfMonth
        val daysInMonth = currentDate.lengthOfMonth()
        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        val firstDayOfWeekIndex = firstDayOfMonth.dayOfWeek.value % 7
        SpatialPanel {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F0EA)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PanelHeader("CALENDAR - BODY LOCKED")
                    Text(
                        text = "$currentMonth $currentYear",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach {
                            Text(it, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column {
                        val daysList = (1..daysInMonth).toList().toIntArray()
                        var dayIndex = 0
                        for (week in 0 until 6) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                            ) {
                                for (dayOfWeek in 0..6) {
                                    val cellIndex = week * 7 + dayOfWeek
                                    val dayToDisplay =
                                        if (
                                            cellIndex >= firstDayOfWeekIndex &&
                                                dayIndex < daysList.size
                                        ) {
                                            daysList[dayIndex].also { dayIndex++ }
                                        } else {
                                            null
                                        }
                                    if (dayToDisplay != null) {
                                        Box(
                                            modifier =
                                                Modifier.size(24.dp)
                                                    .background(
                                                        if (dayToDisplay == currentDay)
                                                            Color(0xFF4CAF50)
                                                        else Color.Transparent
                                                    ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                "$dayToDisplay",
                                                fontSize = 12.sp,
                                                color =
                                                    if (dayToDisplay == currentDay) Color.White
                                                    else Color.Black,
                                            )
                                        }
                                    } else {
                                        Spacer(Modifier.size(24.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PanelHeader(title: String) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(150.dp).height(2.dp).background(Color.DarkGray))
        Spacer(Modifier.height(16.dp))
    }
}
