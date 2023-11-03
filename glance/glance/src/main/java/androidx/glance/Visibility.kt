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

package androidx.glance

import androidx.annotation.RestrictTo

/**
 * Value of the visibility field for a node in the composition tree.
 */
enum class Visibility {
    /** The node is visible (the default). */
    Visible,
    /** The node is invisible, but still uses the space in the layout. */
    Invisible,
    /** The node is invisible, and doesn't use any space, as if removed. */
    Gone
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class VisibilityModifier(val visibility: Visibility) : GlanceModifier.Element

/**
 * Change the visibility of the current node.
 *
 * @param visibility New visibility of the node.
 */
fun GlanceModifier.visibility(visibility: Visibility) =
    this.then(VisibilityModifier(visibility))
