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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TitleCard
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.ToggleButtonDefaults
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSquare

@WearPreviewSmallRound
@WearPreviewLargeRound
@WearPreviewSquare
@Sampled
@Composable
fun ButtonWithIconPreview() {
    Button(
        onClick = { /* Do something */ },
        enabled = true,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
            contentDescription = "airplane",
            modifier = Modifier
                .size(ButtonDefaults.DefaultIconSize).wrapContentSize(align = Alignment.Center),
        )
    }
}

@WearPreviewFontScales
@Sampled
@Composable
fun TitleCardWithImagePreview() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("TitleCard With an ImageBackground") },
        backgroundPainter = CardDefaults.imageWithScrimBackgroundPainter(
            backgroundImagePainter = painterResource(id = R.drawable.backgroundimage)
        ),
        contentColor = MaterialTheme.colors.onSurface,
        titleColor = MaterialTheme.colors.onSurface,
    ) {
        Text("Text coloured to stand out on the image")
    }
}

@WearPreviewDevices
@Sampled
@Composable
fun ToggleButtonWithIconPreview() {
    var checked by remember { mutableStateOf(true) }
    ToggleButton(
        checked = checked,
        onCheckedChange = { checked = it },
        enabled = true,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
            contentDescription = "airplane",
            modifier = Modifier
                .size(ToggleButtonDefaults.DefaultIconSize)
                .wrapContentSize(align = Alignment.Center),
        )
    }
}
