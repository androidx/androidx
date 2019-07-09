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

package androidx.ui.material.demos

import android.app.Activity
import android.os.Bundle
import androidx.compose.Composable
import androidx.compose.setContent
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.layout.Column
import androidx.ui.layout.HeightSpacer
import androidx.ui.material.MaterialTheme
import androidx.ui.material.TopAppBar
import androidx.ui.material.themeTextStyle
import androidx.ui.graphics.Color

class AppBarActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CraneWrapper {
                MaterialTheme {
                    AppBarDemo()
                }
            }
        }
    }
}

@Composable
fun AppBarDemo() {
    Column {
        TopAppBar(title = "Default")
        HeightSpacer(height = 24.dp)
        TopAppBar(title = "Custom color", color = Color(0xFFE91E63.toInt()))
        HeightSpacer(height = 24.dp)
        TopAppBar(title = "Custom icons", icons = listOf(24.dp, 24.dp))
        HeightSpacer(height = 12.dp)
        Text(text = "No title", style = +themeTextStyle { h6 })
        TopAppBar(icons = listOf(24.dp, 24.dp))
        HeightSpacer(height = 24.dp)
        TopAppBar(title = "Too many icons", icons = listOf(24.dp, 24.dp, 24.dp, 24.dp))
    }
}