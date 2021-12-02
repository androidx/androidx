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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TitleCard

@Sampled
@Composable
fun AppCardWithIcon() {
    AppCard(
        onClick = {},
        appName = { Text("AppName") },
        appImage = {
            Icon(
                painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                contentDescription = "airplane",
                modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
            )
        },
        title = { Text("AppCard") },
        time = { Text("now") },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Some body content")
            Text("and some more body content")
        }
    }
}

@Sampled
@Composable
fun TitleCardStandard() {
    TitleCard(
        onClick = {},
        title = { Text("TitleCard") },
        time = { Text("now") },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Some body content")
            Text("and some more body content")
        }
    }
}

@Sampled
@Composable
fun TitleCardWithImage() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("TitleCard With an ImageBackground") },
        backgroundPainter = CardDefaults.imageWithScrimBackgroundPainter(
            backgroundImagePainter = painterResource(id = R.drawable.backgroundimage)
        ),
        contentColor = MaterialTheme.colors.onSurface,
        titleColor = MaterialTheme.colors.onSurface,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Text coloured to stand out on the image")
        }
    }
}
