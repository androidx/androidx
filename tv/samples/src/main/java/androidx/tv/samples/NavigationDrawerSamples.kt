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

package androidx.tv.samples

import androidx.annotation.Sampled
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Sampled
@Composable
fun SampleNavigationDrawer() {
    val navigationRow: @Composable (drawerValue: DrawerValue, color: Color, text: String) -> Unit =
        { drawerValue, color, text ->
            Row(Modifier.padding(10.dp).focusable()) {
                Box(Modifier.size(50.dp).background(color).padding(end = 20.dp))
                AnimatedVisibility(visible = drawerValue == DrawerValue.Open) {
                    Text(
                        text = text,
                        softWrap = false,
                        modifier = Modifier.padding(15.dp).width(50.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

    NavigationDrawer(
        drawerContent = {
            Column(Modifier.background(Color.Gray).fillMaxHeight()) {
                navigationRow(it, Color.Red, "Red")
                navigationRow(it, Color.Blue, "Blue")
                navigationRow(it, Color.Yellow, "Yellow")
            }
        }
    ) {
        Button(modifier = Modifier
            .height(100.dp)
            .fillMaxWidth(), onClick = {}) {
            Text("BUTTON")
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Sampled
@Composable
fun SampleModalNavigationDrawer() {
    val navigationRow: @Composable (drawerValue: DrawerValue, color: Color, text: String) -> Unit =
        { drawerValue, color, text ->
            Row(Modifier.padding(10.dp).focusable()) {
                Box(Modifier.size(50.dp).background(color).padding(end = 20.dp))
                AnimatedVisibility(visible = drawerValue == DrawerValue.Open) {
                    Text(
                        text = text,
                        softWrap = false,
                        modifier = Modifier.padding(15.dp).width(50.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

    ModalNavigationDrawer(
        drawerContent = {
            Column(Modifier.background(Color.Gray).fillMaxHeight()) {
                navigationRow(it, Color.Red, "Red")
                navigationRow(it, Color.Blue, "Blue")
                navigationRow(it, Color.Yellow, "Yellow")
            }
        }
    ) {
        Button(modifier = Modifier
            .height(100.dp)
            .fillMaxWidth(), onClick = {}) {
            Text("BUTTON")
        }
    }
}
