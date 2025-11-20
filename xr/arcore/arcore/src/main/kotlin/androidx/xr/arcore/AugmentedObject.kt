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

package androidx.xr.arcore

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.Anchor
import androidx.xr.arcore.runtime.AnchorResourcesExhaustedException
import androidx.xr.arcore.runtime.AugmentedObject as RuntimeObject
import androidx.xr.runtime.AugmentedObjectCategory as Category
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform

/**
 * A representation of a physical object in real space.
 *
 * Augmented Objects are detected by the XR system and provide information about their pose,
 * extents, and label.
 *
 * The pose represents the position and orientation of the center point of the object.
 *
 * The extents describe the size of the object, as axis-aligned half-widths.
 *
 * The label is an instance of [androidx.xr.runtime.AugmentedObjectCategory] that describes what the
 * object is.
 */
public class AugmentedObject
internal constructor(
    internal val runtimeObject: RuntimeObject,
    private val xrResourceManager: XrResourcesManager,
) : Trackable<AugmentedObject.State>, Updatable {
    public companion object {
        /**
         * Subscribes to a flow of [AugmentedObject]s.
         *
         * The flow emits a new collection of [AugmentedObject]s whenever the underlying XR system
         * detects new objects or updates the state of existing ones. This typically happens on each
         * frame update of the XR system.
         *
         * @param session The [Session] to subscribe to.
         * @throws IllegalStateException if the given [Session]'s [Config.augmentedObjectCategories]
         *   is empty.
         * @sample androidx.xr.arcore.samples.getAugmentedObjects
         */
        @JvmStatic
        public fun subscribe(session: Session): StateFlow<Collection<AugmentedObject>> {
            check(!session.config.augmentedObjectCategories.isEmpty()) {
                "No AugmentedObject categories have been configured"
            }

            return session.state
                .transform { state ->
                    state.perceptionState?.let { perceptionState ->
                        emit(perceptionState.trackables.filterIsInstance<AugmentedObject>())
                    }
                }
                .stateIn(
                    session.coroutineScope,
                    SharingStarted.Eagerly,
                    session.state.value.perceptionState
                        ?.trackables
                        ?.filterIsInstance<AugmentedObject>() ?: emptyList(),
                )
        }
    }

    /** The representation of the current state of an [AugmentedObject]. */
    public class State
    internal constructor(
        public override val trackingState: TrackingState,
        /**
         * * The category of the augmented object.
         *
         * @see Category
         */
        public val category: Category,
        /**
         * The [Pose] determined to represent the center of this object.
         *
         * This value may or may not overlap with the object's center of gravity.
         */
        public val centerPose: Pose,
        /**
         * The dimensions of the object, axis aligned relative to the center pose. These values
         * represent the full length of the specific axis.
         */
        public val extents: FloatSize3d,
    ) : Trackable.State {}

    private val _state =
        MutableStateFlow(
            State(
                runtimeObject.trackingState,
                runtimeObject.category,
                runtimeObject.centerPose,
                runtimeObject.extents,
            )
        )

    /**
     * A [StateFlow] that contains the latest [State] of the [AugmentedObject].
     *
     * @sample androidx.xr.arcore.samples.getAugmentedObjects
     */
    public override val state: StateFlow<State> = _state.asStateFlow()

    /**
     * This function is used by the runtime to propagate internal state changes. It is not intended
     * to be called directly by a developer.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override suspend fun update() {
        _state.emit(
            State(
                runtimeObject.trackingState,
                runtimeObject.category,
                runtimeObject.centerPose,
                runtimeObject.extents,
            )
        )
    }

    /**
     * Creates an [androidx.xr.arcore.runtime.Anchor] that is attached to this trackable, using the
     * given initial [pose].
     *
     * @throws [IllegalStateException] if [Session.config.augmentedObjectCategories] is empty.
     */
    override fun createAnchor(pose: Pose): AnchorCreateResult {
        check(!xrResourceManager.lifecycleManager.config.augmentedObjectCategories.isEmpty()) {
            "Config.augmentedObjectCategories is empty."
        }

        val runtimeAnchor: Anchor
        try {
            runtimeAnchor = runtimeObject.createAnchor(pose)
        } catch (e: AnchorResourcesExhaustedException) {
            return AnchorCreateResourcesExhausted()
        }
        val anchor = Anchor(runtimeAnchor, xrResourceManager)
        xrResourceManager.addUpdatable(anchor)
        return AnchorCreateSuccess(anchor)
    }
}
