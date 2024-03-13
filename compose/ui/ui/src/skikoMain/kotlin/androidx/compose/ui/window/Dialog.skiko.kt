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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformInsetsConfig
import androidx.compose.ui.platform.union
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.rememberComposeSceneLayer
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp

/**
 * The default scrim opacity.
 */
private const val DefaultScrimOpacity = 0.6f
private val DefaultScrimColor = Color.Black.copy(alpha = DefaultScrimOpacity)

/**
 * Properties used to customize the behavior of a [Dialog].
 *
 * @property dismissOnBackPress whether the popup can be dismissed by pressing the back button
 *  * on Android or escape key on desktop.
 * If true, pressing the back button will call onDismissRequest.
 * @property dismissOnClickOutside whether the dialog can be dismissed by clicking outside the
 * dialog's bounds. If true, clicking outside the dialog will call onDismissRequest.
 * @property usePlatformDefaultWidth Whether the width of the dialog's content should be limited to
 * the platform default, which is smaller than the screen width.
 * @property usePlatformInsets Whether the size of the dialog's content should be limited by
 * platform insets.
 * @property useSoftwareKeyboardInset Whether the size of the dialog's content should be limited by
 * software keyboard inset.
 * @property scrimColor Color of background fill.
 */
@Immutable
actual class DialogProperties @ExperimentalComposeUiApi constructor(
    actual val dismissOnBackPress: Boolean = true,
    actual val dismissOnClickOutside: Boolean = true,
    actual val usePlatformDefaultWidth: Boolean = true,
    val usePlatformInsets: Boolean = true,
    val useSoftwareKeyboardInset: Boolean = true,
    val scrimColor: Color = DefaultScrimColor,
) {
    // Constructor with all non-experimental arguments.
    constructor(
        dismissOnBackPress: Boolean = true,
        dismissOnClickOutside: Boolean = true,
        usePlatformDefaultWidth: Boolean = true,
    ) : this(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
        usePlatformInsets = true,
        useSoftwareKeyboardInset = true,
        scrimColor = DefaultScrimColor,
    )

    actual constructor(
        dismissOnBackPress: Boolean,
        dismissOnClickOutside: Boolean,

        /*
         * Temporary hack to skip unsupported arguments from Android source set.
         * Should be removed after upstreaming changes from JetBrains' fork.
         *
         * After skip this unsupported argument, you must name all subsequent arguments.
         */
        @Suppress("FORBIDDEN_VARARG_PARAMETER_TYPE")
        vararg unsupported: Nothing,

        usePlatformDefaultWidth: Boolean,
    ) : this(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
        usePlatformInsets = true,
        useSoftwareKeyboardInset = true,
        scrimColor = DefaultScrimColor,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DialogProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false
        if (usePlatformDefaultWidth != other.usePlatformDefaultWidth) return false
        if (usePlatformInsets != other.usePlatformInsets) return false
        if (useSoftwareKeyboardInset != other.useSoftwareKeyboardInset) return false
        if (scrimColor != other.scrimColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + usePlatformDefaultWidth.hashCode()
        result = 31 * result + usePlatformInsets.hashCode()
        result = 31 * result + useSoftwareKeyboardInset.hashCode()
        result = 31 * result + scrimColor.hashCode()
        return result
    }
}

@Composable
actual fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    content: @Composable () -> Unit
) {
    val onKeyEvent = if (properties.dismissOnBackPress) {
        { event: KeyEvent ->
            if (event.isDismissRequest()) {
                onDismissRequest()
                true
            } else {
                false
            }
        }
    } else {
        null
    }
    val onOutsidePointerEvent = if (properties.dismissOnClickOutside) {
        { eventType: PointerEventType ->
            if (eventType == PointerEventType.Release) {
                onDismissRequest()
            }
        }
    } else {
        null
    }
    DialogLayout(
        modifier = Modifier.semantics { dialog() },
        onKeyEvent = onKeyEvent,
        onOutsidePointerEvent = onOutsidePointerEvent,
        properties = properties,
        content = content
    )
}

@Composable
private fun DialogLayout(
    properties: DialogProperties,
    modifier: Modifier = Modifier,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    onOutsidePointerEvent: ((eventType: PointerEventType) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val platformInsets = properties.platformInsets
    val layer = rememberComposeSceneLayer(
        focusable = true
    )
    layer.scrimColor = properties.scrimColor
    layer.setKeyEventListener(onPreviewKeyEvent, onKeyEvent)
    layer.setOutsidePointerEventListener(onOutsidePointerEvent)
    rememberLayerContent(layer) {
        val containerSize = LocalWindowInfo.current.containerSize
        val measurePolicy = rememberDialogMeasurePolicy(
            layer = layer,
            properties = properties,
            containerSize = containerSize,
            platformInsets = platformInsets
        )
        PlatformInsetsConfig.excludeInsets(
            safeInsets = properties.usePlatformInsets,
            ime = properties.useSoftwareKeyboardInset,
        ) {
            Layout(
                content = content,
                modifier = modifier,
                measurePolicy = measurePolicy
            )
        }
    }
}

private val DialogProperties.platformInsets: PlatformInsets
    @Composable get() {
        val safeInsets = if (usePlatformInsets) {
            PlatformInsetsConfig.safeInsets
        } else {
            PlatformInsets.Zero
        }
        val ime = if (useSoftwareKeyboardInset) {
            PlatformInsetsConfig.ime
        } else {
            PlatformInsets.Zero
        }
        return safeInsets.union(ime)
    }

@Composable
private fun rememberLayerContent(layer: ComposeSceneLayer, content: @Composable () -> Unit) {
    remember(layer, content) {
        layer.setContent(content)
    }
}

@Composable
private fun rememberDialogMeasurePolicy(
    layer: ComposeSceneLayer,
    properties: DialogProperties,
    containerSize: IntSize,
    platformInsets: PlatformInsets
) = remember(layer, properties, containerSize, platformInsets) {
    RootMeasurePolicy(
        platformInsets = platformInsets,
        usePlatformDefaultWidth = properties.usePlatformDefaultWidth
    ) { contentSize ->
        val positionWithInsets = positionWithInsets(platformInsets, containerSize) { sizeWithoutInsets ->
            sizeWithoutInsets.center - contentSize.center
        }
        layer.boundsInWindow = IntRect(positionWithInsets, contentSize)
        layer.calculateLocalPosition(positionWithInsets)
    }
}

private fun KeyEvent.isDismissRequest() =
    type == KeyEventType.KeyDown && key == Key.Escape

internal fun getDialogScrimBlendMode(isWindowTransparent: Boolean) =
    if (isWindowTransparent) {
        // Use background alpha channel to respect transparent window shape.
        BlendMode.SrcAtop
    } else {
        BlendMode.SrcOver
    }
