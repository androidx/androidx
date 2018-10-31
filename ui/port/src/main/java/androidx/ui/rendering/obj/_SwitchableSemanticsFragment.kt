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
import androidx.ui.subList
import kotlin.coroutines.experimental.buildSequence

// / An [_InterestingSemanticsFragment] that can be told to only add explicit
// / [SemanticsNode]s to the parent.
// /
// / If [markAsExplicit] was not called before this fragment is added to
// / another fragment it will merge [config] into the parent's [SemanticsNode]
// / and add its [children] to it.
// /
// / If [markAsExplicit] was called before adding this fragment to another
// / fragment it will create a new [SemanticsNode]. The newly created node will
// / be annotated with the [SemanticsConfiguration] that - without the call to
// / [markAsExplicit] - would have been merged into the parent's [SemanticsNode].
// / Similarly, the new node will also take over the children that otherwise
// / would have been added to the parent's [SemanticsNode].
// /
// / After a call to [markAsExplicit] the only element returned by [children]
// / is the newly created node and [config] will return null as the fragment
// / no longer wants to merge any semantic information into the parent's
// / [SemanticsNode].
internal class _SwitchableSemanticsFragment(
    private val mergeIntoParent: Boolean,
    config: SemanticsConfiguration,
    owner: RenderObject,
    dropsSemanticsOfPreviousSiblings: Boolean
) : _InterestingSemanticsFragment(
    owner = owner,
    dropsSemanticsOfPreviousSiblings = dropsSemanticsOfPreviousSiblings
) {

    init {
        assert(mergeIntoParent != null)
        assert(config != null)
    }

    private var _isConfigWritable = false
    private val _children: MutableList<_InterestingSemanticsFragment> = mutableListOf()

    override fun compileChildren(
        parentSemanticsClipRect: Rect?,
        parentPaintClipRect: Rect?
    ): Iterable<SemanticsNode> {
        return buildSequence<SemanticsNode> {
            if (!_isExplicit) {
                owner._semantics = null
                for (fragment in _children) {
                    assert(_ancestorChain.first() == fragment._ancestorChain.last())
                    fragment._ancestorChain.addAll(_ancestorChain.subList(1))
                    yieldAll(
                        fragment.compileChildren(
                            parentSemanticsClipRect = parentSemanticsClipRect,
                            parentPaintClipRect = parentPaintClipRect
                        )
                    )
                }
                return@buildSequence
            }

            val geometry: _SemanticsGeometry? =
                if (_needsGeometryUpdate) {
                    _SemanticsGeometry(
                        parentSemanticsClipRect = parentSemanticsClipRect,
                        parentPaintClipRect = parentPaintClipRect,
                        ancestors = _ancestorChain
                    )
                } else {
                    null
                }

            if (!mergeIntoParent && (geometry?.dropFromTree == true)) {
                return@buildSequence; // Drop the node, it's not going to be visible.
            }

            owner._semantics = owner._semantics
                    ?: SemanticsNode(showOnScreen = { owner.showOnScreen() })
            val node: SemanticsNode = owner._semantics!!.also {
                it.isMergedIntoParent = mergeIntoParent
                it.tags = _tagsForChildren
            }

            if (geometry != null) {
                assert(_needsGeometryUpdate)
                node.also {
                    it.rect = geometry.rect
                    it.transform = geometry.transform
                    it.parentSemanticsClipRect = geometry.semanticsClipRect
                    it.parentPaintClipRect = geometry.paintClipRect
                }

                if (!mergeIntoParent && geometry.markAsHidden) {
                    _ensureConfigIsWritable()
                    _config.isHidden = true
                }
            }

            val children: MutableList<SemanticsNode> = mutableListOf()
            for (fragment in _children)
                children.addAll(
                    fragment.compileChildren(
                        parentSemanticsClipRect = node.parentSemanticsClipRect,
                        parentPaintClipRect = node.parentPaintClipRect
                    )
                )

            if (_config.isSemanticBoundary) {
                owner.assembleSemanticsNode(node, _config, children)
            } else {
                node.updateWith(config = _config, childrenInInversePaintOrder = children)
            }

            yield(node)
        }.asIterable()
    }

    // TODO(Migration/ryanmentley): This _prefix should stay (remove this comment, though)
    private var _config: SemanticsConfiguration = config
    override val config: SemanticsConfiguration?
        get() = if (_isExplicit) null else _config

    override fun addAll(fragments: Iterable<_InterestingSemanticsFragment>) {
        for (fragment in fragments) {
            _children.add(fragment)
            fragment.config?.let {
                _ensureConfigIsWritable()
                _config.absorb(it)
            }
        }
    }

    private fun _ensureConfigIsWritable() {
        if (!_isConfigWritable) {
            _config = _config.copy()
            _isConfigWritable = true
        }
    }

    private var _isExplicit = false

    override fun markAsExplicit() {
        _isExplicit = true
    }

    private val _needsGeometryUpdate
        get() = _ancestorChain.size > 1
}