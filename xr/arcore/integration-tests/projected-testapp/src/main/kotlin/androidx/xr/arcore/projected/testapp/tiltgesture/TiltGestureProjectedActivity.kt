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

package androidx.xr.arcore.projected.testapp.tiltgesture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.xr.arcore.ExperimentalGesturesApi
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text

@OptIn(ExperimentalGesturesApi::class)
class TiltGestureProjectedActivity : ComponentActivity() {

    internal lateinit var viewModel: TiltGestureViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProjectedView(viewModel) }
    }

    @Composable
    private fun ProjectedView(viewModel: TiltGestureViewModel) {
        GlimmerTheme {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .background(GlimmerTheme.colors.background)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.Start,
            ) {
                TextEntry(text = "Tilt: ${viewModel.tilt.collectAsState().value}")
                TextEntry(text = "Progress: ${viewModel.progress.collectAsState().value}")
                TextEntry(text = "Info: ${viewModel.message.collectAsState().value}")
            }
        }
    }

    @Composable
    private fun TextEntry(text: String) {
        Text(text = text, style = GlimmerTheme.typography.bodyLarge)
    }
}
