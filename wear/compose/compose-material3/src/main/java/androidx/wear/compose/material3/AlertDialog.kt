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
package androidx.wear.compose.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick as semanticOnClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialogDefaults.ConfirmIcon
import androidx.wear.compose.material3.AlertDialogDefaults.DismissIcon
import androidx.wear.compose.material3.AlertDialogDefaults.EdgeButton
import androidx.wear.compose.material3.PaddingDefaults.horizontalContentPadding
import androidx.wear.compose.material3.PaddingDefaults.verticalContentPadding
import androidx.wear.compose.material3.internal.Icons
import androidx.wear.compose.material3.internal.Strings
import androidx.wear.compose.material3.internal.getString
import androidx.wear.compose.material3.lazy.ResponsiveTransformationSpec
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.onehandedgesture.GestureAction
import androidx.wear.compose.material3.onehandedgesture.GesturePriority
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureDefaults
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture

// Confirm and dismiss buttons
/**
 * AlertDialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task. The AlertDialog is scrollable by default if the
 * content exceeds the viewport height.
 *
 * This overload has 2 [IconButton]s for confirmation and cancellation, placed horizontally at the
 * bottom of the dialog. It should be used when the user will be presented with a binary decision,
 * to either confirm or dismiss an action.
 *
 * Where user input is not required, such as displaying a transient success or failure message, use
 * [ConfirmationDialog], [SuccessConfirmationDialog] or [FailureConfirmationDialog] instead.
 *
 * AlertDialog now additionally offers an equivalent overload using TransformingLazyColumn, which is
 * recommended to achieve a consistent Material 3 scrolling experience throughout your app
 *
 * Example of an [AlertDialog] with an icon, title and two buttons to confirm and dismiss:
 *
 * @sample androidx.wear.compose.material3.samples.AlertDialogWithConfirmAndDismissSample
 * @param visible A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping
 *   right (typically also called by the [dismissButton]). Implementation of this lambda must remove
 *   the dialog from the composition hierarchy e.g. by setting [visible] to false.
 * @param confirmButton A slot for a [Button] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy e.g. by setting [visible] to false. It's
 *   recommended to use [AlertDialogDefaults.ConfirmButton] in this slot with onClick callback.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param modifier Modifier to be applied to the dialog content.
 * @param dismissButton A slot for a [Button] indicating negative sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy e.g. by setting [visible] to false. It's
 *   recommended to use [AlertDialogDefaults.DismissButton] in this slot with onClick callback.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. It is recommended
 *   to use the defaults, which adjust to reduce the top padding when an icon is present.
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 */
@Composable
public fun AlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable RowScope.() -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable RowScope.() -> Unit = {
        AlertDialogDefaults.DismissButton(onDismissRequest)
    },
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues =
        if (icon != null) {
            AlertDialogDefaults.confirmDismissWithIconContentPadding()
        } else {
            AlertDialogDefaults.confirmDismissContentPadding()
        },
    properties: DialogProperties = DialogProperties(),
    content: (ScalingLazyListScope.() -> Unit)? = null,
) {
    Dialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier =
            Modifier.alertDialogGesture(
                action = GestureAction.Dismiss,
                onGesture = onDismissRequest,
            ),
        properties = properties,
    ) {
        AlertDialogContent(
            confirmButton = confirmButton,
            title = title,
            dismissButton = dismissButton,
            modifier = modifier,
            icon = icon,
            text = text,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

/**
 * AlertDialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task. The AlertDialog is scrollable by default if the
 * content exceeds the viewport height.
 *
 * This overload has 2 [IconButton]s for confirmation and cancellation and it uses a
 * [TransformationSpec] for advanced control over transformations of scrolling content.
 *
 * The confirmation buttons are centered horizontally at the bottom of the dialog. This overload
 * should be used when the user will be presented with a binary decision, to either confirm or
 * dismiss an action. Where user input is not required, such as displaying a transient success or
 * failure message, use [ConfirmationDialog], [SuccessConfirmationDialog] or
 * [FailureConfirmationDialog] instead.
 *
 * The [transformationSpec] parameter enables the use of a [TransformingLazyColumn] internally for
 * advanced control over transformations of scrolling content, which can not be achieved with
 * [ScalingLazyColumn].
 *
 * Example of an [AlertDialog] with an icon, title and two buttons to confirm and dismiss:
 *
 * @sample androidx.wear.compose.material3.samples.AlertDialogWithConfirmAndDismissTransformingContentSample
 * @param visible A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping
 *   right (typically also called by the [dismissButton]). Implementation of this lambda must remove
 *   the dialog from the composition hierarchy e.g. by setting [visible] to false.
 * @param confirmButton A slot for a [Button] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy e.g. by setting [visible] to false. It's
 *   recommended to use [AlertDialogDefaults.ConfirmButton] in this slot with onClick callback.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param transformationSpec A spec that defines how items inside the [TransformingLazyColumn]
 *   transform and animate as they are scrolled. It is recommended to create a spec using
 *   [rememberTransformationSpec]. This parameter is the key differentiator from the standard
 *   [AlertDialog] overload, which uses a [ScalingLazyColumn] without per-item customizable
 *   animations. The provided [transformationSpec] is automatically applied only to the dialog's
 *   standard components (title, text, and buttons), while additional items in [content] lambda need
 *   to be handled manually, by applying a [TransformationSpec] to each of them. First item has a
 *   fixed spec, with a reduced transition area, so that the top item does not appear scaled or
 *   faded when the dialog is initially displayed.
 * @param modifier Modifier to be applied to the dialog content.
 * @param dismissButton A slot for a [Button] indicating negative sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy e.g. by setting [visible] to false. It's
 *   recommended to use [AlertDialogDefaults.DismissButton] in this slot with onClick callback.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. It is recommended
 *   to use the defaults, which adjust to reduce the top padding when an icon is present.
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param content A slot for additional content, displayed within a scrollable
 *   [TransformingLazyColumn]. To ensure that your custom items animate consistently with the rest
 *   of the dialog, we recommend applying a [TransformationSpec] to every item.
 */
@Composable
public fun AlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable RowScope.() -> Unit,
    title: @Composable () -> Unit,
    transformationSpec: TransformationSpec,
    modifier: Modifier = Modifier,
    dismissButton: @Composable RowScope.() -> Unit = {
        AlertDialogDefaults.DismissButton(onDismissRequest)
    },
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues =
        if (icon != null) {
            AlertDialogDefaults.confirmDismissWithIconContentPadding()
        } else {
            AlertDialogDefaults.confirmDismissContentPadding()
        },
    properties: DialogProperties = DialogProperties(),
    content: (TransformingLazyColumnScope.() -> Unit)? = null,
) {
    Dialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier =
            Modifier.alertDialogGesture(
                action = GestureAction.Dismiss,
                onGesture = onDismissRequest,
            ),
        properties = properties,
    ) {
        AlertDialogContent(
            confirmButton = confirmButton,
            title = title,
            dismissButton = dismissButton,
            modifier = modifier,
            icon = icon,
            text = text,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            transformationSpec = transformationSpec,
            content = content,
        )
    }
}

