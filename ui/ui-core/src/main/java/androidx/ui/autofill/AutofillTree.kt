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

package androidx.ui.autofill

/**
 * The autofill tree.
 *
 * This is used temporarily until we have a semantic tree implemented (b/138604305). This
 * implementation is a tree of height = 1, and we don't use the root node right now, so this is
 * essentially a group of leaf nodes.
 */
class AutofillTree {
    val children: MutableMap<Int, AutofillNode> = mutableMapOf()

    operator fun plusAssign(autofillNode: AutofillNode) {
        children[autofillNode.id] = autofillNode
    }

    fun performAutofill(id: Int, value: String) = children[id]?.onFill?.invoke(value)
}