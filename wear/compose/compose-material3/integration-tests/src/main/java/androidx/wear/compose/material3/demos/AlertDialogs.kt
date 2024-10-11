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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.AlertDialogWithConfirmAndDismissSample
import androidx.wear.compose.material3.samples.AlertDialogWithContentGroupsSample
import androidx.wear.compose.material3.samples.AlertDialogWithEdgeButtonSample

val AlertDialogs =
    listOf(
        ComposableDemo("Edge button") { AlertDialogWithEdgeButtonSample() },
        ComposableDemo("Confirm and Dismiss") { AlertDialogWithConfirmAndDismissSample() },
        ComposableDemo("Content groups") { AlertDialogWithContentGroupsSample() },
        ComposableDemo("Button stack") { AlertDialogWithButtonStack() },
        ComposableDemo("AlertDialog builder") { AlertDialogBuilder() },
    )

@Composable
fun AlertDialogBuilder() {
    val scrollState = rememberScalingLazyListState()

    var showIcon by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf(false) }
    var showSecondaryButton by remember { mutableStateOf(false) }
    var showCaption by remember { mutableStateOf(false) }
    var buttonsType by remember { mutableStateOf(AlertButtonsType.EDGE_BUTTON) }

    var showDialog by remember { mutableStateOf(false) }

    ScreenScaffold(
        scrollState = scrollState,
    ) {
        ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = scrollState) {
            item { ListHeader { Text("AlertDialog") } }
            item {
                SwitchButton(
                    modifier = Modifier.fillMaxWidth(),
                    checked = showIcon,
                    onCheckedChange = { showIcon = it },
                    label = { Text("Icon") },
                )
            }
            item {
                SwitchButton(
                    modifier = Modifier.fillMaxWidth(),
                    checked = true,
                    enabled = false,
                    onCheckedChange = {},
                    label = { Text("Title") },
                )
            }
            item {
                SwitchButton(
                    modifier = Modifier.fillMaxWidth(),
                    checked = showMessage,
                    onCheckedChange = { showMessage = it },
                    label = { Text("Message") },
                )
            }
            item {
                SwitchButton(
                    modifier = Modifier.fillMaxWidth(),
                    checked = showSecondaryButton,
                    onCheckedChange = { showSecondaryButton = it },
                    label = { Text("Secondary button") },
                )
            }
            item {
                SwitchButton(
                    modifier = Modifier.fillMaxWidth(),
                    checked = showCaption,
                    onCheckedChange = { showCaption = it },
                    label = { Text("Caption") },
                )
            }
            item { ListHeader { Text("Buttons") } }
            item {
                RadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    selected = buttonsType == AlertButtonsType.EDGE_BUTTON,
                    onSelect = { buttonsType = AlertButtonsType.EDGE_BUTTON },
                    label = { Text("Single EdgeButton") },
                )
            }
            item {
                RadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    selected = buttonsType == AlertButtonsType.CONFIRM_DISMISS,
                    onSelect = { buttonsType = AlertButtonsType.CONFIRM_DISMISS },
                    label = { Text("Ok/Cancel buttons") },
                )
            }
            item {
                RadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    selected = buttonsType == AlertButtonsType.NO_BUTTONS,
                    onSelect = { buttonsType = AlertButtonsType.NO_BUTTONS },
                    label = { Text("No EdgeButton") },
                )
            }
            item { Button(onClick = { showDialog = true }, label = { Text("Show dialog") }) }
        }
    }

    CustomAlertDialog(
        show = showDialog,
        showIcon = showIcon,
        showCaption = showCaption,
        showSecondaryButton = showSecondaryButton,
        showMessage = showMessage,
        buttonsType = buttonsType,
        onConfirmButton = { showDialog = false },
        onDismissRequest = { showDialog = false }
    )
}