// No buttons
/**
 * Dialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task. The dialog is scrollable by default if the content
 * exceeds the viewport height.
 *
 * This overload doesn't have any dedicated slots for buttons. It has a content slot so that the
 * caller has flexibility in how to seek user input. In most cases, we recommend using other
 * AlertDialog variations with 2 confirm/dismiss buttons or a single confirmation button.
 *
 * Where user input is not required, such as displaying a transient success or failure message, use
 * [ConfirmationDialog], [SuccessConfirmationDialog] or [FailureConfirmationDialog] instead.
 *
 * AlertDialog now additionally offers an equivalent overload using TransformingLazyColumn, which is
 * recommended to achieve a consistent Material 3 scrolling experience throughout your app
 *
 * @param visible A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping to
 *   the right or by other dismiss action. Implementation of this lambda must remove the dialog from
 *   the composition hierarchy e.g. by setting [visible] to false.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. It is recommended
 *   to use the defaults, which adjust to reduce the top padding when an icon is present.
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 *   Any buttons added in this slot that are intended to dismiss the dialog must remove the dialog
 *   from the composition hierarchy e.g. by setting [visible] to false.
 */
@Composable
public fun AlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues =
        if (icon != null) {
            AlertDialogDefaults.contentWithIconPadding()
        } else {
            AlertDialogDefaults.contentPadding()
        },
    properties: DialogProperties = DialogProperties(),
    content: (ScalingLazyListScope.() -> Unit)? = null,
) {
    Dialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier =
            Modifier.alertDialogGesture(
                action = GestureAction.Dismiss,
                onGesture = onDismissRequest,
            ),
        properties = properties,
    ) {
        AlertDialogContent(
            title = title,
            modifier = modifier,
            icon = icon,
            text = text,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

/**
 * Dialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task. The dialog is scrollable by default if the content
 * exceeds the viewport height.
 *
 * This overload doesn't have any dedicated slots for buttons, but it does have a content slot so
 * that the caller has flexibility in how to seek user input. It uses a [TransformationSpec] for
 * advanced control over transformations of scrolling content.
 *
 * In most cases, we recommend using other AlertDialog variations with 2 confirm/dismiss buttons or
 * a single confirmation button. Where user input is not required, such as displaying a transient
 * success or failure message, use [ConfirmationDialog], [SuccessConfirmationDialog] or
 * [FailureConfirmationDialog] instead.
 *
 * The [transformationSpec] parameter enables the use of a [TransformingLazyColumn] internally for
 * advanced control over transformations of scrolling content, which can not be achieved with
 * [ScalingLazyColumn].
 *
 * @param visible A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping to
 *   the right or by other dismiss action. Implementation of this lambda must remove the dialog from
 *   the composition hierarchy e.g. by setting [visible] to false.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param transformationSpec A spec that defines how items inside the [TransformingLazyColumn]
 *   transform and animate as they are scrolled. It is recommended to create a spec using
 *   [rememberTransformationSpec]. This parameter is the key differentiator from the standard
 *   [AlertDialog] overload, which uses a [ScalingLazyColumn] without per-item customizable
 *   animations. The provided [transformationSpec] is automatically applied only to the dialog's
 *   standard components (title, text, and buttons), while additional items in [content] lambda need
 *   to be handled manually, by applying a [TransformationSpec] to each of them. First item has a
 *   fixed spec, with a reduced transition area, so that the top item does not appear scaled or
 *   faded when the dialog is initially displayed.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. It is recommended
 *   to use the defaults, which adjust to reduce the top padding when an icon is present.
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param content A slot for additional content, displayed within a scrollable
 *   [TransformingLazyColumn]. To ensure that your custom items animate consistently with the rest
 *   of the dialog, we recommend applying a [TransformationSpec] to every item.
 */
@Composable
public fun AlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    transformationSpec: TransformationSpec,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: @Composable (Boolean) -> PaddingValues = { isScrollable: Boolean ->
        if (icon != null) {
            AlertDialogDefaults.buttonStackWithIconContentPadding(isScrollable)
        } else {
            AlertDialogDefaults.buttonStackContentPadding(isScrollable)
        }
    },
    properties: DialogProperties = DialogProperties(),
    content: (TransformingLazyColumnScope.() -> Unit)? = null,
) {
    Dialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier =
            Modifier.alertDialogGesture(
                action = GestureAction.Dismiss,
                onGesture = onDismissRequest,
            ),
        properties = properties,
    ) {
        AlertDialogContent(
            title = title,
            modifier = modifier,
            icon = icon,
            text = text,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            transformationSpec = transformationSpec,
            content = content,
        )
    }
}

// Edge button
/**
 * Dialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task. The dialog is scrollable by default if the content
 * exceeds the viewport height.
 *
 * This overload has a single slot for a confirm [EdgeButton] at the bottom of the dialog, which
 * should be used when the user will be presented with a single acknowledgement.
 *
 * Where user input is not required, such as displaying a transient success or failure message, use
 * [ConfirmationDialog], [SuccessConfirmationDialog] or [FailureConfirmationDialog] instead.
 *
 * AlertDialog now additionally offers an equivalent overload using TransformingLazyColumn, which is
 * recommended to achieve a consistent Material 3 scrolling experience throughout your app
 *
 * Example of an [AlertDialog] with an icon, title, text and bottom [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.AlertDialogWithEdgeButtonSample
 *
 * Example of an [AlertDialog] with content groups and a bottom [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.AlertDialogWithContentGroupsSample
 * @param visible A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping to
 *   the right or by other dismiss action. Implementation of this lambda must remove the dialog from
 *   the composition hierarchy e.g. by setting [visible] to false.
 * @param edgeButton Slot for an [EdgeButton] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy e.g. by setting [visible] to false. It's
 *   recommended to use [AlertDialogDefaults.EdgeButton] in this slot with onClick callback. Note
 *   that when using an [EdgeButton] which is not Medium size, the contentPadding parameters should
 *   be specified.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text.By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. Bottom padding
 *   will be ignored and default spacing for the [EdgeButton] will be used. It is recommended to use
 *   the defaults, which adjust to reduce the top padding when an icon is present.
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 *   Any buttons added in this slot that are intended to dismiss the dialog must remove the dialog
 *   from the composition hierarchy e.g. by setting [visible] to false.
 */
