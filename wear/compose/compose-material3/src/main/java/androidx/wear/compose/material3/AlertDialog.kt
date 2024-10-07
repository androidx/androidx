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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.PaddingDefaults.horizontalContentPadding
import androidx.wear.compose.material3.PaddingDefaults.verticalContentPadding
import androidx.wear.compose.materialcore.isSmallScreen
import androidx.wear.compose.materialcore.screenHeightDp
import androidx.wear.compose.materialcore.screenWidthDp

/**
 * AlertDialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task. The AlertDialog is scrollable by default if the
 * content exceeds the viewport height.
 *
 * This overload has 2 [Button]s for confirmation and cancellation, placed horizontally at the
 * bottom of the dialog. It should be used when the user will be presented with a binary decision,
 * to either confirm or dismiss an action.
 *
 * Example of an [AlertDialog] with an icon, title and two buttons to confirm and dismiss:
 *
 * @sample androidx.wear.compose.material3.samples.AlertDialogWithConfirmAndDismissSample
 * @param show A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping
 *   right (typically also called by the [dismissButton]).
 * @param confirmButton A slot for a [Button] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy e.g. by setting [show] to false. It's
 *   recommended to use [AlertDialogDefaults.ConfirmButton] in this slot with onClick callback.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text.
 * @param modifier Modifier to be applied to the dialog content.
 * @param dismissButton A slot for a [Button] indicating negative sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy e.g. by setting [show] to false. It's
 *   recommended to use [AlertDialogDefaults.DismissButton] in this slot with onClick callback.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents.
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 */
@Composable
fun AlertDialog(
    show: Boolean,
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
    contentPadding: PaddingValues = AlertDialogDefaults.confirmDismissContentPadding(),
    properties: DialogProperties = DialogProperties(),
    content: (ScalingLazyListScope.() -> Unit)? = null
) {
    AlertDialogImpl(
        show = show,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        title = title,
        icon = icon,
        text = text,
        alertButtonsParams = AlertButtonsParams.ConfirmDismissButtons(confirmButton, dismissButton),
        content = content
    )
}

/**
 * Dialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task. The dialog is scrollable by default if the content
 * exceeds the viewport height.
 *
 * This overload has a single slot for a confirm [EdgeButton] at the bottom of the dialog. It should
 * be used when the user will be presented with either a single acknowledgement, or a stack of
 * options.
 *
 * Example of an [AlertDialog] with an icon, title, text and bottom [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.AlertDialogWithBottomButtonSample
 *
 * Example of an [AlertDialog] with content groups and a bottom [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.AlertDialogWithContentGroupsSample
 * @param show A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping to
 *   the right or by other dismiss action.
 * @param bottomButton Optional slot for a [EdgeButton] indicating positive sentiment. Clicking the
 *   button must remove the dialog from the composition hierarchy e.g. by setting [show] to false.
 *   It's recommended to use [AlertDialogDefaults.BottomButton] in this slot with onClick callback.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents.
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 */
@Composable
fun AlertDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    bottomButton: (@Composable BoxScope.() -> Unit)?,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues = AlertDialogDefaults.contentPadding(bottomButton != null),
    properties: DialogProperties = DialogProperties(),
    content: (ScalingLazyListScope.() -> Unit)? = null
) {
    AlertDialogImpl(
        show = show,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        title = title,
        icon = icon,
        text = text,
        alertButtonsParams =
            if (bottomButton != null) AlertButtonsParams.BottomButton(bottomButton)
            else AlertButtonsParams.NoButtons,
        content = content
    )
}

/** Contains the default values used by [AlertDialog] */
object AlertDialogDefaults {

