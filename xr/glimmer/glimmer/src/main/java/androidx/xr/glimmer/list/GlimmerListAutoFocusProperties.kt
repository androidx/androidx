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

package androidx.xr.glimmer.list

/**
 * Keeps the auto focus calculations from the previous measure pass for use in the next measure one.
 *
 *         Full content
 *             size
 *        _____________  <- content start
 *       |             |
 *       |  threshold  |   Area where the focus lines moves
 *       |    area     | (between viewport start and center).
 *       |    (top)    |
 *       |             |
 *       |◦◦◦◦◦◦◦◦◦◦◦◦◦| <- scrollThreshold
 *       |             |
 *      _|_____________|_  <- list viewport start
 *     | |             | |
 *     | |    focus    | |
 *     | |    line     | | Between `scrollThreshold` and `content_end - scrollThreshold`
 *     | |    moves    | | the focus line is always in the center of the list viewport.
 *     | |    within   | |
 *     | |   viewport  | |
 *     |_|_____________|_| <- list viewport end
 *       |             |
 *       |             |
 *       |◦◦◦◦◦◦◦◦◦◦◦◦◦| <- (content_end - scrollThreshold)
 *       |             |
 *       |  threshold  |   Area where the focus lines moves
 *       |    area     | (between viewport center and bottom).
 *       |  (bottom)   |
 *       |             |
 *       |_____________| <- content end
 * * `contentScroll = list_viewport_start - content_start`
 * * `focusScroll = focus_line - list_viewport_start`
 * * `userScroll = contentScroll + focusScroll = focus_line - content_start`
 */
internal class GlimmerListAutoFocusProperties(
    /**
     * This is an estimate of how far a user should scroll backward to bring the content and focus
     * line to the top. This value is always the sum of [focusScroll] and [contentScroll]. We need
     * it to correctly calculate the next [contentScroll] value, since transformation is non-linear
     * and depends on the previous value. The maximum value of [userScroll] is always equal to the
     * [contentLength].
     */
    val userScroll: Float,
    /**
     * The position of the focus line relative to the start of the viewport and it doesn't include
     * the paddings.
     *
     * E.g., if the list size is 100dp and the paddings are 20dp, then the minimum value of
     * [focusScroll] would be 0dp and the maximum value would be 60dp.
     */
    val focusScroll: Float,
    /**
     * This is an estimate of the content beyond the top of viewport, measured as the distance
     * between the top of the content and the top of the viewport.
     *
     * E.g., if the list size is 100dp, the paddings are 20dp and the [contentLength] is 150dp, then
     * the minimum value of [contentScroll] would be 0dp and the maximum value would be 90dp.
     */
    val contentScroll: Float,
    /**
     * The full length of the content. Lazy lists never measure the content beyond the visible area,
     * so for long lists we never know a precise value, though we can estimate it based on the
     * average size of visible items.
     */
    val contentLength: Float,
    /**
     * A virtual constant distance that defines how much content must be scrolled for the focus line
     * to reach the center of the list viewport. This effectively controls the speed at which the
     * focus line centers.
     */
    val scrollThreshold: Float,
    /** Extra information on the layout properties (e.g. paddings). */
    val layoutProperties: ListLayoutProperties,
) {
    /**
     * The area within a list where content is displayed, excluding padding. This area defines the
     * region in which the focus line can move.
     *
     * E.g., if the list size is 100dp and the paddings are 20dp, then the value of [viewportSize]
     * would be 60dp.
     */
    val viewportSize: Float
        get() = layoutProperties.mainAxisAvailableSize.toFloat()
}