@Composable
public fun AlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    edgeButton: (@Composable BoxScope.() -> Unit),
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues =
        if (icon != null) {
            AlertDialogDefaults.contentWithIconPadding()
        } else {
            AlertDialogDefaults.contentPadding()
        },
    properties: DialogProperties = DialogProperties(),
    content: (ScalingLazyListScope.() -> Unit)? = null,
) {
    Dialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier =
            Modifier.alertDialogGesture(
                action = GestureAction.Dismiss,
                onGesture = onDismissRequest,
            ),
        properties = properties,
    ) {
        AlertDialogContent(
            edgeButton = edgeButton,
            title = title,
            modifier = modifier,
            icon = icon,
            text = text,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

/**
 * Dialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task. The dialog is scrollable by default if the content
 * exceeds the viewport height.
 *
 * This overload has a single slot for a confirm [EdgeButton] at the bottom of the dialog, which
 * should be used when the user will be presented with a single acknowledgement. It uses a
 * [TransformationSpec] for advanced control over transformations of scrolling content.
 *
 * Where user input is not required, such as displaying a transient success or failure message, use
 * [ConfirmationDialog], [SuccessConfirmationDialog] or [FailureConfirmationDialog] instead.
 *
 * The [transformationSpec] parameter enables the use of a [TransformingLazyColumn] internally for
 * advanced control over transformations of scrolling content, which can not be achieved with
 * [ScalingLazyColumn].
 *
 * Example of an [AlertDialog] with an icon, title, text and bottom [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.AlertDialogWithEdgeButtonTransformingContentSample
 *
 * Example of an [AlertDialog] with content groups and a bottom [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.AlertDialogWithContentGroupsTransformingContentSample
 * @param visible A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping to
 *   the right or by other dismiss action. Implementation of this lambda must remove the dialog from
 *   the composition hierarchy e.g. by setting [visible] to false.
 * @param edgeButton Slot for an [EdgeButton] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy e.g. by setting [visible] to false. It's
 *   recommended to use [AlertDialogDefaults.EdgeButton] in this slot with onClick callback. Note
 *   that when using an [EdgeButton] which is not Medium size, the contentPadding parameters should
 *   be specified.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text.By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param transformationSpec A spec that defines how items inside the [TransformingLazyColumn]
 *   transform and animate as they are scrolled. It is recommended to create a spec using
 *   [rememberTransformationSpec]. This parameter is the key differentiator from the standard
 *   [AlertDialog] overload, which uses a [ScalingLazyColumn] without per-item customizable
 *   animations. The provided [transformationSpec] is automatically applied only to the dialog's
 *   standard components (title, text, and buttons), while additional items in [content] lambda need
 *   to be handled manually, by applying a [TransformationSpec] to each of them. First item has a
 *   fixed spec, with a reduced transition area, so that the top item does not appear scaled or
 *   faded when the dialog is initially displayed.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. Bottom padding
 *   will be ignored and default spacing for the [EdgeButton] will be used. It is recommended to use
 *   the defaults, which adjust to reduce the top padding when an icon is present.
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param content A slot for additional content, displayed within a scrollable
 *   [TransformingLazyColumn]. Any buttons added in this slot that are intended to dismiss the
 *   dialog must remove the dialog from the composition hierarchy e.g. by setting [visible] to
 *   false. To ensure that your custom items animate consistently with the rest of the dialog, we
 *   recommend applying a [TransformationSpec] to each of them - see samples.
 */
@Composable
public fun AlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    edgeButton: (@Composable BoxScope.() -> Unit),
    title: @Composable () -> Unit,
    transformationSpec: TransformationSpec,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues =
        if (icon != null) {
            AlertDialogDefaults.contentWithIconPadding()
        } else {
            AlertDialogDefaults.contentPadding()
        },
    properties: DialogProperties = DialogProperties(),
    content: (TransformingLazyColumnScope.() -> Unit)? = null,
) {
    Dialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier =
            Modifier.alertDialogGesture(
                action = GestureAction.Dismiss,
                onGesture = onDismissRequest,
            ),
        properties = properties,
    ) {
        AlertDialogContent(
            edgeButton = edgeButton,
            title = title,
            modifier = modifier,
            icon = icon,
            text = text,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            transformationSpec = transformationSpec,
            content = content,
        )
    }
}

// AlertDialogContent

// Confirm and dismiss buttons

/**
 * This [AlertDialogContent] overload provides the content for an [AlertDialog] with 2 buttons to
 * confirm or dismiss an action. Prefer using [AlertDialog] directly, which provides built-in
 * animations and a streamlined API. This composable may be used to provide the content for an alert
 * dialog if custom animations are required.
 *
 * AlertDialogContent now additionally offers an equivalent overload using TransformingLazyColumn,
 * which is recommended to achieve a consistent Material 3 scrolling experience throughout your app
 *
 * @param confirmButton A slot for a [Button] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy. It's recommended to use
 *   [AlertDialogDefaults.ConfirmButton] in this slot with onClick callback.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param dismissButton A slot for a [Button] indicating negative sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy. It's recommended to use
 *   [AlertDialogDefaults.DismissButton] in this slot with onClick callback.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. It is recommended
 *   to use the defaults, which adjust to reduce the top padding when an icon is present.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 */
@Composable
public fun AlertDialogContent(
    confirmButton: @Composable RowScope.() -> Unit,
    title: @Composable () -> Unit,
    dismissButton: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues =
        if (icon != null) {
            AlertDialogDefaults.confirmDismissWithIconContentPadding()
        } else {
            AlertDialogDefaults.confirmDismissContentPadding()
        },
    content: (ScalingLazyListScope.() -> Unit)? = null,
) {
    val scrollableLayout: @Composable () -> Unit = {
        val state = rememberScalingLazyListState(initialCenterItemIndex = 0)
        ScreenScaffold(scrollState = state, modifier = modifier, contentPadding = contentPadding) {
            contentPadding ->
            ScalingLazyColumn(
                scalingParams = AlertScalingParams,
                state = state,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = verticalArrangement,
                autoCentering = null,
                modifier = Modifier.fillMaxSize(),
            ) {
                alertDialogCommonContent(icon = icon, title = title, text = text, content = content)
                item {
                    ConfirmDismissButtons(
                        confirmButton = confirmButton,
                        dismissButton = dismissButton,
                        extraBottomPaddingEnabled = true,
                    )
                }
            }
        }
    }

    if (content != null) scrollableLayout()
    else {
        DynamicScrollableOrFixedLayout(
            scrollableContent = scrollableLayout,
            fixedContent = { forMeasure ->
                AlertDialogContentFixedWithConfirmationButtons(
                    confirmButton = confirmButton,
                    title = title,
                    dismissButton = dismissButton,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    contentPadding = contentPadding,
                    forMeasure = forMeasure,
                )
            },
        )
    }
}

/**
 * This overload provides the content for an [AlertDialog] with 2 buttons to confirm or dismiss an
 * action and a [TransformationSpec] for advanced control over transformations of scrolling content.
 * Prefer using [AlertDialog] directly, which provides built-in animations and a streamlined API.
 * This composable may be used to provide the content for an alert dialog if custom animations are
 * required.
 *
 * @param confirmButton A slot for a [Button] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy. It's recommended to use
 *   [AlertDialogDefaults.ConfirmButton] in this slot with onClick callback.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param dismissButton A slot for a [Button] indicating negative sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy. It's recommended to use
 *   [AlertDialogDefaults.DismissButton] in this slot with onClick callback.
 * @param transformationSpec A spec that defines how items inside the [TransformingLazyColumn]
 *   transform and animate as they are scrolled. It is recommended to create a spec using
 *   [rememberTransformationSpec]. This parameter is the key differentiator from the standard
 *   [AlertDialog] overload, which uses a [ScalingLazyColumn] without per-item customizable
 *   animations. The provided [transformationSpec] is automatically applied only to the dialog's
 *   standard components (title, text, and buttons), while additional items in [content] lambda need
 *   to be handled manually, by applying a [TransformationSpec] to each of them. First item has a
 *   fixed spec, with a reduced transition area, so that the top item does not appear scaled or
 *   faded when the dialog is initially displayed.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. It is recommended
 *   to use the defaults, which adjust to reduce the top padding when an icon is present.
 * @param content A slot for additional content, displayed within a scrollable
 *   [TransformingLazyColumn]. To ensure that your custom items animate consistently with the rest
 *   of the dialog, we recommend applying a [TransformationSpec] to each of them.
 */
