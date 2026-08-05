/*
 * Copyright 2026 The Android Open Source Project
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
@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos.examples

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.integration.view.demos.widgets.MyWidget
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun DemoHomeScreen() {
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize().padding(start = 16.rdp),
        contentAlignment = RemoteAlignment.CenterStart,
    ) {
        RemoteColumn {
            RemoteText(text = RemoteString("--- CLOCK WIDGET ---"))
            RemoteText(text = RemoteString("10:42 AM"))
            RemoteText(text = RemoteString("Connected • 85%"))

            RemoteText(text = RemoteString("--- CALENDAR WIDGET ---"))
            RemoteText(text = RemoteString("Monday, July 27"))
            RemoteText(text = RemoteString("Next: Standup at 11:00 AM"))
        }
    }
}

@Composable
fun DemoRon() {
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize().padding(bottom = 150.rdp),
        contentAlignment = RemoteAlignment.BottomCenter,
    ) {
        RemoteBox(modifier = RemoteModifier.background(RemoteColor(Color.DarkGray))) {
            RemoteColumn(modifier = RemoteModifier.padding(8.rdp)) {
                RemoteText(text = RemoteString("=== MEDIA RON ==="))

                RemoteRow {
                    RemoteText(text = RemoteString("NOW PLAYING: "))
                    RemoteText(text = RemoteString("test_music.wav"))
                }

                RemoteRow {
                    RemoteText(text = RemoteString("Artist: "))
                    RemoteText(text = RemoteString("Unknown"))
                }
            }
        }
    }
}

@Composable
fun MultiDocumentDemo() {
    val context = LocalContext.current

    UberDocumentCompositor(
        // LAYER 0: Background Clock
        RemoteLayer(name = "homescreen", visibility = 1) { RcSimpleClock1() },

        // LAYER 1: AI Agent
        RemoteLayer(name = "ai_agent", visibility = 1) {
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().padding(top = 10.rdp),
                contentAlignment = RemoteAlignment.TopCenter,
            ) {
                RemoteBox(modifier = RemoteModifier.height(250.rdp)) { AiAgent() }
            }
        },

        // LAYER 2: The Interactive Widget (Counter)
        RemoteLayer(name = "counter", visibility = 1) {
            RemoteBox(
                modifier = RemoteModifier.fillMaxSize().padding(bottom = 10.rdp),
                contentAlignment = RemoteAlignment.BottomCenter,
            ) {
                RemoteBox(modifier = RemoteModifier.height(120.rdp)) {
                    MyWidget().Content(context = context, widgetId = 0)
                }
            }
        },

        // LAYER 3: DemoRon
        RemoteLayer(name = "DemoRon", visibility = 1) { DemoRon() },

        // LAYER 4: DemoHomeScreen text blocks
        RemoteLayer(name = "DemoHomeScreen", visibility = 0) { DemoHomeScreen() },
    )
}
