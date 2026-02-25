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
 *
 * @property state a [StateFlow] that contains the latest [State] of the AugmentedObject
 */
public class AugmentedObject
internal constructor(
    internal val runtimeObject: RuntimeObject,
    private val xrResourceManager: XrResourcesManager,
) : Trackable<AugmentedObject.State>, Updatable {
    public companion object {
        /**
         * Subscribes to a flow of AugmentedObjects.
         *
         * The flow emits a new collection of AugmentedObjects whenever the underlying XR system
         * detects new objects or updates the state of existing ones. This typically happens on each
         * frame update of the XR system.
         *
         * @param session the [Session] to subscribe to
         * @return a [StateFlow] that emits a collection of AugmentedObjects
         * @throws IllegalStateException if the given [Session]'s [Config.augmentedObjectCategories]
         *   is empty
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

    /**
     * The representation of the current state of an AugmentedObject.
     *
     * @property trackingState the [TrackingState] of the object
     * @property category the [Category] of the augmented object
     * @property centerPose the [Pose] determined to represent the center of this object
     * @property extents the dimensions of the object, axis aligned relative to the center pose,
     *   representing the full length of the specific axis
     */
    public class State
    internal constructor(
        public override val trackingState: TrackingState,
        public val category: Category,
        public val centerPose: Pose,
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
}