@Composable
public fun AlertDialogContent(
    confirmButton: @Composable RowScope.() -> Unit,
    title: @Composable () -> Unit,
    dismissButton: @Composable RowScope.() -> Unit,
    transformationSpec: TransformationSpec,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues =
        if (icon != null) {
            AlertDialogDefaults.confirmDismissWithIconContentPadding()
        } else {
            AlertDialogDefaults.confirmDismissContentPadding()
        },
    content: (TransformingLazyColumnScope.() -> Unit)? = null,
) {
    var scrollGestureIndicatorVisible by remember { mutableStateOf(false) }
    val scrollableLayout: @Composable () -> Unit = {
        val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 0)
        ScreenScaffold(
            scrollState = state,
            modifier = modifier,
            contentPadding = contentPadding,
            scrollIndicator = {
                OneHandedGestureDefaults.ScrollGestureIndicator(
                    scrollGestureIndicatorVisible,
                    onGestureIndicatorFinished = { scrollGestureIndicatorVisible = false },
                    state,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            },
        ) { contentPadding ->
            TransformingLazyColumn(
                state = state,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = verticalArrangement,
                modifier =
                    Modifier.fillMaxSize()
                        .alertDialogGesture(
                            action = GestureAction.Primary,
                            priority = GesturePriority.Scrollable,
                            onGesture = { OneHandedGestureDefaults.scrollDown(state) },
                            onShowIndicator = { scrollGestureIndicatorVisible = true },
                        ),
            ) {
                alertDialogCommonContent(
                    icon = icon,
                    title = title,
                    text = text,
                    transformationSpec = transformationSpec,
                    topItemTransformationSpec = TopItemTransformationSpec,
                    content = content,
                )
                item {
                    ConfirmDismissButtons(
                        confirmButton = confirmButton,
                        dismissButton = dismissButton,
                        transformationSpec = transformationSpec,
                        extraBottomPaddingEnabled = true,
                    )
                }
            }
        }
    }

    if (content != null) scrollableLayout()
    else {
        DynamicScrollableOrFixedLayout(
            scrollableContent = scrollableLayout,
            fixedContent = { forMeasure ->
                AlertDialogContentFixedWithConfirmationButtons(
                    confirmButton = confirmButton,
                    title = title,
                    dismissButton = dismissButton,
                    modifier = modifier,
                    icon = icon,
                    text = text,
                    verticalArrangement = verticalArrangement,
                    contentPadding = contentPadding,
                    forMeasure = forMeasure,
                )
            },
        )
    }
}

// No buttons

/**
 * This [AlertDialogContent] overload provides the content for an [AlertDialog] without any
 * dedicated slots for buttons. Prefer using [AlertDialog] directly, which provides built-in
 * animations and a streamlined API. This composable may be used to provide the content for an alert
 * dialog if custom animations are required.
 *
 * AlertDialogContent now additionally offers an equivalent overload using TransformingLazyColumn,
 * which is recommended to achieve a consistent Material 3 scrolling experience throughout your app
 *
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. It is recommended
 *   to use the defaults, which adjust to reduce the top padding when an icon is present.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 */
@Composable
public fun AlertDialogContent(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues =
        if (icon != null) {
            AlertDialogDefaults.contentWithIconPadding()
        } else {
            AlertDialogDefaults.contentPadding()
        },
    content: (ScalingLazyListScope.() -> Unit)? = null,
) {
    val scrollableLayout: @Composable () -> Unit = {
        val state = rememberScalingLazyListState(initialCenterItemIndex = 0)
        ScreenScaffold(scrollState = state, modifier = modifier, contentPadding = contentPadding) {
            contentPadding ->
            ScalingLazyColumn(
                scalingParams = AlertScalingParams,
                state = state,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = verticalArrangement,
                autoCentering = null,
                modifier = Modifier.fillMaxSize(),
            ) {
                alertDialogCommonContent(icon = icon, title = title, text = text, content = content)
            }
        }
    }

    if (content != null) scrollableLayout()
    else {
        DynamicScrollableOrFixedLayout(
            scrollableContent = scrollableLayout,
            fixedContent = { forMeasure ->
                AlertDialogContentFixed(
                    title = title,
                    icon = icon,
                    text = text,
                    modifier = modifier,
                    verticalArrangement = verticalArrangement,
                    contentPadding = contentPadding,
                    forMeasure = forMeasure,
                )
            },
        )
    }
}

/**
 * This overload provides the content for an [AlertDialog] without any dedicated slots for buttons
 * and a [TransformationSpec] for advanced control over transformations of scrolling content..
 * Prefer using [AlertDialog] directly, which provides built-in animations and a streamlined API.
 * This composable may be used to provide the content for an alert dialog if custom animations are
 * required.
 *
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param transformationSpec A spec that defines how items inside the [TransformingLazyColumn]
 *   transform and animate as they are scrolled. It is recommended to create a spec using
 *   [rememberTransformationSpec]. This parameter is the key differentiator from the standard
 *   [AlertDialog] overload, which uses a [ScalingLazyColumn] without per-item customizable
 *   animations. The provided [transformationSpec] is automatically applied only to the dialog's
 *   standard components (title, text, and buttons), while additional items in [content] lambda need
 *   to be handled manually, by applying a [TransformationSpec] to each of them. First item has a
 *   fixed spec, with a reduced transition area, so that the top item does not appear scaled or
 *   faded when the dialog is initially displayed.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. It is recommended
 *   to use the defaults, which adjust to reduce the top padding when an icon is present.
 * @param content A slot for additional content, displayed within a scrollable
 *   [TransformingLazyColumn]. To ensure that your custom items animate consistently with the rest
 *   of the dialog, we recommend applying a [TransformationSpec] to each of them.
 */