    /**
     * Default composable for the bottom button in an [AlertDialog]. Should be used with
     * [AlertDialog] overload which contains a single bottomButton slot.
     *
     * @param onClick The callback to be invoked when the button is clicked.
     * @param content The composable content of the button. Defaults to [ConfirmIcon].
     */
    @Composable
    fun BottomButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit = ConfirmIcon) {
        EdgeButton(
            modifier = Modifier.padding(top = edgeButtonExtraTopPadding),
            onClick = onClick,
            preferredHeight = ButtonDefaults.EdgeButtonHeightMedium,
            content = content
        )
    }

    /**
     * Default composable for the confirm button in an [AlertDialog]. Should be used with
     * [AlertDialog] overload which has 2 button slots to confirm or dismiss the action.
     *
     * @param onClick The callback to be invoked when the button is clicked.
     * @param content The composable content of the button. Defaults to [ConfirmIcon].
     */
    @Composable
    fun ConfirmButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit = ConfirmIcon) {
        val confirmWidth = if (isSmallScreen()) 63.dp else 76.dp
        val confirmHeight = if (isSmallScreen()) 54.dp else 65.dp

        val confirmShape = CircleShape

        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.rotate(-45f).size(confirmWidth, confirmHeight),
            shape = confirmShape
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center).graphicsLayer { rotationZ = 45f },
                content = content
            )
        }
    }

    /**
     * Default composable for the dismiss button in an [AlertDialog]. Should be used with
     * [AlertDialog] overload which has 2 button slots to confirm or dismiss the action.
     *
     * @param onClick The callback to be invoked when the button is clicked.
     * @param content The composable content of the button. Defaults to [DismissIcon].
     */
    @Composable
    fun DismissButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit = DismissIcon) {
        val dismissSize = if (isSmallScreen()) 60.dp else 72.dp
        val dismissShape = MaterialTheme.shapes.medium

        Box(modifier = Modifier.size(dismissSize + cancelButtonPadding)) {
            FilledTonalIconButton(
                onClick = onClick,
                modifier = Modifier.size(dismissSize).align(Alignment.BottomEnd),
                shape = dismissShape,
            ) {
                Row(content = content)
            }
        }
    }

    /**
     * The padding to apply around the content for the [AlertDialog] variation with confirm dismiss
     * buttons.
     */
    @Composable
    fun confirmDismissContentPadding(): PaddingValues {
        val verticalPadding = verticalContentPadding()
        val horizontalPadding = horizontalContentPadding()
        return PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding)
    }

    /**
     * The padding to apply around the content for the [AlertDialog] variation with a stack of
     * options and optional bottom button.
     */
    @Composable
    fun contentPadding(hasBottomButton: Boolean): PaddingValues {
        val topPadding = verticalContentPadding()
        val horizontalPadding = horizontalContentPadding()
        val bottomPadding =
            if (hasBottomButton) edgeButtonHeightWithPadding
            else screenHeightDp().dp * noEdgeButtonBottomPaddingFraction
        return PaddingValues(
            top = topPadding,
            bottom = bottomPadding,
            start = horizontalPadding,
            end = horizontalPadding,
        )
    }

    /**
     * Separator for the [AlertDialog]. Should be used inside [AlertDialog] content for splitting
     * groups of elements.
     */
    @Composable
    fun GroupSeparator() {
        Spacer(Modifier.height(8.dp))
    }

    /** Default vertical arrangement for an [AlertDialog]. */
    val VerticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically)

    /** Default icon for the confirm button. */
    val ConfirmIcon: @Composable RowScope.() -> Unit = {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(36.dp).align(Alignment.CenterVertically)
        )
    }

    /** Default icon for the dismiss button. */
    val DismissIcon: @Composable RowScope.() -> Unit = {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = null,
            modifier = Modifier.size(36.dp).align(Alignment.CenterVertically)
        )
    }

    /** The extra top padding to apply to the edge button. */
    private val edgeButtonExtraTopPadding = 1.dp
    private val edgeButtonHeightWithPadding = ButtonDefaults.EdgeButtonHeightMedium + 7.dp
    internal val noEdgeButtonBottomPaddingFraction = 0.3646f
    internal val cancelButtonPadding = 1.dp
}

