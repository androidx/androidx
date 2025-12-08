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

package androidx.compose.material3.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// TODO This should not be part of material, but we cannot change it in fork.
//  This is a part of bigger task - we need to remove all Popup/Dialog copies from material
//  See https://youtrack.jetbrains.com/issue/CMP-7224

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun BasicEdgeToEdgeDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    properties: DialogProperties,
    lightStatusBars: Boolean,
    lightNavigationBars: Boolean,
    content: @Composable (PredictiveBackState) -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = properties.dismissOnBackPress,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            usePlatformDefaultWidth = false,
            usePlatformInsets = false,
            useSoftwareKeyboardInset = false,
            scrimColor = Color.Transparent,
            animateTransition = false,
        ),
    ) {
        val predictiveBackState = rememberPredictiveBackState()
        Box(modifier = modifier) {
            content(predictiveBackState)
        }
    }
}
