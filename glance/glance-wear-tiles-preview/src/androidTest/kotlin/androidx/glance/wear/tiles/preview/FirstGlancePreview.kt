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

package androidx.glance.wear.tiles.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

@Composable
fun FirstGlancePreview() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "First Glance widget",
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            style = TextStyle(fontWeight = FontWeight.Bold),
        )
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                text = "Button 1",
                modifier = GlanceModifier.height(48.dp),
                onClick = object : Action { }
            )
            Button(
                text = "Button 2",
                modifier = GlanceModifier.height(48.dp),
                onClick = object : Action { }
            )
        }
    }
}
