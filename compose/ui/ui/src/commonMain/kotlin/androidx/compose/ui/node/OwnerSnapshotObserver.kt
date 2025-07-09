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

package androidx.compose.ui.node

import androidx.compose.runtime.snapshots.SnapshotStateObserver

/**
 * Performs snapshot observation for blocks like draw and layout which should be re-invoked
 * automatically when the snapshot value has been changed.
 */
@Suppress(
    "CallbackName", // TODO rename this and SnapshotStateObserver. b/173401548
    "NOTHING_TO_INLINE",
)
internal class OwnerSnapshotObserver(onChangedExecutor: (callback: () -> Unit) -> Unit) {

    private val observer = SnapshotStateObserver(onChangedExecutor)

    private val onCommitAffectingLookaheadMeasure: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValidOwnerScope) {
            layoutNode.requestLookaheadRemeasure()
        }
    }

    private val onCommitAffectingMeasure: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValidOwnerScope) {
            layoutNode.requestRemeasure()
        }
    }

    private val onCommitAffectingSemantics: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValidOwnerScope) {
            layoutNode.invalidateSemantics()
        }
    }

    private val onCommitAffectingLayout: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValidOwnerScope) {
            layoutNode.requestRelayout()
        }
    }

    private val onCommitAffectingLayoutModifier: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValidOwnerScope) {
            layoutNode.requestRelayout()
        }
    }

    private val onCommitAffectingLayoutModifierInLookahead: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValidOwnerScope) {
            layoutNode.requestLookaheadRelayout()
        }
    }

    private val onCommitAffectingLookahead: (LayoutNode) -> Unit = { layoutNode ->
        if (layoutNode.isValidOwnerScope) {
            layoutNode.requestLookaheadRelayout()
        }
    }

    /** Observe snapshot reads during layout of [node], executed in [block]. */
    // inlined as used only in one place to not add extra function call overhead
    internal inline fun observeLayoutSnapshotReads(node: LayoutNode, noinline block: () -> Unit) {
        observeReads(node, onCommitAffectingLayout, block)
    }

    /** Observe snapshot reads during layout of [node], executed in [block]. */
    // inlined as used only in one place to not add extra function call overhead
    internal inline fun observeLayoutSnapshotReadsAffectingLookahead(
        node: LayoutNode,
        noinline block: () -> Unit,
    ) {
        observeReads(node, onCommitAffectingLookahead, block)
    }

    /** Observe snapshot reads during layout of [node]'s LayoutModifiers, executed in [block]. */
    // inlined as used only in one place to not add extra function call overhead
    internal inline fun observeLayoutModifierSnapshotReads(
        node: LayoutNode,
        noinline block: () -> Unit,
    ) {
        observeReads(node, onCommitAffectingLayoutModifier, block)
    }

    /** Observe snapshot reads during layout of [node]'s LayoutModifiers, executed in [block]. */
    // inlined as used only in one place to not add extra function call overhead
    internal inline fun observeLayoutModifierSnapshotReadsAffectingLookahead(
        node: LayoutNode,
        noinline block: () -> Unit,
    ) {
        observeReads(node, onCommitAffectingLayoutModifierInLookahead, block)
    }

    /** Observe snapshot reads during measure of [node], executed in [block]. */
    // inlined as used only in one place to not add extra function call overhead
    internal inline fun observeMeasureSnapshotReads(node: LayoutNode, noinline block: () -> Unit) {
        observeReads(node, onCommitAffectingMeasure, block)
    }

    /** Observe snapshot reads during measure of [node], executed in [block]. */
    // inlined as used only in one place to not add extra function call overhead
    internal inline fun observeMeasureSnapshotReadsAffectingLookahead(
        node: LayoutNode,
        noinline block: () -> Unit,
    ) {
        observeReads(node, onCommitAffectingLookaheadMeasure, block)
    }

    // inlined as used only in one place to not add extra function call overhead
    internal inline fun observeSemanticsReads(node: LayoutNode, noinline block: () -> Unit) {
        observeReads(node, onCommitAffectingSemantics, block)
    }

    /**
     * Observe snapshot reads for any target, allowing consumers to determine how to respond to
     * state changes.
     */
    // inlined as used only in one place to not add extra function call overhead
    internal inline fun <T : OwnerScope> observeReads(
        target: T,
        noinline onChanged: (T) -> Unit,
        noinline block: () -> Unit,
    ) {
        observer.observeReads(target, onChanged, block)
    }

    internal fun clearInvalidObservations() {
        observer.clearIf { !(it as OwnerScope).isValidOwnerScope }
    }

    internal fun clear(target: Any) {
        observer.clear(target)
    }

    internal fun startObserving() {
        observer.start()
    }

    internal fun stopObserving() {
        observer.stop()
        observer.clear()
    }
}
