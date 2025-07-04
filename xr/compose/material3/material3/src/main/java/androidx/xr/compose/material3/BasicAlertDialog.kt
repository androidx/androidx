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

package androidx.xr.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.BasicAlertDialogOverride
import androidx.compose.material3.BasicAlertDialogOverrideScope
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.xr.compose.spatial.SpatialDialog
import androidx.xr.compose.spatial.SpatialDialogProperties

/**
 * <a href="https://m3.material.io/components/dialogs/overview" class="external"
 * target="_blank">Basic alert dialog dialog</a>.
 *
 * XR-specific Alert dialog that shows a basic alert dialog in a [SpatialDialog].
 *
 * Dialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task.
 *
 * This basic alert dialog expects an arbitrary content that is defined by the caller. Note that
 * your content will need to define its own styling.
 *
 * @param onDismissRequest called when the user tries to dismiss the Dialog by clicking outside or
 *   pressing the back button. This is not called when the dismiss button is clicked.
 * @param properties typically platform specific properties to further configure the dialog.
 * @param content the content of the dialog
 */
@ExperimentalMaterial3XrApi
@Composable
public fun BasicAlertDialog(
    onDismissRequest: () -> Unit,
    properties: SpatialDialogProperties = SpatialDialogProperties(),
    content: @Composable () -> Unit,
) {
    SpatialDialog(onDismissRequest = onDismissRequest, properties = properties) {
        Box(modifier = Modifier.height(IntrinsicSize.Min)) { content() }
    }
}

/**
 * [BasicAlertDialogOverride] that uses the XR-specific [BasicAlertDialog].
 *
 * Note that when using this override, any modifiers passed in to the 2D composable are ignored.
 */
@ExperimentalMaterial3XrApi
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
internal object XrBasicAlertDialogOverride : BasicAlertDialogOverride {
    @Composable
    override fun BasicAlertDialogOverrideScope.BasicAlertDialog() {
        BasicAlertDialog(
            onDismissRequest = onDismissRequest,
            properties =
                SpatialDialogProperties(
                    dismissOnBackPress = properties.dismissOnBackPress,
                    dismissOnClickOutside = properties.dismissOnClickOutside,
                    usePlatformDefaultWidth = properties.usePlatformDefaultWidth,
                ),
            content = content,
        )
    }
}
