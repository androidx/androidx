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

package androidx.compose.mpp.demo.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.mpp.demo.LottieAnimation
import androidx.compose.mpp.demo.Screen
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Dialogs = Screen.Selection(
    "Dialogs",
    DialogExample,
    Screen.Example("CompositionLocal inside Dialog") { DialogCompositionLocalExample() },
    DialogWithTextField,
    FocusAndKeyInput,
    Screen.Dialog("Dialog destination") {
        // Just example of content that should be already inside dialog
        Box(Modifier
            .size(300.dp)
            .shadow(10.dp, CircleShape)
            .background(Color.White)
        ) {
            LottieAnimation()
        }
    }
)
