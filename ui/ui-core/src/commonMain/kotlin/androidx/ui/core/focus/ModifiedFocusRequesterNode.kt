/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.ui.core.DelegatingLayoutNodeWrapper
import androidx.ui.core.LayoutNodeWrapper

@OptIn(ExperimentalFocus::class)
internal class ModifiedFocusRequesterNode(
    wrapped: LayoutNodeWrapper,
    modifier: FocusRequesterModifier
) : DelegatingLayoutNodeWrapper<FocusRequesterModifier>(wrapped, modifier) {

    // Searches for the focus node associated with this focus requester node.
    internal fun findFocusNode(): ModifiedFocusNode2? {
        // TODO(b/157597560): If there is no focus node in this modifier chain, check for a focus
        //  modifier in the layout node's children.
        return findNextFocusWrapper2()
    }

    override fun onModifierChanged() {
        super.onModifierChanged()
        modifier.focusRequester.focusRequesterNode = this
    }

    override fun attach() {
        super.attach()
        modifier.focusRequester.focusRequesterNode = this
    }

    override fun detach() {
        modifier.focusRequester.focusRequesterNode = null
        super.detach()
    }
}