@Composable
public fun AlertDialogContent(
    title: @Composable () -> Unit,
    transformationSpec: TransformationSpec,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: @Composable (Boolean) -> PaddingValues = { isScrollable: Boolean ->
        if (icon != null) {
            AlertDialogDefaults.buttonStackWithIconContentPadding(isScrollable)
        } else {
            AlertDialogDefaults.buttonStackContentPadding(isScrollable)
        }
    },
    content: (TransformingLazyColumnScope.() -> Unit)? = null,
) {
    val scrollableLayout: @Composable () -> Unit = {
        var scrollGestureIndicatorVisible by remember { mutableStateOf(false) }
        val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 0)
        ScreenScaffold(
            scrollState = state,
            modifier = modifier,
            contentPadding = contentPadding(true),
            scrollIndicator = {
                OneHandedGestureDefaults.ScrollGestureIndicator(
                    scrollGestureIndicatorVisible,
                    onGestureIndicatorFinished = { scrollGestureIndicatorVisible = false },
                    state,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            },
        ) { contentPadding ->
            TransformingLazyColumn(
                state = state,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = verticalArrangement,
                modifier =
                    Modifier.fillMaxSize()
                        .alertDialogGesture(
                            action = GestureAction.Primary,
                            priority = GesturePriority.Scrollable,
                            onGesture = { OneHandedGestureDefaults.scrollDown(state) },
                            onShowIndicator = { scrollGestureIndicatorVisible = true },
                        ),
            ) {
                alertDialogCommonContent(
                    icon = icon,
                    title = title,
                    text = text,
                    transformationSpec = transformationSpec,
                    topItemTransformationSpec = TopItemTransformationSpec,
                    content = content,
                )
            }
        }
    }

    if (content != null) scrollableLayout()
    else {
        DynamicScrollableOrFixedLayout(
            scrollableContent = scrollableLayout,
            fixedContent = { forMeasure ->
                AlertDialogContentFixed(
                    title = title,
                    icon = icon,
                    text = text,
                    modifier = modifier,
                    verticalArrangement = verticalArrangement,
                    contentPadding = contentPadding(false),
                    forMeasure = forMeasure,
                )
            },
        )
    }
}

/**
 * This [AlertDialogContent] overload provides the content for an [AlertDialog] with a single
 * [EdgeButton] to confirm an action. Prefer using [AlertDialog] directly, which provides built-in
 * animations and a streamlined API. This composable may be used to provide the content for an alert
 * dialog if custom animations are required.
 *
 * AlertDialogContent now additionally offers an equivalent overload using TransformingLazyColumn,
 * which is recommended to achieve a consistent Material 3 scrolling experience throughout your app
 *
 * @param edgeButton Slot for an [EdgeButton] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy. It's recommended to use
 *   [AlertDialogDefaults.EdgeButton] in this slot with onClick callback. Note that when using an
 *   [EdgeButton] which is not Medium size, the contentPadding parameters should be specified.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. Bottom padding
 *   will be ignored and default spacing for the [EdgeButton] will be used. It is recommended to use
 *   the defaults, which adjust to reduce the top padding when an icon is present.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 */
@Composable
public fun AlertDialogContent(
    edgeButton: (@Composable BoxScope.() -> Unit),
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues =
        if (icon != null) {
            AlertDialogDefaults.contentWithIconPadding()
        } else {
            AlertDialogDefaults.contentPadding()
        },
    content: (ScalingLazyListScope.() -> Unit)? = null,
) {
    // AlertDialog + EdgeButton layout does not differentiate between scrollable/fixed content
    // (unlike the confirm/dismiss and button-stack layouts) - so DynamicScrollableOrFixedLayout
    // is not needed.
    val state = rememberScalingLazyListState(initialCenterItemIndex = 0)
    val noTextAndContent = text == null && content == null
    ScreenScaffold(
        scrollState = state,
        edgeButton = edgeButton,
        modifier = modifier,
        contentPadding = contentPadding,
        edgeButtonSpacing =
            if (noTextAndContent) AlertEdgeButtonSpacingWithoutTextAndContent
            else AlertEdgeButtonSpacing,
    ) { contentPadding ->
        ScalingLazyColumn(
            scalingParams = AlertScalingParams,
            state = state,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = verticalArrangement,
            autoCentering = null,
            modifier = Modifier.fillMaxSize(),
        ) {
            alertDialogCommonContent(icon = icon, title = title, text = text, content = content)
        }
    }
}

/**
 * This overload provides the content for an [AlertDialog] with a single [EdgeButton] to confirm an
 * action and a [TransformationSpec] for advanced control over transformations of scrolling content.
 * Prefer using [AlertDialog] directly, which provides built-in animations and a streamlined API.
 * This composable may be used to provide the content for an alert dialog if custom animations are
 * required.
 *
 * @param edgeButton Slot for an [EdgeButton] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy. It's recommended to use
 *   [AlertDialogDefaults.EdgeButton] in this slot with onClick callback. Note that when using an
 *   [EdgeButton] which is not Medium size, the contentPadding parameters should be specified.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param transformationSpec A spec that defines how items inside the [TransformingLazyColumn]
 *   transform and animate as they are scrolled. It is recommended to create a spec using
 *   [rememberTransformationSpec]. This parameter is the key differentiator from the standard
 *   [AlertDialog] overload, which uses a [ScalingLazyColumn] without per-item customizable
 *   animations. The provided [transformationSpec] is automatically applied only to the dialog's
 *   standard components (title, text, and buttons), while additional items in [content] lambda need
 *   to be handled manually, by applying a [TransformationSpec] to each of them. First item has a
 *   fixed spec, with a reduced transition area, so that the top item does not appear scaled or
 *   faded when the dialog is initially displayed.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. Bottom padding
 *   will be ignored and default spacing for the [EdgeButton] will be used. It is recommended to use
 *   the defaults, which adjust to reduce the top padding when an icon is present.
 * @param transformationSpec A spec that defines how components inside the [TransformingLazyColumn]
 *   transform and animate as they are scrolled. It is recommended to create a spec using
 *   [rememberTransformationSpec]. The provided [transformationSpec] is automatically applied only
 *   to the dialog's standard components (icon, title, text, and buttons). This parameter is the key
 *   differentiator from the standard [AlertDialog] overload, which uses a [ScalingLazyColumn] with
 *   automatic non-customizable animations.
 * @param content A slot for additional content, displayed within a scrollable
 *   [TransformingLazyColumn].To ensure that your custom items animate consistently with the rest of
 *   the dialog, we recommend applying a [TransformationSpec] to each of them.
 */
@Composable
public fun AlertDialogContent(
    edgeButton: (@Composable BoxScope.() -> Unit),
    title: @Composable () -> Unit,
    transformationSpec: TransformationSpec,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues =
        if (icon != null) {
            AlertDialogDefaults.contentWithIconPadding()
        } else {
            AlertDialogDefaults.contentPadding()
        },
    content: (TransformingLazyColumnScope.() -> Unit)? = null,
) {
    /*
       AlertDialog + EdgeButton layout does not differentiate between scrollable/fixed content (unlike the confirm/dismiss and button-stack layouts) - so DynamicScrollableOrFixedLayout is not needed.
    */
    var scrollGestureIndicatorVisible by remember { mutableStateOf(false) }
    val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = 0)
    val noTextAndContent = text == null && content == null
    ScreenScaffold(
        scrollState = state,
        modifier = modifier,
        edgeButton = edgeButton,
        contentPadding = contentPadding,
        scrollIndicator = {
            OneHandedGestureDefaults.ScrollGestureIndicator(
                scrollGestureIndicatorVisible,
                onGestureIndicatorFinished = { scrollGestureIndicatorVisible = false },
                state,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        },
        edgeButtonSpacing =
            if (noTextAndContent) AlertEdgeButtonSpacingWithoutTextAndContent
            else AlertEdgeButtonSpacing,
    ) { contentPadding ->
        TransformingLazyColumn(
            state = state,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = verticalArrangement,
            modifier =
                Modifier.fillMaxSize()
                    .alertDialogGesture(
                        action = GestureAction.Primary,
                        priority = GesturePriority.Scrollable,
                        onGesture = { OneHandedGestureDefaults.scrollDown(state) },
                        onShowIndicator = { scrollGestureIndicatorVisible = true },
                    ),
        ) {
            alertDialogCommonContent(
                icon = icon,
                title = title,
                text = text,
                transformationSpec = transformationSpec,
                topItemTransformationSpec = TopItemTransformationSpec,
                content = content,
            )
        }
    }
}

