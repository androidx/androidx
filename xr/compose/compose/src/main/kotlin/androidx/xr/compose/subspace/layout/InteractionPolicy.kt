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

package androidx.xr.compose.subspace.layout

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.node.CompositionLocalConsumerSubspaceModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.node.currentValueOf
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InputEvent.Action
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.scene
import java.util.function.Consumer
import kotlin.hashCode

/**
 * Defines the interaction policy for a spatial object. This policy enables reacting to a user's
 * spatial inputs.
 *
 * @property isEnabled Whether an interaction policy is enabled for this object. If `false`, spatial
 *   interaction input events will not returned.
 * @property onInputEvent Raw event executed with every input update.
 */
public class InteractionPolicy(
    public val isEnabled: Boolean = true,
    public val onInputEvent: ((SpatialInputEvent) -> Unit),
) {
    public companion object {

        /**
         * An [InteractionPolicy] that detects only click inputs
         *
         * @param isEnabled Whether an interaction policy is enabled for this object. If `false`,
         *   click events will not be returned.
         * @param onClick Executed after a click occurs.
         * @return an [InteractionPolicy] that filters for click events.
         */
        public fun clickable(isEnabled: Boolean = true, onClick: () -> Unit): InteractionPolicy =
            InteractionPolicy(
                isEnabled = isEnabled,
                onInputEvent = { event ->
                    if (event.action == Action.UP && event.hitPosition != null) {
                        onClick()
                    }
                },
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InteractableElement) return false
        if (isEnabled != other.enabled) return false
        if (onInputEvent !== other.onInputEvent) return false
        return true
    }

    override fun hashCode(): Int {
        var result = isEnabled.hashCode()
        result = 31 * result + onInputEvent.hashCode()
        return result
    }

    override fun toString(): String {
        return "InteractionPolicy(isEnabled=$isEnabled, onInputEvent=$onInputEvent)"
    }
}

internal fun SubspaceModifier.interactable(
    enabled: Boolean = true,
    onInputEvent: ((SpatialInputEvent) -> Unit)? = null,
): SubspaceModifier = this.then(InteractableElement(enabled, onInputEvent))

private class InteractableElement(
    val enabled: Boolean = true,
    val onInputEvent: ((SpatialInputEvent) -> Unit)? = null,
) : SubspaceModifierNodeElement<InteractableNode>() {
    override fun create(): InteractableNode =
        InteractableNode(enabled = enabled, onInputEvent = onInputEvent)

    override fun update(node: InteractableNode) {
        node.enabled = enabled
        node.onInputEvent = onInputEvent
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InteractableElement) return false
        if (enabled != other.enabled) return false
        if (onInputEvent !== other.onInputEvent) return false
        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + onInputEvent.hashCode()
        return result
    }
}

internal class InteractableNode(
    var enabled: Boolean,
    var onClick: (() -> Unit)? = null,
    var onInputEvent: ((SpatialInputEvent) -> Unit)? = null,
) :
    SubspaceModifier.Node(),
    CompositionLocalConsumerSubspaceModifierNode,
    CoreEntityNode,
    Consumer<InputEvent> {
    private inline val density: Density
        get() = currentValueOf(LocalDensity)

    private inline val session: Session
        get() = checkNotNull(currentValueOf(LocalSession)) { "Interactable requires a Session." }

    private var component: InteractableComponent? = null

    override fun CoreEntityScope.modifyCoreEntity() {
        // For disabling and enabling component on recomposition.
        updateState()
    }

    override fun onAttach() {
        super.onAttach()
        updateState()
    }

    override fun onDetach() {
        if (component != null) {
            disableComponent()
        }
    }

    /** Updates the movable state of this CoreEntity. */
    private fun updateState() {
        if (coreEntity !is InteractableCoreEntity) {
            return
        }
        // Enabled is on the Node. It means "should be enabled" for the Component.
        if (enabled && component == null) {
            enableComponent()
        } else if (!enabled && component != null) {
            disableComponent()
        }
    }

    /** Enables the InteractableComponent and anchorPlacement for this CoreEntity. */
    private fun enableComponent() {
        check(component == null) { "InteractableComponent already enabled." }
        component = InteractableComponent.create(session = session, inputEventListener = this)

        check(component?.let { coreEntity.addComponent(it) } == true) {
            "Could not add InteractableComponent to Core Entity."
        }
    }

    /**
     * Disables the InteractableComponent for this CoreEntity. Takes care of life cycle tasks for
     * the underlying component in SceneCore.
     */
    private fun disableComponent() {
        check(component != null) { "InteractableComponent already disabled." }
        component?.let { coreEntity.removeComponent(it) }
        component = null
    }

    override fun accept(event: InputEvent) {
        val localizedHitPosition =
            event.hitInfoList.firstOrNull()?.let { hitInfo ->
                val hitPosition = hitInfo.hitPosition
                if (hitPosition != null) {
                    session.scene.activitySpace
                        .transformPoseTo(Pose(translation = hitPosition), hitInfo.inputEntity)
                        .convertMetersToPixels(density)
                        .translation
                } else {
                    null
                }
            }

        if (event.action == Action.UP && localizedHitPosition != null) {
            onClick?.invoke()
        }

        onInputEvent?.invoke(
            SpatialInputEvent(
                source = event.source,
                action = event.action,
                pointerType = event.pointerType,
                timestamp = event.timestamp,
                hitPosition = localizedHitPosition,
                origin = event.origin.convertMetersToPixels(density),
                direction = event.direction.convertMetersToPixels(density),
            )
        )
    }
}
