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

// / Signature for a function that is called during layout.
// /
// / Used by [RenderObject.invokeLayoutCallback].
typealias LayoutCallback = (Constraints) -> Unit

// These are just a stubbed class to remove warnings:
class RendererBinding
class BoxConstraints
class HitTestEntry
class HitTestResult
class RenderView
class PointerEvent
abstract class BindingBase() {
    abstract fun reassembleApplication()
}
class GlobalKey
class SemanticsEvent
class SemanticsOwner
class SemanticsTag
class SemanticsNode() {
    val parent: SemanticsNode? = null
    val parentPaintClipRect: Rect? = null
    val parentSemanticsClipRect: Rect? = null
    val isPartOfNodeMerging = false
    fun updateWith(
        config: SemanticsConfiguration?,
        childrenInInversePaintOrder: Iterable<SemanticsNode>
    ) {}
    fun sendEvent(event: SemanticsEvent) {}
}
open class _SemanticsFragment() {
    val dropsSemanticsOfPreviousSiblings = false
    val interestingFragments = listOf<_InterestingSemanticsFragment>()
    fun markAsExplicit() {}
    fun addAll(interestingFragments: List<_InterestingSemanticsFragment>) {}
}
abstract class _InterestingSemanticsFragment() : _SemanticsFragment() {
    abstract fun compileChildren(parentSemanticsClipRect: Rect?, parentPaintClipRect: Rect?):
            Iterable<SemanticsNode>
    abstract val config: SemanticsConfiguration?
    abstract fun addAncestor(ancestor: RenderObject)
    abstract fun addTags(tags: Iterable<SemanticsTag>)
    val hasConfigForParent = false
}
class _RootSemanticsFragment(
    val owner: RenderObject,
    dropsSemanticsOfPreviousSiblings: Boolean
) : _SemanticsFragment()
class _SwitchableSemanticsFragment(
    val owner: RenderObject,
    dropsSemanticsOfPreviousSiblings: Boolean,
    val config: SemanticsConfiguration,
    val mergeIntoParent: Boolean
) : _SemanticsFragment()
class _ContainerSemanticsFragment(dropsSemanticsOfPreviousSiblings: Boolean) : _SemanticsFragment()
