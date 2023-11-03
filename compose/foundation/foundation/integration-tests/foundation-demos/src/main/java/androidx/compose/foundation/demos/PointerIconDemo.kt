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

package androidx.compose.foundation.demos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val PointerIconDemos = listOf(
    ComposableDemo("Pointer Icon Partial Overlap") {
        PointerIconPartialOverlapDemo()
    },
    ComposableDemo("Pointer Icon Full Overlap") { PointerIconFullOverlapDemo() },
    ComposableDemo("Pointer Icon Non Overlapping Parents") {
        PointerIconNonOverlappingParentsDemo()
    },
    ComposableDemo("Pointer Icon Overlapping Siblings") {
        PointerIconOverlappingSiblingsDemo()
    },
    ComposableDemo("Pointer Icon Multi-Layered Nesting") {
        PointerIconMultiLayeredNestingDemo()
    },
    ComposableDemo("Pointer Icon Child Doesn't Fully Overlap Parent") {
        PointerIconChildNotFullyOverlappedByParentDemo()
    },
)

@Preview
@Composable
fun PointerIconPartialOverlapDemo() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
            .pointerHoverIcon(PointerIcon.Crosshair)
    ) {
        Text(text = "expected crosshair")
        Box(
            Modifier
                .padding(20.dp)
                .fillMaxWidth(0.6f)
                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                .pointerHoverIcon(PointerIcon.Hand, true)
        ) {
            Text(text = "expected hand")
        }
    }
}

@Preview
@Composable
fun PointerIconFullOverlapDemo() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
            .pointerHoverIcon(PointerIcon.Crosshair)
    ) {
        Text(text = "expected crosshair")
        Box(
            Modifier
                .fillMaxSize()
                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                .pointerHoverIcon(PointerIcon.Hand)
        ) {
            Text(text = "expected hand")
        }
    }
}

@Preview
@Composable
fun PointerIconNonOverlappingParentsDemo() {
    Box(
        modifier = Modifier
            .requiredSize(200.dp)
            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
    ) {
        Column {
            Text("default arrow")
            Box(
                Modifier
                    .padding(20.dp)
                    .requiredSize(50.dp)
                    .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text("hand")
            }
            Box(
                Modifier
                    .padding(40.dp)
                    .requiredSize(50.dp)
                    .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                    .pointerHoverIcon(PointerIcon.Crosshair)
            ) {
                Text("crosshair")
            }
        }
    }
}

@Preview
@Composable
fun PointerIconOverlappingSiblingsDemo() {
    Box(
        modifier = Modifier
            .requiredSize(200.dp)
            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
    ) {
        Text(text = "expected default arrow")
        Box(
            Modifier
                .padding(20.dp)
                .requiredSize(120.dp, 60.dp)
                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                .pointerHoverIcon(PointerIcon.Hand)
        ) {
            Text(text = "expected hand")
        }
        Box(
            Modifier
                .padding(horizontal = 100.dp, vertical = 40.dp)
                .requiredSize(120.dp, 20.dp)
                .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                .pointerHoverIcon(PointerIcon.Crosshair)
        ) {
            Text(text = "expected crosshair")
        }
    }
}

@Preview
@Composable
fun PointerIconMultiLayeredNestingDemo() {
    Box(
        modifier = Modifier
            .requiredSize(200.dp)
            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
            .pointerHoverIcon(PointerIcon.Crosshair)
    ) {
        Text(text = "expected crosshair")
        Box(
            Modifier
                .padding(20.dp)
                .requiredSize(150.dp)
                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                .pointerHoverIcon(PointerIcon.Text)
        ) {
            Text(text = "expected text")
            Box(
                Modifier
                    .padding(40.dp)
                    .requiredSize(100.dp)
                    .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(text = "expected hand")
            }
        }
    }
}

@Preview
@Composable
fun PointerIconChildNotFullyOverlappedByParentDemo() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(BorderStroke(2.dp, SolidColor(Color.Yellow)))
    ) {
        Text(text = "expected default arrow")
        Box(
            modifier = Modifier
                .padding(vertical = 20.dp)
                .requiredSize(width = 200.dp, height = 150.dp)
                .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                .pointerHoverIcon(PointerIcon.Crosshair, overrideDescendants = false)
        ) {
            Text(text = "expected crosshair")
            Box(
                Modifier
                    .padding(vertical = 40.dp)
                    .requiredSize(width = 150.dp, height = 125.dp)
                    .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                    .pointerHoverIcon(PointerIcon.Text, overrideDescendants = false)
            ) {
                Text(text = "expected text")
                Box(
                    Modifier
                        .padding(vertical = 80.dp)
                        .requiredSize(width = 300.dp, height = 100.dp)
                        .offset(x = 100.dp)
                        .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                        .pointerHoverIcon(PointerIcon.Hand, overrideDescendants = false)
                ) {
                    Text(text = "expected hand")
                }
            }
        }
    }
}
