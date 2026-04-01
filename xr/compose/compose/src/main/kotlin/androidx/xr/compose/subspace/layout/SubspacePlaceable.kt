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

package androidx.xr.compose.subspace.layout

import androidx.compose.ui.unit.LayoutDirection
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose

/**
 * A [SubspacePlaceable] corresponds to a child layout that can be positioned by its parent layout.
 * Most [SubspacePlaceable] are the result of a [SubspaceMeasurable.measure] call.
 *
 * Based on [androidx.compose.ui.layout.Placeable].
 */
public abstract class SubspacePlaceable {
    /**
     * The width, in pixels, of the measured layout, as seen by the parent. This will usually
     * coincide with the measured width of the layout (aka the `width` value passed into
     * [SubspaceMeasureScope.layout]), but can be different if the layout does not respect its
     * incoming constraints: in these cases the width will be coerced inside the min and max width
     * constraints - to access the actual width that the layout measured itself to, use
     * [measuredWidth].
     */
    public var width: Int = 0
        private set

    /**
     * The height, in pixels, of the measured layout, as seen by the parent. This will usually
     * coincide with the measured height of the layout (aka the `height` value passed into
     * [SubspaceMeasureScope.layout]), but can be different if the layout does not respect its
     * incoming constraints: in these cases the height will be coerced inside the min and max height
     * constraints - to access the actual height that the layout measured itself to, use
     * [measuredHeight].
     */
    public var height: Int = 0
        private set

    /**
     * The depth, in pixels, of the measured layout, as seen by the parent. This will usually
     * coincide with the measured depth of the layout (aka the `depth` value passed into
     * [SubspaceMeasureScope.layout]), but can be different if the layout does not respect its
     * incoming constraints: in these cases the depth will be coerced inside the min and max depth
     * constraints - to access the actual depth that the layout measured itself to, use
     * [measuredDepth].
     */
    public var depth: Int = 0
        private set

    /** The constraints used for the measurement made to obtain this [SubspacePlaceable]. */
    protected var measurementConstraints: VolumeConstraints = VolumeConstraints()
        set(value) {
            if (field != value) {
                field = value
                onMeasuredSizeChanged()
            }
        }

    /** The measured width of the layout. This might not respect [measurementConstraints]. */
    public var measuredWidth: Int = 0
        protected set(value) {
            if (field != value) {
                field = value
                onMeasuredSizeChanged()
            }
        }

    /** The measured height of the layout. This might not respect [measurementConstraints]. */
    public var measuredHeight: Int = 0
        protected set(value) {
            if (field != value) {
                field = value
                onMeasuredSizeChanged()
            }
        }

    /** The measured depth of the layout. This might not respect [measurementConstraints]. */
    public var measuredDepth: Int = 0
        protected set(value) {
            if (field != value) {
                field = value
                onMeasuredSizeChanged()
            }
        }

    private fun onMeasuredSizeChanged() {
        width =
            measuredWidth.coerceIn(measurementConstraints.minWidth, measurementConstraints.maxWidth)
        height =
            measuredHeight.coerceIn(
                measurementConstraints.minHeight,
                measurementConstraints.maxHeight,
            )
        depth =
            measuredDepth.coerceIn(measurementConstraints.minDepth, measurementConstraints.maxDepth)
    }

    /** Positions the [SubspacePlaceable] at [pose] in its parent's coordinate system. */
    protected abstract fun placeAt(pose: Pose)

    /** Receiver scope that permits explicit placement of a [SubspacePlaceable]. */
    public abstract class SubspacePlacementScope {
        /**
         * Keeps the layout direction of the parent of the subspace placeable that is being placed
         * using current [SubspacePlacementScope]. Used to support automatic position mirroring for
         * convenient RTL support in custom layouts.
         */
        protected abstract val parentLayoutDirection: LayoutDirection

        /**
         * The [SubspaceLayoutCoordinates] of this layout, if known or `null` if the layout hasn't
         * been placed yet.
         */
        public open val coordinates: SubspaceLayoutCoordinates?
            get() = null

        /**
         * Place a [SubspacePlaceable] at the [Pose] in its parent's coordinate system.
         *
         * @param pose The pose of the layout.
         */
        public fun SubspacePlaceable.place(pose: Pose) {
            placeAt(pose)
        }

        /**
         * Place a [SubspacePlaceable] at the [Pose] in its parent's coordinate system with auto
         * mirrored position along YZ plane if parent layout direction is [LayoutDirection.Rtl].
         *
         * If the [parentLayoutDirection] is [LayoutDirection.Rtl], this function calculates a new
         * pose by mirroring the original [pose] across the YZ plane. This ensures that layouts
         * designed for LTR behave intuitively when the locale is switched to RTL.
         *
         * The mirroring transformation involves:
         * 1. Translation: The `x` component of the translation vector is negated (`x -> -x`). This
         *    moves the object from the right side to the left side, or vice versa.
         * 2. Rotation: The `y` and `z` components of the underlying quaternion are negated. This
         *    has the effect of reversing the direction of yaw (rotation around the Y-axis) and roll
         *    (rotation around the Z-axis), while leaving pitch (rotation around the X-axis)
         *    unchanged.
         *
         * For example, a pose that places an object 20 dp to the left and yawed 30 degrees to the
         * right will be transformed to place the object 20 dp to the right and yawed 30 degrees to
         * the left.
         *
         * If the layout direction is LTR, the original [pose] is used without modification.
         *
         * This API is not mirroring the internal mesh geometry of 3D models. This function only
         * affects pose of the layout.
         *
         * @param pose The pose of the layout.
         */
        public fun SubspacePlaceable.placeRelative(pose: Pose) {
            var newPose = pose
            if (parentLayoutDirection == LayoutDirection.Rtl) {
                newPose =
                    Pose(
                        translation = pose.translation.copy(x = -pose.translation.x),
                        rotation = pose.rotation.copy(y = -pose.rotation.y, z = -pose.rotation.z),
                    )
            }
            placeAt(newPose)
        }
    }
}
