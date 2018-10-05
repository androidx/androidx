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

package androidx.ui.semantics

import androidx.ui.Float64List
import androidx.ui.Int32List
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.TextDirection

// / An object that creates [SemanticsUpdate] objects.
// /
// / Once created, the [SemanticsUpdate] objects can be passed to
// / [Window.updateSemantics] to update the semantics conveyed to the user.
class SemanticsUpdateBuilder /* TODO(Migration): NativeFieldWrapperClass2 */ {
    //  /// Creates an empty [SemanticsUpdateBuilder] object.
//  SemanticsUpdateBuilder() { _constructor(); }
//  void _constructor() native 'SemanticsUpdateBuilder_constructor';
//
    // / Update the information associated with the node with the given `id`.
    // /
    // / The semantics nodes form a tree, with the root of the tree always having
    // / an id of zero. The `childrenInTraversalOrder` and `childrenInHitTestOrder`
    // / are the ids of the nodes that are immediate children of this node. The
    // / former enumerates children in traversal order, and the latter enumerates
    // / the same children in the hit test order. The two lists must have the same
    // / length and contain the same ids. They may only differ in the order the
    // / ids are listed in. For more information about different child orders, see
    // / [DebugSemanticsDumpOrder].
    // /
    // / The system retains the nodes that are currently reachable from the root.
    // / A given update need not contain information for nodes that do not change
    // / in the update. If a node is not reachable from the root after an update,
    // / the node will be discarded from the tree.
    // /
    // / The `flags` are a bit field of [SemanticsFlag]s that apply to this node.
    // /
    // / The `actions` are a bit field of [SemanticsAction]s that can be undertaken
    // / by this node. If the user wishes to undertake one of these actions on this
    // / node, the [Window.onSemanticsAction] will be called with `id` and one of
    // / the possible [SemanticsAction]s. Because the semantics tree is maintained
    // / asynchronously, the [Window.onSemanticsAction] callback might be called
    // / with an action that is no longer possible.
    // /
    // / The `label` is a string that describes this node. The `value` property
    // / describes the current value of the node as a string. The `increasedValue`
    // / string will become the `value` string after a [SemanticsAction.increase]
    // / action is performed. The `decreasedValue` string will become the `value`
    // / string after a [SemanticsAction.decrease] action is performed. The `hint`
    // / string describes what result an action performed on this node has. The
    // / reading direction of all these strings is given by `textDirection`.
    // /
    // / The fields 'textSelectionBase' and 'textSelectionExtent' describe the
    // / currently selected text within `value`.
    // /
    // / For scrollable nodes `scrollPosition` describes the current scroll
    // / position in logical pixel. `scrollExtentMax` and `scrollExtentMin`
    // / describe the maximum and minimum in-rage values that `scrollPosition` can
    // / be. Both or either may be infinity to indicate unbound scrolling. The
    // / value for `scrollPosition` can (temporarily) be outside this range, for
    // / example during an overscroll.
    // /
    // / The `rect` is the region occupied by this node in its own coordinate
    // / system.
    // /
    // / The `transform` is a matrix that maps this node's coordinate system into
    // / its parent's coordinate system.
    fun updateNode(
        id: Int,
        flags: Int,
        actions: Int,
        textSelectionBase: Int,
        textSelectionExtent: Int,
        scrollPosition: Double,
        scrollExtentMax: Double,
        scrollExtentMin: Double,
        rect: Rect,
        label: String,
        hint: String,
        value: String,
        increasedValue: String,
        decreasedValue: String,
        textDirection: TextDirection?,
        transform: Float64List,
        childrenInTraversalOrder: Int32List,
        childrenInHitTestOrder: Int32List,
//    @Deprecated("use additionalActions instead") // TODO(Migration): Put this back somehow
        customAcccessibilityActions: Int32List? = null,
        additionalActions: Int32List?
    ) {
        if (transform.size != 16) {
            throw IllegalArgumentException("transform argument must have 16 entries.")
        }
        _updateNode(
            id,
            flags,
            actions,
            textSelectionBase,
            textSelectionExtent,
            scrollPosition,
            scrollExtentMax,
            scrollExtentMin,
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            label,
            hint,
            value,
            increasedValue,
            decreasedValue,
            if (textDirection != null) textDirection.ordinal + 1 else 0,
            transform,
            childrenInTraversalOrder,
            childrenInHitTestOrder,
            additionalActions ?: customAcccessibilityActions
        )
    }

    fun _updateNode(
        id: Int,
        flags: Int,
        actions: Int,
        textSelectionBase: Int,
        textSelectionExtent: Int,
        scrollPosition: Double,
        scrollExtentMax: Double,
        scrollExtentMin: Double,
        left: Double,
        top: Double,
        right: Double,
        bottom: Double,
        label: String,
        hInt: String,
        value: String,
        increasedValue: String,
        decreasedValue: String,
        textDirection: Int,
        transform: Float64List,
        childrenIntraversalOrder: Int32List,
        childrenInHitTestOrder: Int32List,
        additionalActions: Int32List?
    ) {
        TODO()
        // native "SemanticsUpdateBuilder_updateNode";
    }

    // / Update the custom semantics action associated with the given `id`.
    // /
    // / The name of the action exposed to the user is the `label`. For overriden
    // / standard actions this value is ignored.
    // /
    // / The `hint` should describe what happens when an action occurs, not the
    // / manner in which a tap is accomplished. For example, use "delete" instead
    // / of "double tap to delete".
    // /
    // / The text direction of the `hint` and `label` is the same as the global
    // / window.
    // /
    // / For overriden standard actions, `overrideId` corresponds with a
    // / [SemanticsAction.index] value. For custom actions this argument should not be
    // / provided.
//  fun updateCustomAction({int id, String label, String hint, int overrideId = -1}) {
//        TODO()
//    assert(id != null);
//    assert(overrideId != null);
//    _updateCustomAction(id, label, hint, overrideId);
//  }
//  void _updateCustomAction(int id, String label, String hint, int overrideId) native 'SemanticsUpdateBuilder_updateCustomAction';
//
//  /// Creates a [SemanticsUpdate] object that encapsulates the updates recorded
//  /// by this object.
//  ///
//  /// The returned object can be passed to [Window.updateSemantics] to actually
//  /// update the semantics retained by the system.
//  SemanticsUpdate build() native 'SemanticsUpdateBuilder_build';
}