/** Contains the default values used by [AlertDialog] */
public object AlertDialogDefaults {
    /**
     * Default composable for the edge button in an [AlertDialog]. This is a medium sized
     * [EdgeButton]. Should be used with [AlertDialog] overload which contains a single edgeButton
     * slot.
     *
     * @param onClick The callback to be invoked when the button is clicked.
     * @param modifier The [Modifier] to be applied to the button.
     * @param colors The [ButtonColors] to be used for the button.
     * @param content The composable content of the button. Defaults to [ConfirmIcon].
     */
    @Composable
    public fun EdgeButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        content: @Composable RowScope.() -> Unit = ConfirmIcon,
    ) {
        var currentButtonSize by remember { mutableStateOf(IntSize.Zero) }
        val density = LocalDensity.current
        val buttonSize = EdgeButtonSize.Medium
        val fullSizeInPx = with(density) { buttonSize.maximumHeight.toPx() }

        val isButtonFullyVisible by remember {
            derivedStateOf {
                // EdgeButton includes internal padding in its layout. Since that padding value
                // isn't
                // exposed here, we consider the button 'fully visible' once its transformed height
                // meets or exceeds its base theoretical size in pixels.
                currentButtonSize.height.toFloat() >= fullSizeInPx
            }
        }

        val interactionSource = remember { MutableInteractionSource() }
        var gestureIndicatorVisible by remember { mutableStateOf(false) }

        return EdgeButton(
            modifier =
                modifier.onSizeChanged { size -> currentButtonSize = size } then
                    if (isButtonFullyVisible) {
                        Modifier.alertDialogGesture(
                            action = GestureAction.Primary,
                            priority = GesturePriority.Clickable,
                            interactionSource = interactionSource,
                            onShowIndicator = { gestureIndicatorVisible = true },
                            onGesture = onClick,
                        )
                    } else {
                        Modifier
                    },
            interactionSource = interactionSource,
            onClick = onClick,
            colors = colors,
            buttonSize = buttonSize,
            content = {
                OneHandedGestureDefaults.GestureIndicator(
                    gestureIndicatorVisible,
                    { gestureIndicatorVisible = false },
                ) {
                    // wrap content() in a Row because EdgeButton's content parameter is as
                    // RowScope.() -> Unit.
                    Row(content = content)
                }
            },
        )
    }

    /**
     * Default composable for the confirm button in an [AlertDialog]. Should be used with
     * [AlertDialog] overload which has 2 button slots to confirm or dismiss the action.
     *
     * @param onClick The callback to be invoked when the button is clicked.
     * @param modifier The [Modifier] to be applied to the button.
     * @param colors The [IconButtonColors] to be used for the button.
     * @param content The composable content of the button. Defaults to [ConfirmIcon].
     */
    @Composable
    public fun ConfirmButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
        content: @Composable RowScope.() -> Unit = ConfirmIcon,
    ) {
        val confirmWidth = 63.dp
        val confirmHeight = 54.dp
        val confirmShape = CircleShape
        val interactionSource = remember { MutableInteractionSource() }
        var gestureIndicatorVisible by remember { mutableStateOf(false) }
        var buttonVisible by remember { mutableStateOf(false) }
        FilledIconButton(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier =
                modifier
                    .onVisibilityChanged(minFractionVisible = 0.9f) { buttonVisible = it }
                    .rotate(-45f)
                    .size(confirmWidth, confirmHeight)
                    .then(
                        if (buttonVisible) {
                            Modifier.alertDialogGesture(
                                action = GestureAction.Primary,
                                interactionSource = interactionSource,
                                priority = GesturePriority.Clickable,
                                onShowIndicator = { gestureIndicatorVisible = true },
                                onGesture = onClick,
                            )
                        } else {
                            Modifier
                        }
                    ),
            colors = colors,
            shapes = IconButtonDefaults.shapes(confirmShape),
        ) {
            OneHandedGestureDefaults.GestureIndicator(
                gestureIndicatorVisible,
                { gestureIndicatorVisible = false },
                modifier = Modifier.rotate(45f),
            ) {
                Row(
                    modifier =
                        Modifier.semantics(mergeDescendants = true) {
                                semanticOnClick(
                                    action = {
                                        onClick()
                                        true
                                    }
                                )
                                role = Role.Button
                            }
                            .align(Alignment.Center)
                            .graphicsLayer { rotationZ = 45f }
                            .padding(10.dp),
                    content = content,
                )
            }
        }
    }

    /**
     * Default composable for the dismiss button in an [AlertDialog]. Should be used with
     * [AlertDialog] overload which has 2 button slots to confirm or dismiss the action.
     *
     * @param onClick The callback to be invoked when the button is clicked.
     * @param modifier The [Modifier] to be applied to the button.
     * @param colors The [IconButtonColors] to be used for the button.
     * @param content The composable content of the button. Defaults to [DismissIcon].
     */
    @Composable
    public fun DismissButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
        content: @Composable RowScope.() -> Unit = DismissIcon,
    ) {
        val dismissSize = 60.dp
        val dismissShape = MaterialTheme.shapes.medium
        Box(modifier = Modifier.size(dismissSize + cancelButtonPadding)) {
            FilledTonalIconButton(
                onClick = onClick,
                modifier = modifier.size(dismissSize).align(Alignment.BottomEnd),
                colors = colors,
                shapes = IconButtonDefaults.shapes(dismissShape),
            ) {
                Row(content = content)
            }
        }
    }

    /**
     * The padding to apply around the content for the [AlertDialog] variation with confirm dismiss
     * buttons and no icon content.
     */
    @Composable
    public fun confirmDismissContentPadding(): PaddingValues {
        val verticalPadding = verticalContentPadding()
        val horizontalPadding = horizontalContentPadding()
        return PaddingValues(
            top = calculateTopPadding(hasIcon = false),
            bottom = verticalPadding,
            start = horizontalPadding,
            end = horizontalPadding,
        )
    }

    /**
     * The padding to apply around the content for the [AlertDialog] variation when an icon is
     * provided and with confirm dismiss buttons.
     */
    @Composable
    public fun confirmDismissWithIconContentPadding(): PaddingValues {
        val verticalPadding = verticalContentPadding()
        val horizontalPadding = horizontalContentPadding()
        return PaddingValues(
            top = calculateTopPadding(hasIcon = true),
            bottom = verticalPadding,
            start = horizontalPadding,
            end = horizontalPadding,
        )
    }

    /**
     * The padding to apply around the content for the [AlertDialog] with either a stack of buttons
     * (no confirm or dismiss buttons) or an EdgeButton, and when there is no icon. For the
     * EdgeButton variation, the bottom padding is ignored.
     */
    @Composable
    public fun contentPadding(): PaddingValues {
        val horizontalPadding = horizontalContentPadding()
        return PaddingValues(
            top = calculateTopPadding(hasIcon = false),
            bottom = screenHeightFraction(noEdgeButtonBottomPaddingFraction),
            start = horizontalPadding,
            end = horizontalPadding,
        )
    }

    /**
     * The padding to apply around the content for the [AlertDialog] with either a stack of buttons
     * (no confirm or dismiss buttons) or an EdgeButton, and when there is an icon. For the
     * EdgeButton variation, the bottom padding is ignored.
     */
    @Composable
    public fun contentWithIconPadding(): PaddingValues {
        val horizontalPadding = horizontalContentPadding()
        return PaddingValues(
            top = calculateTopPadding(hasIcon = true),
            bottom = screenHeightFraction(noEdgeButtonBottomPaddingFraction),
            start = horizontalPadding,
            end = horizontalPadding,
        )
    }

    /**
     * The padding to apply around the content for the [AlertDialog] variation using
     * TransformingLazyColumn, without an icon, and with a stack of buttons for options. Bottom
     * padding is calculated based on the provided [isScrollable] parameter, which favors the use of
     * a bigger padding for scrollable content and a smaller one for fixed content.
     */
    @Composable
    public fun buttonStackContentPadding(isScrollable: Boolean): PaddingValues {
        val horizontalPadding = horizontalContentPadding()
        return PaddingValues(
            top = calculateTopPadding(hasIcon = false),
            bottom =
                if (isScrollable) screenHeightFraction(noEdgeButtonBottomPaddingFraction)
                else verticalContentPadding(),
            start = horizontalPadding,
            end = horizontalPadding,
        )
    }

    /**
     * The padding to apply around the content for the [AlertDialog] variation using
     * TransformingLazyColumn, with icon, and with a stack of buttons for options. Bottom padding is
     * calculated based on the provided [isScrollable] parameter, which favors the use of a bigger
     * padding for scrollable content and a smaller one for fixed content.
     */
    @Composable
    public fun buttonStackWithIconContentPadding(isScrollable: Boolean): PaddingValues {
        val horizontalPadding = horizontalContentPadding()
        return PaddingValues(
            top = calculateTopPadding(hasIcon = true),
            bottom =
                if (isScrollable) screenHeightFraction(noEdgeButtonBottomPaddingFraction)
                else verticalContentPadding(),
            start = horizontalPadding,
            end = horizontalPadding,
        )
    }

    /**
     * Separator for the [AlertDialog]. Should be used inside [AlertDialog] content for splitting
     * groups of elements.
     */
    @Composable
    public fun GroupSeparator() {
        Spacer(Modifier.height(8.dp))
    }

    /** Default vertical arrangement for an [AlertDialog]. */
    public val VerticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically)

    /** Default icon for the confirm button. */
    public val ConfirmIcon: @Composable RowScope.() -> Unit = {
        Icon(
            imageVector = Icons.Check,
            contentDescription = getString(Strings.AlertDialogContentDescriptionConfirmButton),
            modifier = Modifier.size(28.dp).align(Alignment.CenterVertically),
        )
    }

    /** Default icon for the dismiss button. */
    public val DismissIcon: @Composable RowScope.() -> Unit = {
        Icon(
            imageVector = Icons.Close,
            contentDescription = getString(Strings.AlertDialogContentDescriptionDismissButton),
            modifier = Modifier.size(28.dp).align(Alignment.CenterVertically),
        )
    }

    @Composable
    private fun calculateTopPadding(hasIcon: Boolean): Dp {
        return if (hasIcon) screenHeightFraction(iconTopPaddingFraction)
        else verticalContentPadding()
    }

    internal val iconTopPaddingFraction = 0.012f
    internal val noEdgeButtonBottomPaddingFraction = 0.3646f
    internal val cancelButtonPadding = 1.dp
}

