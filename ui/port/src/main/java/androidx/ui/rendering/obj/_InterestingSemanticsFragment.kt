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

import androidx.ui.engine.geometry.Rect
import androidx.ui.semantics.SemanticsConfiguration
import androidx.ui.semantics.SemanticsNode
import androidx.ui.semantics.SemanticsTag
import kotlin.coroutines.experimental.buildSequence

// / A [_SemanticsFragment] that describes which concrete semantic information
// / a [RenderObject] wants to add to the [SemanticsNode] of its parent.
// /
// / Specifically, it describes which children (as returned by [compileChildren])
// / should be added to the parent's [SemanticsNode] and which [config] should be
// / merged into the parent's [SemanticsNode].
internal abstract class _InterestingSemanticsFragment(
    owner: RenderObject,
    dropsSemanticsOfPreviousSiblings: Boolean
) : _SemanticsFragment(dropsSemanticsOfPreviousSiblings) {
    init {
        assert(owner != null)
    }

    internal val _ancestorChain: MutableList<RenderObject> = mutableListOf(owner)

    // / The [RenderObject] that owns this fragment (and any new [SemanticNode]
    // / introduced by it).
    val owner: RenderObject
        get() = _ancestorChain.first()

    // / The children to be added to the parent.
    abstract fun compileChildren(
        parentSemanticsClipRect: Rect?,
        parentPaintClipRect: Rect?
    ): Iterable<SemanticsNode>

    // / The [SemanticsConfiguration] the child wants to merge into the parent's
    // / [SemanticsNode] or null if it doesn't want to merge anything.
    abstract val config: SemanticsConfiguration?

    // / Disallows this fragment to merge any configuration into its parent's
    // / [SemanticsNode].
    // /
    // / After calling this the fragment will only produce children to be added
    // / to the parent and it will return null for [config].
    abstract fun markAsExplicit()

    // / Consume the fragments of children.
    // /
    // / For each provided fragment it will add that fragment's children to
    // / this fragment's children (as returned by [compileChildren]) and merge that
    // / fragment's [config] into this fragment's [config].
    // /
    // / If a provided fragment should not merge anything into [config] call
    // / [markAsExplicit] before passing the fragment to this method.
    abstract override fun addAll(fragments: Iterable<_InterestingSemanticsFragment>)

    // / Whether this fragment wants to add any semantic information to the parent
    // / [SemanticsNode].
    val hasConfigForParent
        get() = config != null

    override val interestingFragments: Iterable<_InterestingSemanticsFragment>
        get() = buildSequence { yield(this@_InterestingSemanticsFragment) }.asIterable()

    protected var _tagsForChildren: MutableSet<SemanticsTag> = mutableSetOf()

    // / Tag all children produced by [compileChildren] with `tags`.
    fun addTags(tags: Iterable<SemanticsTag>?) {
        if (tags == null)
            return
        _tagsForChildren.addAll(tags)
    }

    // / Adds the geometric information of `ancestor` to this object.
    // /
    // / Those information are required to properly compute the value for
    // / [SemanticsNode.transform], [SemanticsNode.clipRect], and
    // / [SemanticsNode.rect].
    // /
    // / Ancestors have to be added in order from [owner] up until the next
    // / [RenderObject] that owns a [SemanticsNode] is reached.
    fun addAncestor(ancestor: RenderObject) {
        _ancestorChain.add(ancestor)
    }
}