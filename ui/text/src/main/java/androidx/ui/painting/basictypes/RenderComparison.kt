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
 * The description of the difference between two objects, in the context of how
 *  it will affect the rendering.
 *
 *  Used by [TextSpan.compareTo] and [TextStyle.compareTo].
 *
 *  The values in this enum are ordered such that they are in increasing order
 *  of cost. A value with index N implies all the values with index less than N.
 *  For example, [layout] (index 3) implies [paint] (2).
 */
// TODO(siyamed) remove this class if not required
internal enum class RenderComparison {
    /**
     * The two objects are identical (meaning deeply equal, not necessarily ===).
     */
    IDENTICAL,

    /**
     * The two objects are identical for the purpose of layout, but may be different
     * in other ways.
     *
     * For example, maybe some event handlers changed.
     */
    METADATA,

    /**
     * The two objects are different but only in ways that affect paint, not layout.
     *
     * For example, only the color is changed.
     *
     * [RenderObject.markNeedsPaint] would be necessary to handle this kind of
     * change in a render object.
     */
    PAINT,

    /**
     *  The two objects are different in ways that affect layout (and therefore paint).
     *
     *  For example, the size is changed.
     *
     *  This is the most drastic level of change possible.
     *
     *  [RenderObject.markNeedsLayout] would be necessary to handle this kind of
     *  change in a render object.
     */
    LAYOUT
}
