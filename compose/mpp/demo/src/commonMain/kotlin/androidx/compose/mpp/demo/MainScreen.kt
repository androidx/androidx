/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.mpp.demo

import androidx.compose.mpp.demo.bug.BugReproducers
import androidx.compose.mpp.demo.components.Components
import androidx.compose.mpp.demo.textfield.android.AndroidTextFieldSamples
import androidx.compose.mpp.demo.textfield.android.TextBrushDemo

val MainScreen = Screen.Selection(
    "Demo",
    Components,
    BugReproducers,
    Screen.Example("Example1") { Example1() },
    Screen.Example("ImageViewer") { ImageViewer() },
    Screen.Example("TextDirection") { TextDirection() },
    Screen.Example("FontFamilies") { FontFamilies() },
    Screen.Example("LottieAnimation") { LottieAnimation() },
    Screen.Fullscreen("ApplicationLayouts") { ApplicationLayouts(it) },
    Screen.Example("GraphicsLayerSettings") { GraphicsLayerSettings() },
    Screen.Example("Blending") { Blending() },
    Screen.Example("FontRasterization") { FontRasterization() },
    Screen.Example("InteropOrder") { InteropOrder() },
    AndroidTextFieldSamples,
    Screen.Example("Android TextBrushDemo") { TextBrushDemo() },
)
