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

// / Describes the semantics information a [RenderObject] wants to add to its
// / parent.
// /
// / It has two notable subclasses:
// /  * [_InterestingSemanticsFragment] describing actual semantic information to
// /    be added to the parent.
// /  * [_ContainerSemanticsFragment]: a container class to transport the semantic
// /    information of multiple [_InterestingSemanticsFragment] to a parent.
internal abstract class _SemanticsFragment(
    // / Whether this fragment wants to make the semantics information of
    // / previously painted [RenderObject]s unreachable for accessibility purposes.
    // /
    // / See also:
    // /
    // /  * [SemanticsConfiguration.isBlockingSemanticsOfPreviouslyPaintedNodes]
    // /    describes what semantics are dropped in more detail.
    val dropsSemanticsOfPreviousSiblings: Boolean
) {
    init {
        assert(dropsSemanticsOfPreviousSiblings != null)
    }

    // / Incorporate the fragments of children into this fragment.
    abstract fun addAll(fragments: Iterable<_InterestingSemanticsFragment>)

    // / Returns [_InterestingSemanticsFragment] describing the actual semantic
    // / information that this fragment wants to add to the parent.
    abstract val interestingFragments: Iterable<_InterestingSemanticsFragment>
}
