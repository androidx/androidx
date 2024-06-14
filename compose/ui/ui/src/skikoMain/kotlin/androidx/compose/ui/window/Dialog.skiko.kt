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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.LocalComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.SkiaBasedOwner
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/** The default scrim opacity. */
private const val DefaultScrimOpacity = 0.6f
private val DefaultScrimColor = Color.Black.copy(alpha = DefaultScrimOpacity)

/**
 * Properties used to customize the behavior of a [Dialog].
 *
 * @property dismissOnBackPress whether the popup can be dismissed by pressing the back button
 *     * on Android or escape key on desktop. If true, pressing the back button will call
 *       onDismissRequest.
 *
 * @property dismissOnClickOutside whether the dialog can be dismissed by clicking outside the
 *   dialog's bounds. If true, clicking outside the dialog will call onDismissRequest.
 * @property usePlatformDefaultWidth Whether the width of the dialog's content should be limited to
 *   the platform default, which is smaller than the screen width.
 * @property usePlatformInsets Whether the size of the dialog's content should be limited by
 *   platform insets.
 * @property useSoftwareKeyboardInset Whether the size of the dialog's content should be limited by
 *   software keyboard inset.
 * @property scrimColor Color of background fill.
 */
@Immutable
actual class DialogProperties
constructor(
    actual val dismissOnBackPress: Boolean = true,
    actual val dismissOnClickOutside: Boolean = true,
    actual val usePlatformDefaultWidth: Boolean = true,
    val usePlatformInsets: Boolean = true,
    val useSoftwareKeyboardInset: Boolean = true,
    val scrimColor: Color = DefaultScrimColor,
) {
    // Constructor with all non-experimental arguments.
    actual constructor(
        dismissOnBackPress: Boolean,
        dismissOnClickOutside: Boolean,
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
    DialogLayout(
        if (properties.dismissOnClickOutside) onDismissRequest else null,
        modifier = Modifier.drawBehind { drawRect(properties.scrimColor) },
        onPreviewKeyEvent = { false },
        onKeyEvent = {
            if (properties.dismissOnBackPress && it.isDismissRequest()) {
                onDismissRequest()
                true
            } else {
                false
            }
        },
        content = content
    )
}

@Composable
internal fun DialogLayout(
    onDismissRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
    onKeyEvent: ((KeyEvent) -> Boolean)?,
    content: @Composable () -> Unit
) {
    // TODO: Upstream ComposeScene refactor
    val scene = LocalComposeScene.current
    val density = LocalDensity.current

    val parentComposition = rememberCompositionContext()
    val (owner, composition) =
        remember {
            val owner =
                SkiaBasedOwner(
                    platformInputService = scene.platformInputService,
                    component = scene.component,
                    density = density,
                    coroutineContext = parentComposition.effectCoroutineContext,
                    isPopup = true,
                    isFocusable = true,
                    onDismissRequest = onDismissRequest,
                    onPreviewKeyEvent = onPreviewKeyEvent ?: { false },
                    onKeyEvent = onKeyEvent ?: { false }
                )
            scene.attach(owner)
            val composition =
                owner.setContent(parent = parentComposition) {
                    Layout(
                        content = content,
                        modifier = modifier,
                        measurePolicy = { measurables, constraints ->
                            val width = constraints.maxWidth
                            val height = constraints.maxHeight

                            layout(constraints.maxWidth, constraints.maxHeight) {
                                measurables.forEach {
                                    val placeable = it.measure(constraints)
                                    val position =
                                        Alignment.Center.align(
                                            size = IntSize(placeable.width, placeable.height),
                                            space = IntSize(width, height),
                                            layoutDirection = layoutDirection
                                        )
                                    owner.bounds =
                                        IntRect(
                                            position,
                                            IntSize(placeable.width, placeable.height)
                                        )
                                    placeable.place(position.x, position.y)
                                }
                            }
                        }
                    )
                }
            owner to composition
        }
    owner.density = density
    DisposableEffect(Unit) {
        onDispose {
            scene.detach(owner)
            composition.dispose()
            owner.dispose()
        }
    }
}

private fun KeyEvent.isDismissRequest() = type == KeyEventType.KeyDown && key == Key.Escape
