/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.layout.demos

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.unit.dp
import androidx.ui.unit.sp

@Composable
fun SimpleLayoutDemo() {
    val lightGrey = Color(0xFFCFD8DC)
    Column {
        Text(text = "Row", fontSize = 48.sp)

        Stack(Modifier.preferredWidth(ExampleSize).drawBackground(color = lightGrey)) {
            Row(Modifier.fillMaxWidth()) {
                PurpleSquare()
                CyanSquare()
            }
        }
        Spacer(Modifier.preferredHeight(24.dp))
        Stack(Modifier.preferredWidth(ExampleSize).drawBackground(color = lightGrey)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                PurpleSquare()
                CyanSquare()
            }
        }
        Spacer(Modifier.preferredHeight(24.dp))
        Stack(Modifier.preferredWidth(ExampleSize).drawBackground(color = lightGrey)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PurpleSquare()
                CyanSquare()
            }
        }
        Spacer(Modifier.preferredHeight(24.dp))
        Stack(Modifier.preferredWidth(ExampleSize).drawBackground(color = lightGrey)) {
            Row(Modifier.fillMaxWidth()) {
                PurpleSquare()
                CyanSquare()
            }
        }
        Spacer(Modifier.preferredHeight(24.dp))
        Stack(Modifier.preferredWidth(ExampleSize).drawBackground(color = lightGrey)) {
            Row(Modifier.fillMaxWidth()) {
                PurpleSquare(Modifier.gravity(Alignment.Bottom))
                CyanSquare(Modifier.gravity(Alignment.Bottom))
            }
        }
        Spacer(Modifier.preferredHeight(24.dp))
        Text(text = "Column", fontSize = 48.sp)
        Row(Modifier.fillMaxWidth()) {
            Stack(Modifier.preferredHeight(ExampleSize).drawBackground(color = lightGrey)) {
                Column(Modifier.fillMaxHeight()) {
                    PurpleSquare()
                    CyanSquare()
                }
            }
            Spacer(Modifier.preferredWidth(24.dp))
            Stack(Modifier.preferredHeight(ExampleSize).drawBackground(color = lightGrey)) {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    PurpleSquare()
                    CyanSquare()
                }
            }
            Spacer(Modifier.preferredWidth(24.dp))
            Stack(Modifier.preferredHeight(ExampleSize).drawBackground(color = lightGrey)) {
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                    PurpleSquare()
                    CyanSquare()
                }
            }
            Spacer(Modifier.preferredWidth(24.dp))
            Stack(Modifier.preferredHeight(ExampleSize).drawBackground(color = lightGrey)) {
                Column(Modifier.fillMaxHeight()) {
                    PurpleSquare()
                    CyanSquare()
                }
            }
            Spacer(Modifier.preferredWidth(24.dp))
            Stack(Modifier.preferredHeight(ExampleSize).drawBackground(color = lightGrey)) {
                Column(Modifier.fillMaxHeight()) {
                    PurpleSquare(Modifier.gravity(Alignment.End))
                    CyanSquare(Modifier.gravity(Alignment.End))
                }
            }
        }
    }
}

@Composable
private fun PurpleSquare(modifier: Modifier = Modifier) {
    Box(modifier.preferredSize(48.dp), backgroundColor = Color(0xFF6200EE))
}

@Composable
private fun CyanSquare(modifier: Modifier = Modifier) {
    Box(modifier.preferredSize(24.dp), backgroundColor = Color(0xFF03DAC6))
}

private val ExampleSize = 140.dp