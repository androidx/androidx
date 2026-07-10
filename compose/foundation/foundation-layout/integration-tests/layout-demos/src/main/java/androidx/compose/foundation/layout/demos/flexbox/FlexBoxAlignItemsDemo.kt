/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.layout.demos.flexbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignItems
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun FlexBoxAlignItemsDemo() {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Items - Stretch", fontSize = 20.sp)
        FlexBoxRowAlignItemsStretchSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Items - Center", fontSize = 20.sp)
        FlexBoxRowAlignItemsCenterSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Items - Start", fontSize = 20.sp)
        FlexBoxRowAlignItemsStartSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Items - End", fontSize = 20.sp)
        FlexBoxRowAlignItemsEndSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Align Items - Stretch", fontSize = 20.sp)
        FlexBoxColumnAlignItemsStretchSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Align Items - Center", fontSize = 20.sp)
        FlexBoxColumnAlignItemsCenterSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Align Items - Start", fontSize = 20.sp)
        FlexBoxColumnAlignItemsStartSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Align Items - End", fontSize = 20.sp)
        FlexBoxColumnAlignItemsEndSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Items - Baseline", fontSize = 20.sp)
        FlexBoxRowAlignItemsBaselineSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Items - To LastBaseline", fontSize = 20.sp)
        FlexBoxRowAlignItemsToLastBaselineSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Items - To Custom Baseline", fontSize = 20.sp)
        FlexBoxRowAlignItemsToCustomBaselineSample()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowAlignItemsStretchSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            alignItems(FlexAlignItems.Stretch)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "1", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "2", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "3", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowAlignItemsCenterSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            alignItems(FlexAlignItems.Center)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "1", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "2", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "3", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowAlignItemsStartSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            alignItems(FlexAlignItems.Start)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "1", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "2", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "3", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowAlignItemsEndSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            alignItems(FlexAlignItems.End)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "1", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "2", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "3", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowAlignItemsBaselineSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            alignItems(FlexAlignItems.Baseline)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "1", fontSize = 40.sp, modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(80.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(
                text = "This one\n" + "has more lines\n" + "of text.",
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(80.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "3", fontSize = 10.sp, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowAlignItemsToLastBaselineSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            alignItems(LastBaseline)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "1", fontSize = 40.sp, modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(80.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(
                text = "This one\n" + "has more lines\n" + "of text.",
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(80.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "3", fontSize = 10.sp, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowAlignItemsToCustomBaselineSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            alignItems { measured ->
                // Custom baseline at the bottom of the item
                measured.measuredHeight / 2
            }
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "1", fontSize = 40.sp, modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.width(80.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(
                text = "This one\n" + "has more lines\n" + "of text.",
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Box(
            modifier =
                Modifier.width(50.dp)
                    .height(80.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "3", fontSize = 10.sp, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnAlignItemsStretchSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            alignItems(FlexAlignItems.Stretch)
        },
        modifier = Modifier.height(250.dp).border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "1", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "2", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "3", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnAlignItemsCenterSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            alignItems(FlexAlignItems.Center)
        },
        modifier = Modifier.height(250.dp).border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "1", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "2", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "3", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnAlignItemsStartSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            alignItems(FlexAlignItems.Start)
        },
        modifier = Modifier.height(250.dp).border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "1", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "2", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "3", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnAlignItemsEndSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            alignItems(FlexAlignItems.End)
        },
        modifier = Modifier.height(250.dp).border(1.dp, Color.Black),
    ) {
        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(100.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "1", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "2", modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier =
                Modifier.height(50.dp)
                    .width(50.dp)
                    .background(color = randomColor())
                    .border(1.dp, color = Color.Black)
        ) {
            Text(text = "3", modifier = Modifier.align(Alignment.Center))
        }
    }
}

private fun randomColor(): Color {
    val random = Random.Default
    return Color(red = random.nextInt(256), green = random.nextInt(256), blue = random.nextInt(256))
}
