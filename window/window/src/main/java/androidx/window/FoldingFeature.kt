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
import androidx.annotation.IntDef
import androidx.window.FoldingFeature.Companion.ORIENTATION_VERTICAL as ORIENTATION_VERTICAL1

/**
 * A feature that describes a fold in the flexible display
 * or a hinge between two physical display panels.
 *
 * @param [type] that is either [FoldingFeature.TYPE_FOLD] or [FoldingFeature.TYPE_HINGE]
 * @param [state] the physical state of the hinge that is either [FoldingFeature.STATE_FLAT] or
 * [FoldingFeature.STATE_HALF_OPENED]
 */
public class FoldingFeature internal constructor(
    /**
     * The bounding rectangle of the feature within the application window in the window
     * coordinate space.
     */
    private val featureBounds: Bounds,
    @Type internal val type: Int,
    @State public val state: Int
) : DisplayFeature {

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(TYPE_FOLD, TYPE_HINGE)
    internal annotation class Type

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(OCCLUSION_NONE, OCCLUSION_FULL)
    internal annotation class OcclusionType

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL1)
    internal annotation class Orientation

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(STATE_HALF_OPENED, STATE_FLAT)
    internal annotation class State

    override val bounds: Rect
        get() = featureBounds.toRect()

    init {
        validateState(state)
        validateType(type)
        validateFeatureBounds(featureBounds)
    }

    public constructor(
        bounds: Rect,
        type: Int,
        state: Int
    ) : this(Bounds(bounds), type, state)

    /**
     * Calculates if a [FoldingFeature] should be thought of as splitting the window into
     * multiple physical areas that can be seen by users as logically separate. Display panels
     * connected by a hinge are always separated. Folds on flexible screens should be treated as
     * separating when they are not [FoldingFeature.STATE_FLAT].
     *
     * Apps may use this to determine if content should lay out around the [FoldingFeature].
     * Developers should consider the placement of interactive elements. Similar to the case of
     * [FoldingFeature.OCCLUSION_FULL], when a feature is separating then consider laying
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
            type == TYPE_HINGE -> true
            type == TYPE_FOLD && state == STATE_HALF_OPENED -> true
            else -> false
        }

    /**
     * Calculates the occlusion mode to determine if a [FoldingFeature] occludes a part of
     * the window. This flag is useful for determining if UI elements need to be moved
     * around so that the user can access them. For some devices occluded elements can not be
     * accessed by the user at all.
     *
     * For occlusion type [FoldingFeature.OCCLUSION_NONE] the feature can be treated as a
     * guideline. One example would be for a continuously folding screen. For occlusion type
     * [FoldingFeature.OCCLUSION_FULL] the feature should be avoided completely since content
     * will not be visible or touchable, like a hinge device with two displays.
     *
     * The occlusion mode is useful to determine if the UI needs to adapt to the
     * [FoldingFeature]. For example, full screen games should consider avoiding anything in
     * the occluded region if it negatively affects the gameplay.  The user can not tap
     * on the occluded interactive UI elements nor can they see important information.
     *
     * @return [FoldingFeature.OCCLUSION_NONE] if the [FoldingFeature] has empty
     * bounds.
     */
    @get:OcclusionType
    public val occlusionMode: Int
        get() = if (featureBounds.width == 0 || featureBounds.height == 0) {
            OCCLUSION_NONE
        } else {
            OCCLUSION_FULL
        }

    /**
     * Returns [FoldingFeature.ORIENTATION_HORIZONTAL] if the width is greater than the
     * height, [FoldingFeature.ORIENTATION_VERTICAL] otherwise.
     */
    @get:Orientation
    public val orientation: Int
        get() {
            return if (featureBounds.width > featureBounds.height) {
                ORIENTATION_HORIZONTAL
            } else {
                ORIENTATION_VERTICAL
            }
        }

    override fun toString(): String {
        return (
            "${FoldingFeature::class.java.simpleName} { $featureBounds, " +
                "type=${typeToString(type)}, state=${stateToString(state)} }"
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
        result = 31 * result + type
        result = 31 * result + state
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

        internal fun occlusionTypeToString(@OcclusionType type: Int): String {
            return when (type) {
                OCCLUSION_NONE -> "OCCLUSION_NONE"
                OCCLUSION_FULL -> "OCCLUSION_FULL"
                else -> "UNKNOWN"
            }
        }

        internal fun orientationToString(@Orientation direction: Int): String {
            return when (direction) {
                ORIENTATION_HORIZONTAL -> "ORIENTATION_HORIZONTAL"
                ORIENTATION_VERTICAL1 -> "ORIENTATION_VERTICAL"
                else -> "UNKNOWN"
            }
        }

        /**
         * Verifies the state is [FoldingFeature.STATE_FLAT] or
         * [FoldingFeature.STATE_HALF_OPENED].
         */
        internal fun validateState(state: Int) {
            require(!(state != STATE_FLAT && state != STATE_HALF_OPENED)) {
                "State must be either ${stateToString(STATE_FLAT)} or " +
                    stateToString(STATE_HALF_OPENED)
            }
        }

        /**
         * Verifies the type is either [FoldingFeature.TYPE_HINGE] or
         * [FoldingFeature.TYPE_FOLD]
         */
        internal fun validateType(type: Int) {
            require(!(type != TYPE_FOLD && type != TYPE_HINGE)) {
                "Type must be either ${typeToString(TYPE_FOLD)} or ${typeToString(TYPE_HINGE)}"
            }
        }

        /**
         * Verifies the bounds of the folding feature.
         */
        internal fun validateFeatureBounds(bounds: Bounds) {
            require(!(bounds.width == 0 && bounds.height == 0)) { "Bounds must be non zero" }
            require(!(bounds.left != 0 && bounds.top != 0)) {
                "Bounding rectangle must start at the top or left window edge for folding features"
            }
        }

        internal fun typeToString(type: Int): String {
            return when (type) {
                TYPE_FOLD -> "FOLD"
                TYPE_HINGE -> "HINGE"
                else -> "Unknown feature type ($type)"
            }
        }

        internal fun stateToString(state: Int): String {
            return when (state) {
                STATE_FLAT -> "FLAT"
                STATE_HALF_OPENED -> "HALF_OPENED"
                else -> "Unknown feature state ($state)"
            }
        }
    }
}
