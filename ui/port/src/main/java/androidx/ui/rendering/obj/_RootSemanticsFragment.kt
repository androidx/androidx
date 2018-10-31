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
import androidx.ui.painting.matrixutils.matrixEquals
import androidx.ui.semantics.SemanticsConfiguration
import androidx.ui.semantics.SemanticsNode
import androidx.ui.vectormath64.Matrix4
import kotlin.coroutines.experimental.buildSequence

// / An [_InterestingSemanticsFragment] that produces the root [SemanticsNode] of
// / the semantics tree.
// /
// / The root node is available as the only element in the Iterable returned by
// / [children].
internal class _RootSemanticsFragment(
    owner: RenderObject,
    dropsSemanticsOfPreviousSiblings: Boolean
) : _InterestingSemanticsFragment(
    owner = owner,
    dropsSemanticsOfPreviousSiblings = dropsSemanticsOfPreviousSiblings
) {

    override fun compileChildren(
        parentSemanticsClipRect: Rect?,
        parentPaintClipRect: Rect?
    ): Iterable<SemanticsNode> {
        return buildSequence {
            assert(_tagsForChildren == null || _tagsForChildren!!.isEmpty())
            assert(parentSemanticsClipRect == null)
            assert(parentPaintClipRect == null)
            assert(_ancestorChain.size == 1)

            owner._semantics = owner._semantics
                    ?: SemanticsNode.root(
                        showOnScreen = { owner.showOnScreen() },
                        owner = owner.owner!!.semanticsOwner!!
                    )
            val node: SemanticsNode = owner._semantics!!
            assert(
                matrixEquals(
                    node.transform,
                    Matrix4.identity()
                )
            )
            assert(node.parentSemanticsClipRect == null)
            assert(node.parentPaintClipRect == null)

            node.rect = owner.semanticBounds!!

            val children: MutableList<SemanticsNode> = mutableListOf()
            for (fragment in _children) {
                assert(fragment.config == null)
                children.addAll(
                    fragment.compileChildren(
                        parentSemanticsClipRect = parentSemanticsClipRect,
                        parentPaintClipRect = parentPaintClipRect
                    )
                )
            }
            node.updateWith(config = null, childrenInInversePaintOrder = children)

            assert(!node.isInvisible)
            yield(node)
        }.asIterable()
    }

    override val config: SemanticsConfiguration?
        get() = null

    private val _children: MutableList<_InterestingSemanticsFragment> = mutableListOf()

    override fun markAsExplicit() {
        // nothing to do, we are always explicit.
    }

    override fun addAll(fragments: Iterable<_InterestingSemanticsFragment>) {
        _children.addAll(fragments)
    }
}