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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ModalNavigationDrawer

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SampleModalDrawer() {
    Row(Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .height(400.dp)
            .width(400.dp)
            .border(2.dp, Color.Magenta)) {
            ModalNavigationDrawer(drawerContent = drawerContent()) {
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
                ModalNavigationDrawer(drawerContent = drawerContent()) {
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

@Composable
@OptIn(ExperimentalTvMaterial3Api::class)
internal fun drawerContent(): @Composable (DrawerValue) -> Unit =
    {
        Column(Modifier.background(Color.Gray).fillMaxHeight()) {
            NavigationRow(it, Color.Red, "Red")
            NavigationRow(it, Color.Blue, "Blue")
            NavigationRow(it, Color.Yellow, "Yellow")
        }
    }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavigationRow(drawerValue: DrawerValue, color: Color, text: String) {
    Row(Modifier.padding(10.dp).drawBorderOnFocus(width = 2.dp).focusable()) {
        Box(Modifier.size(50.dp).background(color).padding(end = 20.dp))
        AnimatedVisibility(
            drawerValue == DrawerValue.Open,
            // intentionally slow to test animation
            exit = shrinkHorizontally(tween(2000))
        ) {
            Text(
                text = text,
                softWrap = false,
                modifier = Modifier.padding(15.dp).width(50.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
