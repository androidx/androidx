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
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexJustifyContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun FlexBoxJustifyContentDemo() {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Justify Content - Start", fontSize = 20.sp)
        FlexBoxRowJustifyContentStartSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Justify Content - Center", fontSize = 20.sp)
        FlexBoxRowJustifyContentCenterSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Justify Content - SpaceAround", fontSize = 20.sp)
        FlexBoxRowJustifyContentSpaceAroundSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Justify Content - SpaceBetween", fontSize = 20.sp)
        FlexBoxRowJustifyContentSpaceBetweenSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Justify Content - End", fontSize = 20.sp)
        FlexBoxRowJustifyContentEndSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Justify Content - Start", fontSize = 20.sp)
        FlexBoxColumnJustifyContentStartSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Justify Content - Center", fontSize = 20.sp)
        FlexBoxColumnJustifyContentCenterSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Justify Content - SpaceAround", fontSize = 20.sp)
        FlexBoxColumnJustifyContentSpaceAroundSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Justify Content - SpaceBetween", fontSize = 20.sp)
        FlexBoxColumnJustifyContentSpaceBetweenSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Justify Content - End", fontSize = 20.sp)
        FlexBoxColumnJustifyContentEndSample()
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowJustifyContentStartSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            justifyContent(FlexJustifyContent.Start)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier.height(50.dp)
                        .widthIn(min = 50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowJustifyContentCenterSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            justifyContent(FlexJustifyContent.Center)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier.height(50.dp)
                        .widthIn(min = 50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowJustifyContentSpaceAroundSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            justifyContent(FlexJustifyContent.SpaceAround)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier.height(50.dp)
                        .widthIn(min = 50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowJustifyContentSpaceBetweenSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            justifyContent(FlexJustifyContent.SpaceBetween)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier.height(50.dp)
                        .widthIn(min = 50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowJustifyContentEndSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            justifyContent(FlexJustifyContent.End)
        },
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier.height(50.dp)
                        .widthIn(min = 50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnJustifyContentStartSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            justifyContent(FlexJustifyContent.Start)
        },
        modifier = Modifier.height(300.dp).fillMaxWidth().border(1.dp, Color.Black),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier.width(50.dp)
                        .heightIn(min = 50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnJustifyContentCenterSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            justifyContent(FlexJustifyContent.Center)
        },
        modifier = Modifier.height(300.dp).fillMaxWidth().border(1.dp, Color.Black),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier.width(50.dp)
                        .heightIn(min = 50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnJustifyContentSpaceAroundSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            justifyContent(FlexJustifyContent.SpaceAround)
        },
        modifier = Modifier.height(300.dp).fillMaxWidth().border(1.dp, Color.Black),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier.width(50.dp)
                        .heightIn(min = 50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnJustifyContentSpaceBetweenSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            justifyContent(FlexJustifyContent.SpaceBetween)
        },
        modifier = Modifier.height(300.dp).fillMaxWidth().border(1.dp, Color.Black),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier.width(50.dp)
                        .heightIn(min = 50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxColumnJustifyContentEndSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            justifyContent(FlexJustifyContent.End)
        },
        modifier = Modifier.height(300.dp).fillMaxWidth().border(1.dp, Color.Black),
    ) {
        repeat(4) {
            Box(
                modifier =
                    Modifier.width(50.dp)
                        .heightIn(min = 50.dp)
                        .background(color = randomColor())
                        .border(1.dp, color = Color.Black)
            ) {
                Text(text = "$it", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

private fun randomColor(): Color {
    val random = Random.Default
    return Color(red = random.nextInt(256), green = random.nextInt(256), blue = random.nextInt(256))
}
