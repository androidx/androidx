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

package androidx.wear.compose.material.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.SwipeDismissTarget
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.rememberSwipeToDismissBoxState

/**
 * [Dialog] displays a full-screen dialog, layered over any other content. It takes a single slot,
 * which is expected to be an opinionated Wear dialog content, such as [Alert]
 * or [Confirmation].
 *
 * The dialog supports swipe-to-dismiss and reveals the parent content in the background
 * during the swipe gesture.
 *
 * Example of content using [Dialog] to trigger an alert dialog using [Alert]:
 * @sample androidx.wear.compose.material.samples.AlertDialogSample
 *
 * Example of content using [Dialog] to trigger a confirmation dialog using
 * [Confirmation]:
 * @sample androidx.wear.compose.material.samples.ConfirmationDialogSample

 * @param onDismissRequest Executes when the user dismisses the dialog.
 * Must remove the dialog from the composition.
 * @param properties Typically platform specific properties to further configure the dialog.
 * @param content Slot for dialog content such as [Alert] or [Confirmation].
 */
@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        val swipeState = rememberSwipeToDismissBoxState()
        LaunchedEffect(swipeState.currentValue) {
            if (swipeState.currentValue == SwipeDismissTarget.Dismissal) {
                onDismissRequest()
            }
        }
        SwipeToDismissBox(
            state = swipeState,
        ) { isBackground ->
            if (!isBackground) content()
        }
    }
}
