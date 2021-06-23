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
package androidx.window

import android.graphics.Rect

/**
 * A feature that describes a fold in the flexible display
 * or a hinge between two physical display panels.
 *
 * @param [type] that is either [FoldingFeature.Type.FOLD] or [FoldingFeature.Type.HINGE]
 * @param [state] the physical state of the hinge that is either [FoldingFeature.State.FLAT] or
 * [FoldingFeature.State.HALF_OPENED]
 */
public class FoldingFeature internal constructor(
    /**
     * The bounding rectangle of the feature within the application window in the window
     * coordinate space.
     */
    private val featureBounds: Bounds,
    internal val type: Type,
    public val state: State
) : DisplayFeature {

    /**
     * Represents the type of hinge.
     */
    public class Type private constructor(private val description: String) {

        override fun toString(): String {
            return description
        }

        public companion object {
            /**
             * Represent a continuous screen that folds.
             */
            @JvmField
            public val FOLD: Type = Type("FOLD")

            /**
             * Represents a hinge connecting two separate display panels.
             */
            @JvmField
            public val HINGE: Type = Type("HINGE")

            internal fun from(value: Int): Type {
                return when (value) {
                    TYPE_FOLD -> FOLD
                    TYPE_HINGE -> HINGE
                    else -> throw IllegalArgumentException(
                        "${FoldingFeature::class.java.simpleName} incorrect type value"
                    )
                }
            }
        }
    }

    /**
     * Represents how the hinge might occlude content.
     */
    public class OcclusionType private constructor(private val description: String) {

        override fun toString(): String {
            return description
        }

        public companion object {
            /**
             * The [FoldingFeature] does not occlude the content in any way. One example is a flat
             * continuous fold where content can stretch across the fold. Another example is a hinge
             * that has width or height equal to 0. In this case the content is physically split
             * across both displays, but fully visible.
             */
            @JvmField
            public val NONE: OcclusionType = OcclusionType("NONE")

            /**
             * The [FoldingFeature] occludes all content. One example is a hinge that is considered
             * to be part of the window, so that part of the UI is not visible to the user.
             * Any content shown in the same area as the hinge may not be accessible in any way.
             * Fully occluded areas should always be avoided when placing interactive UI elements
             * and text.
             */
            @JvmField
            public val FULL: OcclusionType = OcclusionType("FULL")
        }
    }

    /**
     * Represents the axis for which the [FoldingFeature] runs parallel to.
     */
    public class Orientation private constructor(private val description: String) {

        override fun toString(): String {
            return description
        }

        public companion object {

            /**
             * The height of the [FoldingFeature] is greater than or equal to the width.
             */
            @JvmField
            public val VERTICAL: Orientation = Orientation("VERTICAL")

            /**
             * The width of the [FoldingFeature] is greater than the height.
             */
            @JvmField
            public val HORIZONTAL: Orientation = Orientation("HORIZONTAL")
        }
    }

    /**
     * Represents the [State] of the [FoldingFeature].
     */
    public class State private constructor(private val description: String) {

        override fun toString(): String {
            return description
        }

        public companion object {
            /**
             * The foldable device is completely open, the screen space that is presented to the
             * user is flat. See the
             * [Posture](https://developer.android.com/guide/topics/ui/foldables#postures)
             * section in the official documentation for visual samples and references.
             */
            @JvmField
            public val FLAT: State = State("FLAT")

            /**
             * The foldable device's hinge is in an intermediate position between opened and closed
             * state, there is a non-flat angle between parts of the flexible screen or between
             * physical screen panels. See the
             * [Posture](https://developer.android.com/guide/topics/ui/foldables#postures)
             * section in the official documentation for visual samples and references.
             */
            @JvmField
            public val HALF_OPENED: State = State("HALF_OPENED")
        }
    }

    override val bounds: Rect
        get() = featureBounds.toRect()

    init {
        validateFeatureBounds(featureBounds)
    }

    public constructor(
        bounds: Rect,
        type: Type,
        state: State
    ) : this(Bounds(bounds), type, state)

    /**
     * Calculates if a [FoldingFeature] should be thought of as splitting the window into
     * multiple physical areas that can be seen by users as logically separate. Display panels
     * connected by a hinge are always separated. Folds on flexible screens should be treated as
     * separating when they are not [FoldingFeature.State.FLAT].
     *
     * Apps may use this to determine if content should lay out around the [FoldingFeature].
     * Developers should consider the placement of interactive elements. Similar to the case of
     * [FoldingFeature.OcclusionType.FULL], when a feature is separating then consider laying
     * out the controls around the [FoldingFeature].
     *
     * An example use case is to determine if the UI should be split into two logical areas. A
     * media app where there is some auxiliary content, such as comments or description of a video,
     * may need to adapt the layout. The media can be put on one side of the [FoldingFeature] and
     * the auxiliary content can be placed on the other side.
     *
     * @return `true` if the feature splits the display into two areas, `false`
     * otherwise.
     */
    public val isSeparating: Boolean
        get() = when {
            type == Type.HINGE -> true
            type == Type.FOLD && state == State.HALF_OPENED -> true
            else -> false
        }

    /**
     * Calculates the occlusion mode to determine if a [FoldingFeature] occludes a part of
     * the window. This flag is useful for determining if UI elements need to be moved
     * around so that the user can access them. For some devices occluded elements can not be
     * accessed by the user at all.
     *
     * For occlusion type [FoldingFeature.OcclusionType.NONE] the feature can be treated as a
     * guideline. One example would be for a continuously folding screen. For occlusion type
     * [FoldingFeature.OcclusionType.FULL] the feature should be avoided completely since content
     * will not be visible or touchable, like a hinge device with two displays.
     *
     * The occlusion mode is useful to determine if the UI needs to adapt to the
     * [FoldingFeature]. For example, full screen games should consider avoiding anything in
     * the occluded region if it negatively affects the gameplay.  The user can not tap
     * on the occluded interactive UI elements nor can they see important information.
     *
     * @return [FoldingFeature.OcclusionType.NONE] if the [FoldingFeature] has empty
     * bounds.
     */
    public val occlusionType: OcclusionType
        get() = if (featureBounds.width == 0 || featureBounds.height == 0) {
            OcclusionType.NONE
        } else {
            OcclusionType.FULL
        }

    /**
     * Returns [FoldingFeature.Orientation.HORIZONTAL] if the width is greater than the
     * height, [FoldingFeature.Orientation.VERTICAL] otherwise.
     */
    public val orientation: Orientation
        get() {
            return if (featureBounds.width > featureBounds.height) {
                Orientation.HORIZONTAL
            } else {
                Orientation.VERTICAL
            }
        }

    override fun toString(): String {
        return (
            "${FoldingFeature::class.java.simpleName} { $featureBounds, " +
                "type=$type, state=$state }"
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FoldingFeature

        if (featureBounds != other.featureBounds) return false
        if (type != other.type) return false
        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        var result = featureBounds.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + state.hashCode()
        return result
    }

    public companion object {
        /**
         * A fold in the flexible screen without a physical gap.
         */
        public const val TYPE_FOLD: Int = 1

        /**
         * A physical separation with a hinge that allows two display panels to fold.
         */
        public const val TYPE_HINGE: Int = 2

        /**
         * The foldable device is completely open, the screen space that is presented to the user
         * is flat. See the
         * [Posture](https://developer.android.com/guide/topics/ui/foldables#postures)
         * section in the official documentation for visual samples and references.
         */
        public const val STATE_FLAT: Int = 1

        /**
         * The foldable device's hinge is in an intermediate position between opened and closed
         * state, there is a non-flat angle between parts of the flexible screen or between
         * physical screen panels. See the
         * [Posture](https://developer.android.com/guide/topics/ui/foldables#postures)
         * section in the official documentation for visual samples and references.
         */
        public const val STATE_HALF_OPENED: Int = 2

        /**
         * The [FoldingFeature] does not occlude the content in any way. One example is a flat
         * continuous fold where content can stretch across the fold. Another example is a hinge
         * that has width or height equal to 0. In this case the content is physically split across
         * both displays, but fully visible.
         */
        public const val OCCLUSION_NONE: Int = 0

        /**
         * The [FoldingFeature] occludes all content. One example is a hinge that is considered to
         * be part of the window, so that part of the UI is not visible to the user. Any content
         * shown in the same area as the hinge may not be accessible in any way. Fully occluded
         * areas should always be avoided when placing interactive UI elements and text.
         */
        public const val OCCLUSION_FULL: Int = 1

        /**
         * The height of the [FoldingFeature] is greater than or equal to the width.
         */
        public const val ORIENTATION_VERTICAL: Int = 0

        /**
         * The width of the [FoldingFeature] is greater than the height.
         */
        public const val ORIENTATION_HORIZONTAL: Int = 1

        /**
         * Verifies the bounds of the folding feature.
         */
        internal fun validateFeatureBounds(bounds: Bounds) {
            require(!(bounds.width == 0 && bounds.height == 0)) { "Bounds must be non zero" }
            require(!(bounds.left != 0 && bounds.top != 0)) {
                "Bounding rectangle must start at the top or left window edge for folding features"
            }
        }
    }
}
