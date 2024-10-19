/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.semantics

/** A listener that can be used to observe semantic changes. */
internal interface SemanticsListener {

    /**
     * [onSemanticsChanged] is called when the [SemanticsConfiguration] of a LayoutNode changes, or
     * when a node calls SemanticsModifierNode.invalidateSemantics.
     *
     * @param semanticsInfo the current [SemanticsInfo] of the layout node that has changed.
     * @param previousSemanticsConfiguration the previous [SemanticsConfiguration] associated with
     *   the layout node.
     */
    fun onSemanticsChanged(
        semanticsInfo: SemanticsInfo,
        previousSemanticsConfiguration: SemanticsConfiguration?
    )
}