@Composable
fun AlertDialogWithButtonStack() {
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showDialog = true },
            label = { Text("Show Dialog") }
        )
    }

    AlertDialog(
        show = showDialog,
        onDismissRequest = { showDialog = false },
        icon = {
            Icon(
                Icons.Rounded.AccountCircle,
                modifier = Modifier.size(32.dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Allow access to your photos?") },
        text = { Text("Lerp ipsum dolor sit amet.") }
    ) {
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                label = { Text("While using app") },
                icon = { Icon(Icons.Filled.Check, "Check") }
            )
        }
        item {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                label = { Text("Ask every time") },
                icon = { Icon(Icons.Filled.Check, "Check") }
            )
        }
        item {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {},
                label = { Text("Don't allow") },
                icon = { Icon(Icons.Filled.Check, "Check") }
            )
        }
    }
}

@Composable
private fun CustomAlertDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    showIcon: Boolean,
    onConfirmButton: () -> Unit,
    buttonsType: AlertButtonsType,
    showMessage: Boolean,
    showSecondaryButton: Boolean,
    showCaption: Boolean,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val captionHorizontalPadding = screenWidth.dp * 0.0416f

    AlertDialogHelper(
        show = show,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        title = { Text("Mobile network is not currently available") },
        icon =
            if (showIcon) {
                { ExclamationMark() }
            } else null,
        message =
            if (showMessage) {
                { Message() }
            } else null,
        onConfirmButton =
            if (buttonsType == AlertButtonsType.CONFIRM_DISMISS) {
                onConfirmButton
            } else null,
        onDismissButton =
            if (buttonsType == AlertButtonsType.CONFIRM_DISMISS) {
                { /* dismiss action */ }
            } else null,
        onEdgeButton =
            if (buttonsType == AlertButtonsType.EDGE_BUTTON) {
                onConfirmButton
            } else null,
        content =
            if (showSecondaryButton || showCaption) {
                {
                    if (showSecondaryButton) {
                        item { SecondaryButton() }
                    }
                    if (showCaption) {
                        item { Caption(captionHorizontalPadding) }
                        if (buttonsType == AlertButtonsType.EDGE_BUTTON) {
                            item { AlertDialogDefaults.GroupSeparator() }
                        }
                    }
                }
            } else null
    )
}

@Composable
internal fun ExclamationMark() {
    Box(
        modifier =
            Modifier.size(32.dp)
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
    ) {
        Text(
            "!",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun Message() {
    Text("Your battery is low. Turn on battery saver.")
}

@Composable
private fun SecondaryButton() {
    SwitchButton(
        modifier = Modifier.fillMaxWidth(),
        checked = false,
        enabled = true,
        onCheckedChange = {},
        label = { Text("Don't show again") },
    )
}

@Composable
private fun Caption(horizontalPadding: Dp) =
    Text(
        modifier = Modifier.padding(horizontal = horizontalPadding),
        text = "Caption enim ad minim, quis eu veniam vel aru fermentum eu tristique",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
    )

@Composable
private fun AlertDialogHelper(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    properties: DialogProperties,
    title: @Composable () -> Unit,
    icon: @Composable (() -> Unit)?,
    message: @Composable (() -> Unit)?,
    onDismissButton: (() -> Unit)?,
    onConfirmButton: (() -> Unit)?,
    onEdgeButton: (() -> Unit)?,
    content: (ScalingLazyListScope.() -> Unit)?
) {
    if (onConfirmButton != null && onDismissButton != null) {
        AlertDialog(
            show = show,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            properties = properties,
            title = title,
            icon = icon,
            text = message,
            confirmButton = { AlertDialogDefaults.ConfirmButton(onConfirmButton) },
            dismissButton = { AlertDialogDefaults.DismissButton(onDismissButton) },
            content = content
        )
    } else if (onEdgeButton != null) {
        AlertDialog(
            show = show,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            properties = properties,
            title = title,
            icon = icon,
            text = message,
            edgeButton = { AlertDialogDefaults.EdgeButton(onEdgeButton) },
            content = content
        )
    } else {
        AlertDialog(
            show = show,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            properties = properties,
            title = title,
            icon = icon,
            text = message,
            content = content
        )
    }
}

private enum class AlertButtonsType {
    NO_BUTTONS,
    EDGE_BUTTON,
    CONFIRM_DISMISS
}