@Composable
private fun DynamicScrollableOrFixedLayout(
    scrollableContent: @Composable () -> Unit,
    fixedContent: @Composable (measureStep: Boolean) -> Unit,
) {
    val measurableContentLambda =
        @Composable {
            Box(modifier = Modifier.wrapContentHeight().clearAndSetSemantics {}) {
                fixedContent(true)
            }
        }
    val fixedContentLambda = @Composable { fixedContent(false) }
    SubcomposeLayout { constraints ->
        val fixedMeasurable =
            subcompose("measurableContent", measurableContentLambda)
                .first()
                .measure(constraints.copy(maxHeight = Constraints.Infinity))
        val isScrollable = fixedMeasurable.height > constraints.maxHeight

        val placeable =
            subcompose(
                    if (isScrollable) "scrollableContent" else "fixedContent",
                    if (isScrollable) scrollableContent else fixedContentLambda,
                )
                .first()
                .measure(constraints)
        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }
}

@Composable
private fun AlertDialogContentFixedWithConfirmationButtons(
    confirmButton: @Composable RowScope.() -> Unit,
    title: @Composable () -> Unit,
    dismissButton: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues,
    forMeasure: Boolean,
) {
    Column(
        // Ignore the incoming modifier on measuring pass. We need to know the unconstrained height.
        modifier =
            if (forMeasure) Modifier.padding(contentPadding).wrapContentSize()
            else modifier.padding(contentPadding).fillMaxSize(),
        verticalArrangement = verticalArrangement,
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth().let { modifier ->
                    if (!forMeasure) modifier.weight(1f) else modifier
                },
            verticalArrangement = verticalArrangement,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                Row { IconAlert(content = icon) }
            }
            Row { Title(content = title) }
            if (text != null) {
                Row { TextMessage(content = text) }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            contentAlignment = Alignment.Center,
        ) {
            ConfirmDismissButtons(
                confirmButton = confirmButton,
                dismissButton = dismissButton,
                extraBottomPaddingEnabled = false,
            )
        }
    }
}

@Composable
private fun AlertDialogContentFixed(
    title: @Composable () -> Unit,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    modifier: Modifier,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues,
    forMeasure: Boolean,
) {
    Column(
        // Ignore the incoming modifier on measuring pass. We need to know the unconstrained height.
        modifier =
            if (forMeasure) Modifier.padding(contentPadding).wrapContentSize()
            else modifier.padding(contentPadding).fillMaxSize(),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Row { IconAlert(content = icon) }
        }
        Row { Title(content = title) }
        if (text != null) {
            Row { TextMessage(content = text) }
        }
    }
}

