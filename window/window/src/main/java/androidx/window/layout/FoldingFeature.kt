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
package androidx.window.layout

/**
 * A feature that describes a fold in the flexible display
 * or a hinge between two physical display panels.
 *
 */
public interface FoldingFeature : DisplayFeature {

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

    /**
     * Returns [FoldingFeature.Orientation.HORIZONTAL] if the width is greater than the
     * height, [FoldingFeature.Orientation.VERTICAL] otherwise.
     */
    public val orientation: Orientation

    /**
     * Returns the [FoldingFeature.State] for the [FoldingFeature]
     */
    public val state: FoldingFeature.State
}
