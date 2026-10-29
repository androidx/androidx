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

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3XrApi::class)

package androidx.xr.compose.material3.integration.testapp.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.xr.compose.material3.ExperimentalMaterial3XrApi
import androidx.xr.compose.material3.SpatialBasicAlertDialog
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.SpatialDialogProperties

/**
 * A basic alert dialog that uses [SpatialBasicAlertDialog] when spatial UI is enabled, and
 * [androidx.compose.material3.BasicAlertDialog] otherwise.
 *
 * When spatial UI is enabled, the relevant fields of [properties] are mapped to
 * [SpatialDialogProperties].
 */
@Composable
internal fun BasicAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        SpatialBasicAlertDialog(
            onDismissRequest = onDismissRequest,
            properties =
                SpatialDialogProperties(
                    dismissOnBackPress = properties.dismissOnBackPress,
                    dismissOnClickOutside = properties.dismissOnClickOutside,
                    usePlatformDefaultWidth = properties.usePlatformDefaultWidth,
                ),
        ) {
            Box(modifier) { content() }
        }
    } else {
        androidx.compose.material3.BasicAlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            properties = properties,
            content = content,
        )
    }
}
