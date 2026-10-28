/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapAndPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.launch

/** Required for the press and tap [MutableInteractionSource] consistency for TextField. */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.tapPressTextFieldModifier(
    interactionSource: MutableInteractionSource?,
    enabled: Boolean = true,
    onTap: (Offset) -> Unit,
): Modifier =
    if (enabled) {
        if (ComposeFoundationFlags.isNewTextFieldModifierImplementationEnabled) {
            this then TapPressTextFieldElement(interactionSource, onTap)
        } else {
            composed {
                val scope = rememberCoroutineScope()
                val pressedInteraction = remember { mutableStateOf<PressInteraction.Press?>(null) }
                val onTapState = rememberUpdatedState(onTap)
                DisposableEffect(interactionSource) {
                    onDispose {
                        pressedInteraction.value?.let { oldValue ->
                            val interaction = PressInteraction.Cancel(oldValue)
                            interactionSource?.tryEmit(interaction)
                            pressedInteraction.value = null
                        }
                    }
                }
                Modifier.pointerInput(interactionSource) {
                    detectTapAndPress(
                        onPress = {
                            scope.launch {
                                // Remove any old interactions if we didn't fire stop / cancel
                                // properly
                                pressedInteraction.value?.let { oldValue ->
                                    val interaction = PressInteraction.Cancel(oldValue)
                                    interactionSource?.emit(interaction)
                                    pressedInteraction.value = null
                                }
                                val interaction = PressInteraction.Press(it)
                                interactionSource?.emit(interaction)
                                pressedInteraction.value = interaction
                            }
                            val success = tryAwaitRelease()
                            scope.launch {
                                pressedInteraction.value?.let { oldValue ->
                                    val interaction =
                                        if (success) {
                                            PressInteraction.Release(oldValue)
                                        } else {
                                            PressInteraction.Cancel(oldValue)
                                        }
                                    interactionSource?.emit(interaction)
                                    pressedInteraction.value = null
                                }
                            }
                        },
                        onTap = { onTapState.value.invoke(it) },
                    )
                }
            }
        }
    } else {
        this
    }

private class TapPressTextFieldElement(
    private val interactionSource: MutableInteractionSource?,
    private val onTap: (Offset) -> Unit,
) : ModifierNodeElement<TapPressTextFieldNode>() {
    override fun create(): TapPressTextFieldNode = TapPressTextFieldNode(interactionSource, onTap)

    override fun update(node: TapPressTextFieldNode) {
        node.update(interactionSource, onTap)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "tapPressTextFieldModifier"
        properties["interactionSource"] = interactionSource
        properties["onTap"] = onTap
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TapPressTextFieldElement) return false
        if (interactionSource != other.interactionSource) return false
        if (onTap !== other.onTap) return false
        return true
    }

    override fun hashCode(): Int {
        var result = interactionSource?.hashCode() ?: 0
        result = 31 * result + onTap.hashCode()
        return result
    }
}

private class TapPressTextFieldNode(
    private var interactionSource: MutableInteractionSource?,
    private var onTap: (Offset) -> Unit,
) : DelegatingNode() {
    private var pressedInteraction: PressInteraction.Press? = null

    private val pointerInputNode =
        delegate(
            SuspendingPointerInputModifierNode {
                detectTapAndPress(
                    onPress = { offset ->
                        coroutineScope.launch {
                            // Remove any old interactions if we didn't fire stop / cancel properly
                            pressedInteraction?.let { oldValue ->
                                val interaction = PressInteraction.Cancel(oldValue)
                                interactionSource?.emit(interaction)
                                pressedInteraction = null
                            }
                            val interaction = PressInteraction.Press(offset)
                            interactionSource?.emit(interaction)
                            pressedInteraction = interaction
                        }
                        val success = tryAwaitRelease()
                        coroutineScope.launch {
                            pressedInteraction?.let { oldValue ->
                                val interaction =
                                    if (success) {
                                        PressInteraction.Release(oldValue)
                                    } else {
                                        PressInteraction.Cancel(oldValue)
                                    }
                                interactionSource?.emit(interaction)
                                pressedInteraction = null
                            }
                        }
                    },
                    onTap = { onTap.invoke(it) },
                )
            }
        )

    fun update(
        interactionSource: MutableInteractionSource?,
        onTap: (Offset) -> Unit,
    ) {
        this.onTap = onTap
        if (this.interactionSource != interactionSource) {
            disposeInteraction()
            this.interactionSource = interactionSource
            pointerInputNode.resetPointerInputHandler()
        }
    }

    override fun onDetach() {
        disposeInteraction()
    }

    private fun disposeInteraction() {
        pressedInteraction?.let { oldValue ->
            val interaction = PressInteraction.Cancel(oldValue)
            interactionSource?.tryEmit(interaction)
            pressedInteraction = null
        }
    }
}
