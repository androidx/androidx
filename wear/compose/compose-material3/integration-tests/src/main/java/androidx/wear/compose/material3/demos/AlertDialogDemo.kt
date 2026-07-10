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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
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
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.samples.AlertDialogWithConfirmAndDismissSample
import androidx.wear.compose.material3.samples.AlertDialogWithConfirmAndDismissTransformingContentSample
import androidx.wear.compose.material3.samples.AlertDialogWithContentGroupsSample
import androidx.wear.compose.material3.samples.AlertDialogWithContentGroupsTransformingContentSample
import androidx.wear.compose.material3.samples.AlertDialogWithEdgeButtonSample
import androidx.wear.compose.material3.samples.AlertDialogWithEdgeButtonTransformingContentSample

val AlertDialogDemos =
    listOf(
        ComposableDemo("Edge button (SLC)") { AlertDialogWithEdgeButtonSample() },
        ComposableDemo("Edge button (TLC)") {
            AlertDialogWithEdgeButtonTransformingContentSample()
        },
        ComposableDemo("Confirm and Dismiss (SLC)") { AlertDialogWithConfirmAndDismissSample() },
        ComposableDemo("Confirm and Dismiss (TLC)") {
            AlertDialogWithConfirmAndDismissTransformingContentSample()
        },
        ComposableDemo("Content groups (SLC)") { AlertDialogWithContentGroupsSample() },
        ComposableDemo("Content groups (TLC)") {
            AlertDialogWithContentGroupsTransformingContentSample()
        },
        ComposableDemo("Button stack (SLC)") { AlertDialogWithSLCButtonStack() },
        ComposableDemo("Button stack (TLC)") { AlertDialogWithTLCButtonStack() },
        ComposableDemo("AlertDialog builder") { AlertDialogBuilder() },
    )

@Composable
fun AlertDialogBuilder() {
    val scrollState = rememberScalingLazyListState()

    var showIcon by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(TextSize.SMALL) }
    var showMessage by remember { mutableStateOf<TextSize?>(null) }
    var showSecondaryButton by remember { mutableStateOf(false) }
    var showCaption by remember { mutableStateOf(false) }
    var buttonsType by remember { mutableStateOf(AlertButtonsType.EDGE_BUTTON) }
    var useTLC by remember { mutableStateOf(false) }
    var useSLC by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }

    ScreenScaffold(scrollState = scrollState) {
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
                RadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    selected = showTitle == TextSize.SMALL,
                    onSelect = { showTitle = TextSize.SMALL },
                    label = { Text("Title (sm)") },
                )
            }
            item {
                RadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    selected = showTitle == TextSize.LARGE,
                    onSelect = { showTitle = TextSize.LARGE },
                    label = { Text("Title (lg)") },
                )
            }
            item {
                SwitchButton(
                    modifier = Modifier.fillMaxWidth(),
                    checked = showMessage == TextSize.SMALL,
                    onCheckedChange = { showMessage = if (it) TextSize.SMALL else null },
                    label = { Text("Message (sm)") },
                )
            }
            item {
                SwitchButton(
                    modifier = Modifier.fillMaxWidth(),
                    checked = showMessage == TextSize.LARGE,
                    onCheckedChange = { showMessage = if (it) TextSize.LARGE else null },
                    label = { Text("Message (lg)") },
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
            item { ListHeader { Text("Container") } }
            item {
                RadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    selected = useSLC,
                    onSelect = {
                        useSLC = true
                        useTLC = false
                    },
                    label = { Text("Use SLC") },
                )
            }
            item {
                RadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    selected = useTLC,
                    onSelect = {
                        useTLC = true
                        useSLC = false
                    },
                    label = { Text("Use TLC") },
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { Button(onClick = { showDialog = true }, label = { Text("Show dialog") }) }
        }
    }

    CustomAlertDialog(
        show = showDialog,
        showIcon = showIcon,
        showCaption = showCaption,
        showSecondaryButton = showSecondaryButton,
        showTitle = showTitle,
        showMessage = showMessage,
        buttonsType = buttonsType,
        onConfirmButton = { showDialog = false },
        onDismissRequest = { showDialog = false },
        useSLC = useSLC,
    )
}

@Composable
fun AlertDialogWithSLCButtonStack() {
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showDialog = true },
            label = { Text("Show Dialog") },
        )
    }

    AlertDialog(
        visible = showDialog,
        onDismissRequest = { showDialog = false },
        icon = {
            Icon(
                Icons.Rounded.AccountCircle,
                modifier = Modifier.size(32.dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("Allow access to your photos?") },
        text = { Text("Lerp ipsum dolor sit amet.") },
    ) {
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDialog = false },
                label = { Text("While using app") },
            )
        }
        item {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDialog = false },
                label = { Text("Ask every time") },
            )
        }
        item {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDialog = false },
                label = { Text("Don't allow") },
            )
        }
    }
}

