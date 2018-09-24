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

package androidx.ui.rendering.flex

/**
 * How the child is inscribed into the available space.
 *
 * See also:
 *
 *  * [RenderFlex], the flex render object.
 *  * [Column], [Row], and [Flex], the flex widgets.
 *  * [Expanded], the widget equivalent of [tight].
 *  * [Flexible], the widget equivalent of [loose].
 */
enum class FlexFit {
    /**
     * The child is forced to fill the available space.
     *
     * The [Expanded] widget assigns this kind of [FlexFit] to its child.
     */
    TIGHT,

    /**
     * The child can be at most as large as the available space (but is
     * allowed to be smaller).
     *
     * The [Flexible] widget assigns this kind of [FlexFit] to its child.
     */
    LOOSE
}