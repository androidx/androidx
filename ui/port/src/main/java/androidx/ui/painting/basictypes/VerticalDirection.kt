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

package androidx.ui.painting.basictypes

/**
 * A direction in which boxes flow vertically.
 *
 * This is used by the flex algorithm (e.g. [Column]) to decide in which
 * direction to draw boxes.
 *
 * This is also used to disambiguate `start` and `end` values (e.g.
 * [MainAxisAlignment.START] or [CrossAxisAlignment.END]).
 *
 * See also:
 *
 *  * [TextDirection], which controls the same thing but horizontally.
 */
enum class VerticalDirection {
    /**
     * Boxes should start at the bottom and be stacked vertically towards the top.
     *
     * The "start" is at the bottom, the "end" is at the top.
     */
    UP,

    /**
     * Boxes should start at the top and be stacked vertically towards the bottom.
     *
     * The "start" is at the top, the "end" is at the bottom.
     */
    DOWN
}