@Composable
fun AlertDialogWithTLCButtonStack() {
    var showDialog by remember { mutableStateOf(false) }
    val transformationSpec = rememberTransformationSpec()

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showDialog = true },
            label = { Text("Show Dialog") },
        )
    }

    AlertDialog(
        visible = showDialog,
        onDismissRequest = { showDialog = false },
        icon = {
            Icon(
                Icons.Rounded.AccountCircle,
                modifier = Modifier.size(32.dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("Allow access to your photos?") },
        text = { Text("Lerp ipsum dolor sit amet.") },
        transformationSpec = transformationSpec,
    ) {
        item {
            Button(
                modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
                onClick = { showDialog = false },
                label = { Text("While using app") },
            )
        }
        item {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
                onClick = { showDialog = false },
                label = { Text("Ask every time") },
            )
        }
        item {
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
                onClick = { showDialog = false },
                label = { Text("Don't allow") },
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
    showTitle: TextSize,
    showIcon: Boolean,
    showMessage: TextSize?,
    onConfirmButton: () -> Unit,
    buttonsType: AlertButtonsType,
    showSecondaryButton: Boolean,
    showCaption: Boolean,
    useSLC: Boolean,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val captionHorizontalPadding = screenWidth.dp * 0.0416f

    val slcContent: (ScalingLazyListScope.() -> Unit)? =
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

    val tlcContent: (TransformingLazyColumnScope.() -> Unit)? =
        if (showSecondaryButton || showCaption) {
            {
                if (showSecondaryButton) {
                    item { SecondaryButton(transformation = rememberTransformationSpec()) }
                }
                if (showCaption) {
                    item { Caption(captionHorizontalPadding) }
                    if (buttonsType == AlertButtonsType.EDGE_BUTTON) {
                        item { AlertDialogDefaults.GroupSeparator() }
                    }
                }
            }
        } else null

    AlertDialogHelper(
        show = show,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        title = { Title(showTitle) },
        icon =
            if (showIcon) {
                { ExclamationMark() }
            } else null,
        message =
            if (showMessage != null) {
                { Message(showMessage) }
            } else null,
        onConfirmButton =
            if (buttonsType == AlertButtonsType.CONFIRM_DISMISS) {
                onConfirmButton
            } else null,
        onDismissButton =
            if (buttonsType == AlertButtonsType.CONFIRM_DISMISS) {
                onDismissRequest
            } else null,
        onEdgeButton =
            if (buttonsType == AlertButtonsType.EDGE_BUTTON) {
                onConfirmButton
            } else null,
        contentSLC = if (useSLC) slcContent else null,
        contentTLC = if (!useSLC) tlcContent else null,
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
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun Title(size: TextSize) {
    when (size) {
        TextSize.SMALL -> Text("Mobile network")
        TextSize.LARGE -> Text("Your mobile network is off")
    }
}

@Composable
private fun Message(size: TextSize) {
    when (size) {
        TextSize.SMALL -> Text("Your battery is low. Turn on battery saver.")
        TextSize.LARGE ->
            Text(
                "Your battery is low. Turn on battery saver. Or charge your device. You need a charger for that! Don't forget your at home!"
            )
    }
}

@Composable
private fun SecondaryButton() {
    var checked by remember { mutableStateOf(false) }
    SwitchButton(
        modifier = Modifier.fillMaxWidth(),
        checked = checked,
        enabled = true,
        onCheckedChange = { checked = it },
        label = { Text("Don't show again") },
    )
}

@Composable
private fun TransformingLazyColumnItemScope.SecondaryButton(transformation: TransformationSpec) {
    var checked by remember { mutableStateOf(false) }
    SwitchButton(
        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformation),
        checked = checked,
        enabled = true,
        transformation = SurfaceTransformation(transformation),
        onCheckedChange = { checked = it },
        label = { Text("Don't show again") },
    )
}

@Composable
private fun Caption(horizontalPadding: Dp) =
    Text(
        modifier = Modifier.padding(horizontal = horizontalPadding),
        text = "Caption enim ad minim, quis eu veniam vel",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
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
    contentSLC: (ScalingLazyListScope.() -> Unit)?,
    contentTLC: (TransformingLazyColumnScope.() -> Unit)?,
) {
    val useSLC = contentSLC != null
    if (onConfirmButton != null && onDismissButton != null) {
        if (useSLC) {
            AlertDialog(
                visible = show,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                properties = properties,
                title = title,
                icon = icon,
                text = message,
                confirmButton = { AlertDialogDefaults.ConfirmButton(onConfirmButton) },
                dismissButton = { AlertDialogDefaults.DismissButton(onDismissButton) },
                content = contentSLC,
            )
        } else {
            AlertDialog(
                visible = show,
                transformationSpec = rememberTransformationSpec(),
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                properties = properties,
                title = title,
                icon = icon,
                text = message,
                confirmButton = { AlertDialogDefaults.ConfirmButton(onConfirmButton) },
                dismissButton = { AlertDialogDefaults.DismissButton(onDismissButton) },
                content = contentTLC,
            )
        }
    } else if (onEdgeButton != null) {
        if (useSLC) {
            AlertDialog(
                visible = show,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                properties = properties,
                title = title,
                icon = icon,
                text = message,
                edgeButton = { AlertDialogDefaults.EdgeButton(onEdgeButton) },
                content = contentSLC,
            )
        } else {
            AlertDialog(
                visible = show,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                properties = properties,
                title = title,
                icon = icon,
                text = message,
                edgeButton = { AlertDialogDefaults.EdgeButton(onEdgeButton) },
                content = contentTLC,
                transformationSpec = rememberTransformationSpec(),
            )
        }
    } else {
        if (useSLC) {
            AlertDialog(
                visible = show,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                properties = properties,
                title = title,
                icon = icon,
                text = message,
                content = contentSLC,
            )
        } else {
            AlertDialog(
                visible = show,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                properties = properties,
                title = title,
                icon = icon,
                text = message,
                content = contentTLC,
                transformationSpec = rememberTransformationSpec(),
            )
        }
    }
}

private enum class AlertButtonsType {
    NO_BUTTONS,
    EDGE_BUTTON,
    CONFIRM_DISMISS,
}

private enum class TextSize {
    SMALL,
    LARGE,
}