private fun ScalingLazyListScope.alertDialogCommonContent(
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
    text: @Composable (() -> Unit)? = null,
    content: (ScalingLazyListScope.() -> Unit)? = null,
) {
    if (icon != null) {
        item { IconAlert(content = icon) }
    }
    item { Title(content = title) }
    if (text != null) {
        item { TextMessage(content = text) }
    }
    if (content != null) {
        item { Spacer(modifier = Modifier.height(AlertContentTopSpacing)) }
        content()
    }
}

private fun TransformingLazyColumnScope.alertDialogCommonContent(
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
    text: @Composable (() -> Unit)? = null,
    transformationSpec: TransformationSpec,
    topItemTransformationSpec: TransformationSpec,
    content: (TransformingLazyColumnScope.() -> Unit)? = null,
) {
    if (icon != null) {
        item {
            IconAlert(
                modifier =
                    Modifier.transformedHeight(scope = this@item, topItemTransformationSpec)
                        .graphicsLayer {
                            with(SurfaceTransformation(topItemTransformationSpec)) {
                                applyContentTransformation()
                                applyContainerTransformation()
                            }
                        },
                content = icon,
            )
        }
    }
    item {
        val transformationSpec = if (icon == null) topItemTransformationSpec else transformationSpec
        Title(
            modifier =
                Modifier.transformedHeight(scope = this@item, transformationSpec).graphicsLayer {
                    with(SurfaceTransformation(transformationSpec)) {
                        applyContentTransformation()
                        applyContainerTransformation()
                    }
                },
            content = title,
        )
    }
    if (text != null) {
        item {
            TextMessage(
                modifier =
                    Modifier.transformedHeight(scope = this@item, transformationSpec)
                        .graphicsLayer {
                            with(SurfaceTransformation(transformationSpec)) {
                                applyContentTransformation()
                                applyContainerTransformation()
                            }
                        },
                content = text,
            )
        }
    }
    if (content != null) {
        item {
            Spacer(
                Modifier.height(AlertContentTopSpacing)
                    .transformedHeight(scope = this@item, transformationSpec)
                    .graphicsLayer {
                        with(SurfaceTransformation(transformationSpec)) {
                            applyContentTransformation()
                            applyContainerTransformation()
                        }
                    }
            )
        }
        content()
    }
}

@Composable
private fun IconAlert(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier) {
        content()
        Spacer(Modifier.height(AlertIconBottomSpacing))
    }
}

@Composable
private fun Title(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val horizontalPadding = screenWidthFraction(TitlePaddingFraction)
    Column(modifier = modifier.padding(horizontal = horizontalPadding)) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalTextStyle provides MaterialTheme.typography.titleMedium,
            LocalTextConfiguration provides
                TextConfiguration(
                    textAlign = TextAlign.Center,
                    maxLines = AlertTitleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                ),
            content = content,
        )
    }
}

@Composable
private fun ConfirmDismissButtons(
    confirmButton: @Composable RowScope.() -> Unit,
    dismissButton: @Composable RowScope.() -> Unit,
    spacingEnabled: Boolean = true,
    extraBottomPaddingEnabled: Boolean,
) {
    Column {
        Spacer(modifier = Modifier.height(ConfirmDismissButtonsTopSpacing))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(6.dp))
            dismissButton(this)
            Spacer(
                modifier =
                    Modifier.width(screenWidthFraction(ConfirmDismissBetweenButtonsPaddingFraction))
            )
            confirmButton(this)
            Spacer(modifier = Modifier.width(2.dp))
        }
        if (extraBottomPaddingEnabled)
            Spacer(
                modifier =
                    Modifier.height(
                        screenHeightFraction(ConfirmDismissButtonsBottomSpacingFraction)
                    )
            )
    }
}

@Composable
private fun TransformingLazyColumnItemScope.ConfirmDismissButtons(
    confirmButton: @Composable RowScope.() -> Unit,
    dismissButton: @Composable RowScope.() -> Unit,
    transformationSpec: TransformationSpec,
    extraBottomPaddingEnabled: Boolean,
) {
    val transformation = SurfaceTransformation(transformationSpec)
    Column(
        modifier =
            Modifier.transformedHeight(scope = this@ConfirmDismissButtons, transformationSpec)
    ) {
        Spacer(modifier = Modifier.height(ConfirmDismissButtonsTopSpacing))
        Row(
            modifier =
                Modifier.graphicsLayer {
                    with(transformation) {
                        applyContainerTransformation()
                        applyContentTransformation()
                    }
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(6.dp))
            dismissButton(this)
            Spacer(
                modifier =
                    Modifier.width(screenWidthFraction(ConfirmDismissBetweenButtonsPaddingFraction))
            )
            confirmButton(this)
            Spacer(modifier = Modifier.width(2.dp))
        }
        if (extraBottomPaddingEnabled)
            Spacer(
                modifier =
                    Modifier.height(
                        screenHeightFraction(ConfirmDismissButtonsBottomSpacingFraction)
                    )
            )
    }
}

@Composable
private fun TextMessage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val horizontalPadding = screenWidthFraction(TextPaddingFraction)
    Column(modifier = modifier.padding(horizontal = horizontalPadding)) {
        Spacer(Modifier.height(AlertTextMessageTopSpacing))
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
            LocalTextConfiguration provides
                TextConfiguration(
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = TextConfigurationDefaults.MaxLines,
                ),
            content = content,
        )
    }
}

@OptIn(ExperimentalWearComposeMaterial3Api::class)
@Composable
private fun Modifier.alertDialogGesture(
    action: GestureAction,
    priority: GesturePriority = GesturePriority.Unspecified,
    interactionSource: MutableInteractionSource? = null,
    onShowIndicator: () -> Unit = {},
    onGesture: suspend () -> Unit,
): Modifier =
    this then
        if (WearComposeMaterial3Flags.isOneHandedGesturesInAlertDialogEnabled) {
            Modifier.oneHandedGesture(
                action = action,
                priority = priority,
                interactionSource = interactionSource,
                onShowIndicator = onShowIndicator,
                onGesture = onGesture,
            )
        } else Modifier

internal val AlertIconBottomSpacing = 4.dp
internal val AlertTextMessageTopSpacing = 4.dp
internal val AlertEdgeButtonSpacing = 4.dp
internal val AlertEdgeButtonSpacingWithoutTextAndContent = 16.dp
internal val ConfirmDismissButtonsTopSpacing = 12.dp
internal val AlertContentTopSpacing = 8.dp
internal const val ConfirmDismissButtonsBottomSpacingFraction = 0.045f
internal const val AlertTitleMaxLines = 3
private const val TextPaddingFraction = 0.0416f
private const val TitlePaddingFraction = 0.12f
private const val ConfirmDismissBetweenButtonsPaddingFraction = 0.03f
private val AlertScalingParams = ScalingLazyColumnDefaults.scalingParams(minTransitionArea = 0.2f)

private val TopItemTransformationSpec: ResponsiveTransformationSpec =
    ResponsiveTransformationSpec.smallScreen(minTransitionAreaHeightFraction = 0.14f)
