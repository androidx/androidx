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
import androidx.compose.foundation.layout.FlexAlignContent
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexWrap
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun FlexBoxAlignContentDemo() {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Content - Stretch", fontSize = 20.sp)
        FlexBoxRowAlignContentStretchSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Content - Center", fontSize = 20.sp)
        FlexBoxRowAlignContentCenterSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Content - SpaceAround", fontSize = 20.sp)
        FlexBoxRowAlignContentSpaceAroundSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Content - SpaceBetween", fontSize = 20.sp)
        FlexBoxRowAlignContentSpaceBetweenSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Content - Start", fontSize = 20.sp)
        FlexBoxRowAlignContentStartSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Row Align Content - End", fontSize = 20.sp)
        FlexBoxRowAlignContentEndSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Align Content - Stretch", fontSize = 20.sp)
        FlexBoxColumnAlignContentStretchSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Align Content - Center", fontSize = 20.sp)
        FlexBoxColumnAlignContentCenterSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Align Content - SpaceAround", fontSize = 20.sp)
        FlexBoxColumnAlignContentSpaceAroundSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Align Content - SpaceBetween", fontSize = 20.sp)
        FlexBoxColumnAlignContentSpaceBetweenSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Align Content - Start", fontSize = 20.sp)
        FlexBoxColumnAlignContentStartSample()
        Spacer(Modifier.height(24.dp))

        Text(text = "Column Align Content - End", fontSize = 20.sp)
        FlexBoxColumnAlignContentEndSample()
    }
}

@Composable
@OptIn(ExperimentalFlexBoxApi::class)
private fun FlexBoxRowAlignContentStretchSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            wrap(FlexWrap.Wrap)
            alignContent(FlexAlignContent.Stretch)
        },
        modifier = Modifier.fillMaxWidth().height(150.dp).border(1.dp, Color.Black),
    ) {
        repeat(8) {
            Box(
                modifier =
                    Modifier.width(70.dp)
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
private fun FlexBoxRowAlignContentCenterSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            wrap(FlexWrap.Wrap)
            alignContent(FlexAlignContent.Center)
        },
        modifier = Modifier.fillMaxWidth().height(150.dp).border(1.dp, Color.Black),
    ) {
        repeat(8) {
            Box(
                modifier =
                    Modifier.width(70.dp)
                        .height(40.dp)
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
private fun FlexBoxRowAlignContentSpaceAroundSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            wrap(FlexWrap.Wrap)
            alignContent(FlexAlignContent.SpaceAround)
        },
        modifier = Modifier.fillMaxWidth().height(150.dp).border(1.dp, Color.Black),
    ) {
        repeat(8) {
            Box(
                modifier =
                    Modifier.width(70.dp)
                        .height(40.dp)
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
private fun FlexBoxRowAlignContentSpaceBetweenSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            wrap(FlexWrap.Wrap)
            alignContent(FlexAlignContent.SpaceBetween)
        },
        modifier = Modifier.fillMaxWidth().height(150.dp).border(1.dp, Color.Black),
    ) {
        repeat(8) {
            Box(
                modifier =
                    Modifier.width(70.dp)
                        .height(40.dp)
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
private fun FlexBoxRowAlignContentStartSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            wrap(FlexWrap.Wrap)
            alignContent(FlexAlignContent.Start)
        },
        modifier = Modifier.fillMaxWidth().height(150.dp).border(1.dp, Color.Black),
    ) {
        repeat(8) {
            Box(
                modifier =
                    Modifier.width(70.dp)
                        .height(40.dp)
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
private fun FlexBoxRowAlignContentEndSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Row)
            wrap(FlexWrap.Wrap)
            alignContent(FlexAlignContent.End)
        },
        modifier = Modifier.fillMaxWidth().height(150.dp).border(1.dp, Color.Black),
    ) {
        repeat(8) {
            Box(
                modifier =
                    Modifier.width(70.dp)
                        .height(40.dp)
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
private fun FlexBoxColumnAlignContentStretchSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            wrap(FlexWrap.Wrap)
            alignContent(FlexAlignContent.Stretch)
        },
        modifier = Modifier.height(300.dp).width(250.dp).border(1.dp, Color.Black),
    ) {
        repeat(8) {
            Box(
                modifier =
                    Modifier.height(70.dp)
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
private fun FlexBoxColumnAlignContentCenterSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            wrap(FlexWrap.Wrap)
            alignContent(FlexAlignContent.Center)
        },
        modifier = Modifier.height(300.dp).width(250.dp).border(1.dp, Color.Black),
    ) {
        repeat(8) {
            Box(
                modifier =
                    Modifier.height(70.dp)
                        .width(40.dp)
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
private fun FlexBoxColumnAlignContentSpaceAroundSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            wrap(FlexWrap.Wrap)
            alignContent(FlexAlignContent.SpaceAround)
        },
        modifier = Modifier.height(300.dp).width(250.dp).border(1.dp, Color.Black),
    ) {
        repeat(8) {
            Box(
                modifier =
                    Modifier.height(70.dp)
                        .width(40.dp)
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
private fun FlexBoxColumnAlignContentSpaceBetweenSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            wrap(FlexWrap.Wrap)
            alignContent(FlexAlignContent.SpaceBetween)
        },
        modifier = Modifier.height(300.dp).width(250.dp).border(1.dp, Color.Black),
    ) {
        repeat(8) {
            Box(
                modifier =
                    Modifier.height(70.dp)
                        .width(40.dp)
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
private fun FlexBoxColumnAlignContentStartSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            wrap(FlexWrap.Wrap)
            alignContent(FlexAlignContent.Start)
        },
        modifier = Modifier.height(300.dp).width(250.dp).border(1.dp, Color.Black),
    ) {
        repeat(8) {
            Box(
                modifier =
                    Modifier.height(70.dp)
                        .width(40.dp)
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
private fun FlexBoxColumnAlignContentEndSample() {
    FlexBox(
        config = {
            direction(FlexDirection.Column)
            wrap(FlexWrap.Wrap)
            alignContent(FlexAlignContent.End)
        },
        modifier = Modifier.height(300.dp).width(250.dp).border(1.dp, Color.Black),
    ) {
        repeat(8) {
            Box(
                modifier =
                    Modifier.height(70.dp)
                        .width(40.dp)
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
