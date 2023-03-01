/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass.Main
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.modifier.ProvidableModifierLocal
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.LocalPointerIconService
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Represents a pointer icon to use in [Modifier.pointerHoverIcon]
 */
@Stable
interface PointerIcon {

    /**
     * A collection of common pointer icons used for the mouse cursor. These icons will be used to
     * assign default pointer icons for various widgets.
     */
    companion object {

        /** The default arrow icon that is commonly used for cursor icons. */
        val Default = pointerIconDefault

        /** Commonly used when selecting precise portions of the screen. */
        val Crosshair = pointerIconCrosshair

        /** Also called an I-beam cursor, this is commonly used on selectable or editable text. */
        val Text = pointerIconText

        /** Commonly used to indicate to a user that an element is clickable. */
        val Hand = pointerIconHand
    }
}

internal expect val pointerIconDefault: PointerIcon
internal expect val pointerIconCrosshair: PointerIcon
internal expect val pointerIconText: PointerIcon
internal expect val pointerIconHand: PointerIcon

internal interface PointerIconService {
    fun getIcon(): PointerIcon
    fun setIcon(value: PointerIcon?)
}

/**
 * Modifier that lets a developer define a pointer icon to display when the cursor is hovered over
 * the element. When [overrideDescendants] is set to true, children cannot override the pointer icon
 * using this modifier.
 *
 * @sample androidx.compose.ui.samples.PointerIconSample
 *
 * @param icon The icon to set
 * @param overrideDescendants when false (by default) descendants are able to set their own pointer
 * icon. If true, all children under this parent will receive the requested pointer [icon] and are
 * no longer allowed to override their own pointer icon.
 */
@Stable
fun Modifier.pointerHoverIcon(icon: PointerIcon, overrideDescendants: Boolean = false) =
    composed(inspectorInfo = debugInspectorInfo {
        name = "pointerHoverIcon"
        properties["icon"] = icon
        properties["overrideDescendants"] = overrideDescendants
    }) {
        val pointerIconService = LocalPointerIconService.current
        if (pointerIconService == null) {
            Modifier
        } else {
            val onSetIcon = { pointerIcon: PointerIcon? ->
                pointerIconService.setIcon(pointerIcon)
            }
            val pointerIconModifierLocal = remember {
                PointerIconModifierLocal(icon, overrideDescendants, onSetIcon)
            }
            SideEffect {
                pointerIconModifierLocal.updateValues(
                    icon = icon,
                    overrideDescendants = overrideDescendants,
                    onSetIcon = onSetIcon
                )
            }
            val pointerInputModifier = if (pointerIconModifierLocal.shouldUpdatePointerIcon()) {
                pointerInput(pointerIconModifierLocal) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(Main)

                            if (event.type == PointerEventType.Enter) {
                                pointerIconModifierLocal.enter()
                            } else if (event.type == PointerEventType.Exit) {
                                pointerIconModifierLocal.exit()
                            }
                        }
                    }
                }
            } else {
                Modifier
            }

            pointerIconModifierLocal.then(pointerInputModifier)
        }
    }

/**
 * Handles storing all pointer icon information that needs to be passed between Modifiers to
 * determine which icon needs to be set in the hierarchy.
 *
 * @property icon the stored current icon we are keeping track of.
 * @property overrideDescendants value indicating whether the stored icon should always be
 * respected by its children. If true, the stored icon will be considered the source of truth for
 * all children. If false, the stored icon can be overwritten by a child.
 * @property onSetIcon is a lambda that will handle the process of physically setting the user
 * facing pointer icon. This allows the [PointerIconModifierLocal] to be solely responsible for
 * determining what the state of the icon should be, but removes the responsibility of needing to
 * actually set the icon for the user.
 */
private class PointerIconModifierLocal(
    private var icon: PointerIcon,
    private var overrideDescendants: Boolean,
    private var onSetIcon: (PointerIcon?) -> Unit,
) : PointerIcon, ModifierLocalProvider<PointerIconModifierLocal?>, ModifierLocalConsumer {
    // TODO: (b/266976920) Remove making this a mutable state once we fully support a dynamic
    //  overrideDescendants param.
    private var parentInfo: PointerIconModifierLocal? by mutableStateOf(null)

    // TODO: (b/267170292) Properly reset isPaused upon PointerIconModifierLocal disposal.
    var isPaused: Boolean = false

    /* True if the cursor is within the surface area of this element's bounds. Otherwise, false. */
    var isHovered: Boolean = false

    override val key: ProvidableModifierLocal<PointerIconModifierLocal?> = ModifierLocalPointerIcon
    override val value: PointerIconModifierLocal = this

    override fun onModifierLocalsUpdated(scope: ModifierLocalReadScope) = with(scope) {
        val oldParentInfo = parentInfo
        parentInfo = ModifierLocalPointerIcon.current
        if (oldParentInfo != null && parentInfo == null) {
            // When the old parentInfo for this element is reassigned to null, we assume this
            // element is being alienated for disposal. Exit out of our pointer icon logic for this
            // element and then update onSetIcon to null so it will not change the icon any further.
            exit(oldParentInfo)
            onSetIcon = {}
        }
    }

    fun shouldUpdatePointerIcon(): Boolean {
        val parentPointerInfo = parentInfo
        return parentPointerInfo == null || !parentPointerInfo.hasOverride()
    }

    private fun hasOverride(): Boolean {
        return overrideDescendants || parentInfo?.hasOverride() == true
    }

    fun enter() {
        isHovered = true
        if (!isPaused) {
            parentInfo?.pause()
            onSetIcon(icon)
        }
    }

    fun exit() {
        exit(parentInfo)
    }

    private fun exit(parent: PointerIconModifierLocal?) {
        if (isHovered) {
            if (parent == null) {
                // Notify that oldest ancestor in hierarchy exited by passing null to onSetIcon().
                onSetIcon(null)
            } else {
                parent.reassignIcon()
            }
        }
        isHovered = false
    }

    private fun reassignIcon() {
        isPaused = false
        if (isHovered) {
            onSetIcon(icon)
        } else if (parentInfo == null) {
            // Reassign the icon back to the default arrow by passing in a null PointerIcon
            onSetIcon(null)
        } else {
            parentInfo?.reassignIcon()
        }
    }

    private fun pause() {
        isPaused = true
        parentInfo?.pause()
    }

    fun updateValues(
        icon: PointerIcon,
        overrideDescendants: Boolean,
        onSetIcon: (PointerIcon?) -> Unit
    ) {
        if (this.icon != icon && isHovered && !isPaused) {
            // Hovered element's icon has dynamically changed so we need to set the user facing icon
            onSetIcon(icon)
        }
        this.icon = icon
        this.overrideDescendants = overrideDescendants
        this.onSetIcon = onSetIcon
    }
}

/**
 * The unique identifier used as the key for the custom [ModifierLocalProvider] created to tell us
 * the current [PointerIcon].
 */
private val ModifierLocalPointerIcon = modifierLocalOf<PointerIconModifierLocal?> { null }