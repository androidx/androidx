/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.mpp.demo.bugs

import androidx.compose.mpp.demo.Screen

val BugsScreen = Screen.Selection("Web Bug Reproducers", screens = listOf(
    Screen.Example("Text Fields in Scrollable") {
        TextFieldsInTallScrollableContainer()
    },
    Screen.Example("Touch events") {
        // https://youtrack.jetbrains.com/issue/CMP-9030/wasm-canvas-incorrect-handling-of-multi-touch-input
        PointerInputDebugOverlay()
    },
    Screen.Example("MagicMouse") {
        MagicMouse()
    }
))

