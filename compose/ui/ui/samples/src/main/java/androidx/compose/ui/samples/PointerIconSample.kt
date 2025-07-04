/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.stylusHoverIcon
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun PointerIconSample() {
    Column(Modifier.pointerHoverIcon(PointerIcon.Crosshair)) {
        SelectionContainer {
            Column {
                Text("Selectable text")
                Text(
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand, true),
                    text = "Selectable text with hand",
                )
            }
        }
        Text("Just text with global pointerIcon")
    }
}

@Sampled
@Composable
fun StylusHoverIconSample() {
    Box(
        Modifier.requiredSize(200.dp)
            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
            .stylusHoverIcon(PointerIcon.Crosshair)
    ) {
        Text(text = "crosshair icon")
        Box(
            Modifier.padding(20.dp)
                .requiredSize(150.dp)
                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
                .stylusHoverIcon(PointerIcon.Text)
        ) {
            Text(text = "text icon")
            Box(
                Modifier.padding(40.dp)
                    .requiredSize(100.dp)
                    .border(BorderStroke(2.dp, SolidColor(Color.Blue)))
                    .stylusHoverIcon(PointerIcon.Hand)
            ) {
                Text(text = "hand icon")
            }
        }
    }
}
