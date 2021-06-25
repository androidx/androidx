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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TitleCard

@Composable
fun CardDemo() {
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(start = 8.dp, end = 8.dp)
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.size(30.dp))
        Card(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Basic unopinionated chip")
                Text("Sets the shape")
                Text("and the background")
            }
        }
        Spacer(modifier = Modifier.size(4.dp))
        AppCard(
            onClick = {},
            appName = { Text("AppName") },
            title = { Text("AppCard") },
            time = { Text("now") },
            body = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Some body content")
                    Text("and some more body content")
                }
            },
        )
        Spacer(modifier = Modifier.size(4.dp))
        TitleCard(
            onClick = {},
            title = { Text("TitleCard") },
            time = { Text("now") },
            body = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Some body content")
                    Text("and some more body content")
                }
            },
        )
        Spacer(modifier = Modifier.size(4.dp))
        TitleCard(
            onClick = {},
            title = { Text("TitleCard") },
            body = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("This title card doesn't show time")
                }
            }
        )
        Spacer(modifier = Modifier.size(4.dp))
        TitleCard(
            onClick = {},
            title = { Text("Custom TitleCard") },
            body = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("This title card emphasises the title with custom color")
                }
            },
            titleColor = Color.Yellow
        )
        Spacer(modifier = Modifier.size(4.dp))
        TitleCard(
            onClick = {},
            title = {
                Column {
                    Text("Custom TitleCard")
                    Text("With a Coloured Secondary Label", color = Color.Yellow)
                }
            },
            body = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("This title card emphasises the title with custom color")
                }
            },
        )
        Spacer(modifier = Modifier.size(4.dp))
        TitleCard(
            onClick = {},
            title = { Text("TitleCard With an ImageBackground") },
            body = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Text coloured to stand out on the image")
                }
            },
            backgroundPainter = CardDefaults.imageBackgroundPainter(
                backgroundImagePainter = painterResource(id = R.drawable.backgroundimage1)
            ),
            bodyColor = MaterialTheme.colors.onSurface,
            titleColor = MaterialTheme.colors.onSurface,
        )
        Spacer(modifier = Modifier.size(30.dp))
    }
}