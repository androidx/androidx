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

package androidx.compose.ui.platform

import androidx.compose.ui.input.InputMode
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

internal class IOSPlatformContextImpl(
    inputServices: PlatformTextInputService,
    override val textToolbar: TextToolbar,
    override val windowInfo: WindowInfo,
    density: Density,
    override val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener?
) : PlatformContext by PlatformContext.Empty {
    override val textInputService: PlatformTextInputService = inputServices
    override val viewConfiguration = object : ViewConfiguration by EmptyViewConfiguration {
        override val touchSlop: Float
            get() = with(density) {
                // this value is originating from iOS 16 drag behavior reverse engineering
                10.dp.toPx()
            }
    }
    override val inputModeManager = DefaultInputModeManager(InputMode.Touch)
}
