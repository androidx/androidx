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

package androidx.compose.mpp.demo.components.text

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.mpp.demo.graphics.SliderSetting
import androidx.compose.mpp.demo.textfield.android.Language
import androidx.compose.mpp.demo.textfield.android.loremIpsum
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextOverflow() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement
            .spacedBy(10.dp)
    ) {
        var overflow by remember { mutableStateOf(TextOverflow.Clip) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = overflow == TextOverflow.Clip,
                onClick = { overflow = TextOverflow.Clip },
            )
            Text(text = "Clip")
            Spacer(modifier = Modifier.width(8.dp))
            RadioButton(
                selected = overflow == TextOverflow.Ellipsis,
                onClick = { overflow = TextOverflow.Ellipsis },
            )
            Text(text = "Ellipsis")
            Spacer(modifier = Modifier.width(8.dp))
            RadioButton(
                selected = overflow == TextOverflow.Visible,
                onClick = { overflow = TextOverflow.Visible },
            )
            Text(text = "Visible")
        }

        var width by remember { mutableStateOf(200f) }
        var height by remember { mutableStateOf(200f) }
        SliderSetting("Width", width, 10f..600f) { width = it }
        SliderSetting("Height", height, 10f..600f) { height = it }

        Text(
            text = loremIpsum(Language.Latin, wordCount = 200),
            modifier = Modifier
                .size(width.dp, height.dp)
                .border(1.dp, Color.Black),
            overflow = overflow,
        )
    }
}
