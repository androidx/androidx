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

package androidx.wear.protolayout.testing

import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row

internal val LayoutElement.children: List<LayoutElement>
    get() =
        when (this) {
            is Box -> this.contents
            is Row -> this.contents
            is Column -> this.contents
            // TODO b/372916396 - Dealing with Arc container and ArcLayoutElements
            else -> emptyList<LayoutElement>()
        }

internal fun searchElement(root: LayoutElement?, matcher: LayoutElementMatcher): LayoutElement? {
    if (root == null) return null
    if (matcher.matches(root)) return root
    return root.children.firstNotNullOfOrNull { searchElement(it, matcher) }
}
