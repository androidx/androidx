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

package androidx.ui.rendering.obj

import androidx.ui.rendering.box.BoxParentData

/** Parent data to support a doubly-linked list of children. */
// TODO(migration/Mihai): this should inherit from ParentData(), but specializing it here
//                        to inherit from BoxParentData as a workaround for mixins
abstract class ContainerParentDataMixin<ChildType : RenderObject> : BoxParentData() {
    // This class is intended to be used as a mixin, and should not be
    // extended directly.
//    factory ContainerParentDataMixin._() => null;

    /** The previous sibling in the parent's child list. */
    var previousSibling: ChildType? = null
    /** The next sibling in the parent's child list. */
    var nextSibling: ChildType? = null

    /** Clear the sibling pointers. */
    override fun detach() {
        super.detach()
        if (previousSibling != null) {
            val previousSiblingParentData =
                previousSibling!!.parentData as ContainerParentDataMixin<ChildType>
            assert(previousSibling != this)
            assert(previousSiblingParentData.nextSibling == this)
            previousSiblingParentData.nextSibling = nextSibling
        }
        if (nextSibling != null) {
            val nextSiblingParentData =
                nextSibling!!.parentData as ContainerParentDataMixin<ChildType>
            assert(nextSibling != this)
            assert(nextSiblingParentData.previousSibling == this)
            nextSiblingParentData.previousSibling = previousSibling
        }
        previousSibling = null
        nextSibling = null
    }
}