@Composable
private fun AlertDialogImpl(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties,
    verticalArrangement: Arrangement.Vertical,
    contentPadding: PaddingValues,
    title: @Composable () -> Unit,
    icon: @Composable (() -> Unit)?,
    text: @Composable (() -> Unit)? = null,
    alertButtonsParams: AlertButtonsParams,
    content: (ScalingLazyListScope.() -> Unit)?
) {
    val state = rememberScalingLazyListState(initialCenterItemIndex = 0)

    Dialog(
        show = show,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        ScreenScaffold(
            scrollState = state,
            modifier = modifier,
            bottomButton =
                if (alertButtonsParams is AlertButtonsParams.BottomButton)
                    alertButtonsParams.bottomButton
                else null
        ) {
            ScalingLazyColumn(
                state = state,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = verticalArrangement,
                autoCentering = null,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (icon != null) {
                    item { IconAlert(icon) }
                }
                item { Title(title) }
                if (text != null) {
                    item { TextMessage(text) }
                }
                if (content != null) {
                    item { Spacer(Modifier.height(ContentTopSpacing)) }
                    content()
                }

                when (alertButtonsParams) {
                    is AlertButtonsParams.ConfirmDismissButtons ->
                        item { ConfirmDismissButtons(alertButtonsParams) }
                    is AlertButtonsParams.BottomButton ->
                        if (content == null) {
                            item { Spacer(Modifier.height(BottomButtonSpacing)) }
                        }
                    is AlertButtonsParams.NoButtons -> Unit
                }
            }
        }
    }
}

@Composable
private fun IconAlert(content: @Composable () -> Unit) {
    Column {
        content()
        Spacer(Modifier.height(AlertIconBottomSpacing))
    }
}

@Composable
private fun Title(content: @Composable () -> Unit) {
    val horizontalPadding = screenWidthDp().dp * TitlePaddingFraction
    Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalTextStyle provides MaterialTheme.typography.titleMedium,
            LocalTextConfiguration provides
                TextConfiguration(
                    textAlign = TextAlign.Center,
                    maxLines = AlertTitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                ),
            content = content
        )
    }
}

@Composable
private fun ConfirmDismissButtons(alertButtonsParams: AlertButtonsParams.ConfirmDismissButtons) {
    Column {
        Spacer(modifier = Modifier.height(ConfirmDismissButtonsTopSpacing))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(6.dp))
            alertButtonsParams.dismissButton(this)
            Spacer(modifier = Modifier.width(6.dp))
            alertButtonsParams.confirmButton(this)
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

@Composable
private fun TextMessage(content: @Composable () -> Unit) {
    val horizontalPadding = screenWidthDp().dp * TextPaddingFraction
    Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
        Spacer(Modifier.height(AlertTextMessageTopSpacing))
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
            LocalTextConfiguration provides
                TextConfiguration(
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = TextConfigurationDefaults.MaxLines
                ),
            content = content
        )
    }
}

private sealed interface AlertButtonsParams {
    object NoButtons : AlertButtonsParams

    class BottomButton(
        val bottomButton: @Composable BoxScope.() -> Unit,
    ) : AlertButtonsParams

    class ConfirmDismissButtons(
        val confirmButton: @Composable RowScope.() -> Unit,
        val dismissButton: @Composable RowScope.() -> Unit
    ) : AlertButtonsParams
}

internal val AlertIconBottomSpacing = 4.dp
internal val AlertTextMessageTopSpacing = 8.dp
internal val ConfirmDismissButtonsTopSpacing = 12.dp
internal const val AlertTitleMaxLines = 3

private val ContentTopSpacing = 8.dp
private val BottomButtonSpacing = 8.dp
private const val TextPaddingFraction = 0.0416f
private const val TitlePaddingFraction = 0.12f
