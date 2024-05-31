/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.tv.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.StandardCardContainer
import androidx.tv.material3.Text
import androidx.tv.material3.WideCardContainer

@Sampled
@Composable
fun StandardCardContainerSample() {
    StandardCardContainer(
        modifier = Modifier.size(150.dp, 120.dp),
        imageCard = { interactionSource ->
            Card(onClick = {}, interactionSource = interactionSource) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(Color.Blue))
            }
        },
        title = { Text("Standard Card") }
    )
}

@Sampled
@Composable
fun WideCardContainerSample() {
    WideCardContainer(
        modifier = Modifier.size(180.dp, 100.dp),
        imageCard = { interactionSource ->
            Card(onClick = {}, interactionSource = interactionSource) {
                Box(modifier = Modifier.fillMaxWidth().height(90.dp).background(Color.Blue))
            }
        },
        title = { Text("Wide Card", Modifier.padding(start = 8.dp)) },
    )
}
