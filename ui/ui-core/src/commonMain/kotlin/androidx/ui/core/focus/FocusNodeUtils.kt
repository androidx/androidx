/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.focus

import androidx.ui.core.ExperimentalLayoutNodeApi
import androidx.ui.core.LayoutNode
import androidx.ui.util.fastForEach

internal val FOCUS_TAG = "Compose Focus"

@OptIn(ExperimentalLayoutNodeApi::class)
internal fun LayoutNode.focusableChildren(): List<ModifiedFocusNode> {
    val focusableChildren = mutableListOf<ModifiedFocusNode>()
    // TODO(b/152529395): Write a test for LayoutNode.focusableChildren(). We were calling the wrong
    //  function on [LayoutNodeWrapper] but no test caught this.
    this.outerLayoutNodeWrapper.findNextFocusWrapper()?.let { focusableChildren.add(it) }
        ?: children.fastForEach { layout -> focusableChildren.addAll(layout.focusableChildren()) }
    return focusableChildren
}

@OptIn(ExperimentalLayoutNodeApi::class)
internal fun LayoutNode.focusableChildren2(): List<ModifiedFocusNode2> {
    val focusableChildren = mutableListOf<ModifiedFocusNode2>()
    // TODO(b/152529395): Write a test for LayoutNode.focusableChildren(). We were calling the wrong
    //  function on [LayoutNodeWrapper] but no test caught this.
    this.outerLayoutNodeWrapper.findNextFocusWrapper2()?.let { focusableChildren.add(it) }
        ?: children.fastForEach { layout -> focusableChildren.addAll(layout.focusableChildren2()) }
    return focusableChildren
}