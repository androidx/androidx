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

package androidx.compose.foundation.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.testutils.paparazzi.androidxPaparazzi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BasicTextPaparazziTest {
    @get:Rule
    val paparazzi = androidxPaparazzi()

    @Test
    fun colorChangingState_changesColor() {
        paparazzi.snapshot {
            val color = remember { mutableStateOf(Color.Red) }
            BasicText(
                "ABCD",
                color = { color.value }
            )
            SideEffect {
                color.value = Color.Yellow
            }
        }
    }

    @Test
    fun colorChangingState_changesColor_annotatedString() {
        paparazzi.snapshot {
            val color = remember { mutableStateOf(Color.Red) }
            BasicText(
                AnnotatedString("ABCD"),
                color = { color.value }
            )
            SideEffect {
                color.value = Color.Yellow
            }
        }
    }

    @Test
    fun brushState_changesBrush() {
        paparazzi.snapshot {
            val brush = remember { mutableStateOf(Brush.linearGradient(listOf(Color.Gray))) }
            Column {
                BasicText(
                    AnnotatedString("Annotated"),
                    brush = { brush.value }
                )
                BasicText(
                    "String",
                    brush = { brush.value }
                )
            }
            SideEffect {
                brush.value = Brush.sweepGradient(listOf(Color.Gray, Color.Green, Color.Gray))
            }
        }
    }

    @Test
    fun overflowEllipsis_doesEllipsis_whenInPreferredWrapContent() {
        paparazzi.snapshot {
            // b/275369323
            ConstraintLayout(modifier = Modifier
                .width(100.dp)
                .background(Color.White)
                .padding(8.dp)) {
                val (thumbnail, textBox, actionButton) = createRefs()
                Box(modifier = Modifier
                    .background(Color.Green)
                    .constrainAs(thumbnail) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        bottom.linkTo(parent.bottom)
                    }
                    .size(28.dp)
                )
                // Text region
                Column(
                    modifier = Modifier
                        .constrainAs(textBox) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(thumbnail.end)
                            end.linkTo(actionButton.start)
                            width = Dimension.preferredWrapContent
                        },
                ) {
                    BasicText(
                        text = "ASome very long text that is sure to clip in this layout",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(modifier = Modifier
                    .constrainAs(actionButton) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end)
                    }
                    .background(Color.Blue.copy(alpha = 0.5f))
                    .size(28.dp)
                )
            }
        }
    }

    @Test
    fun RtlAppliedCorrectly_inConstraintLayout_withWrapContentText() {
        // b/275369323
        paparazzi.snapshot {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                ConstraintLayout(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.Green)) {
                    val (title, progressBar, expander) = createRefs()
                    BasicText(
                        text = "Locale-aware Text",
                        modifier = Modifier
                            .constrainAs(title) {
                                top.linkTo(parent.top)
                                start.linkTo(parent.start)
                                end.linkTo(expander.start)
                                width = Dimension.fillToConstraints
                            }
                            .border(2.dp, Color.Red)
                    )
                    Box(
                        modifier = Modifier
                            .constrainAs(progressBar) {
                                top.linkTo(title.bottom)
                                start.linkTo(parent.start)
                                end.linkTo(expander.start)
                                width = Dimension.fillToConstraints
                                height = Dimension.value(10.dp)
                            }
                            .background(Color.Yellow)
                    )
                    // expander image button
                    Box(modifier = Modifier
                        .constrainAs(expander) {
                            top.linkTo(parent.top)
                            start.linkTo(progressBar.end)
                            end.linkTo(parent.end)
                            width = Dimension.value(28.dp)
                            height = Dimension.value(28.dp)
                        }
                        .background(Color.Cyan)
                    )
                }
            }
        }
    }
}