/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering.viewport_offset

/**
 * The direction of a scroll, relative to the positive scroll offset axis given by an
 * [AxisDirection] and a [GrowthDirection].
 *
 * This contrasts to [GrowthDirection] in that it has a third value, [idle], for the case where no
 * scroll is occurring.
 *
 * This is used by [RenderSliverFloatingPersistentHeader] to only expand when the user is scrolling
 * in the same direction as the detected scroll offset change.
 */
enum class ScrollDirection {
    /** No scrolling is underway. */
    IDLE,

    /**
     * Scrolling is happening in the positive scroll offset direction.
     *
     * For example, for the [GrowthDirection.forward] part of a vertical [AxisDirection.down] list,
     * this means the content is moving up, exposing lower content.
     */
    FORWARD,

    /**
     * Scrolling is happening in the negative scroll offset direction.
     *
     * For example, for the [GrowthDirection.forward] part of a vertical [AxisDirection.down] list,
     * this means the content is moving down, exposing earlier content.
     */
    REVERSE
}

/**
 * Returns the opposite of the given [ScrollDirection].
 *
 * Specifically, returns [ScrollDirection.reverse] for [ScrollDirection.forward]  (and vice versa)
 * and returns [ScrollDirection.idle] for [ScrollDirection.idle].
 */
fun flipScrollDirection(direction: ScrollDirection): ScrollDirection =
    when (direction) {
        ScrollDirection.IDLE -> ScrollDirection.IDLE
        ScrollDirection.FORWARD -> ScrollDirection.REVERSE
        ScrollDirection.REVERSE -> ScrollDirection.FORWARD
    }
