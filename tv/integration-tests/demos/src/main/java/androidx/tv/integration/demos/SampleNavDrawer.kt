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

package androidx.tv.integration.demos

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.NavigationDrawer

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SampleDrawer() {
    Row(Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .height(400.dp)
            .width(400.dp)
            .border(2.dp, Color.Magenta)) {
            NavigationDrawer(drawerContent = drawerContent()) {
                Button(modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth(), onClick = {}) {
                    Text("BUTTON")
                }
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .height(400.dp)
                    .width(400.dp)
                    .border(2.dp, Color.Magenta)
            ) {
                NavigationDrawer(drawerContent = drawerContent()) {
                    Button(modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth(), onClick = {}) {
                        Text("BUTTON")
                    }
                }
            }
        }
    }
}