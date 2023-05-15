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

@file:OptIn(ExperimentalTvMaterial3Api::class)

package androidx.tv.samples

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
import androidx.tv.material3.CardLayoutDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.StandardCardLayout
import androidx.tv.material3.Text
import androidx.tv.material3.WideCardLayout

@Sampled
@Composable
fun StandardCardLayoutSample() {
    StandardCardLayout(
        modifier = Modifier.size(150.dp, 120.dp),
        imageCard = { interactionSource ->
            CardLayoutDefaults.ImageCard(
                onClick = { },
                interactionSource = interactionSource
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color.Blue)
                )
            }
        },
        title = { Text("Standard Card") }
    )
}

@Sampled
@Composable
fun WideCardLayoutSample() {
    WideCardLayout(
        modifier = Modifier.size(180.dp, 100.dp),
        imageCard = { interactionSource ->
            CardLayoutDefaults.ImageCard(
                onClick = { },
                interactionSource = interactionSource
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(Color.Blue)
                )
            }
        },
        title = { Text("Wide Card", Modifier.padding(start = 8.dp)) },
    )
}
