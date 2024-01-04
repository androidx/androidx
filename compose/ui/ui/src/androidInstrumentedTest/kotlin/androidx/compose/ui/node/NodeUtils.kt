/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectorInfo

/**
 * Remove the root modifier nodes as they are not relevant from the perspective of the tests.
 * There are 4 nodes: KeyInputNode, FocusTargetNode, RotaryInputNode and SemanticsNode.
 */
internal fun <T> List<T>.trimRootModifierNodes(): List<T> = dropLast(4)

internal fun Modifier.elementOf(node: Modifier.Node): Modifier {
    return this.then(ElementOf { node })
}
private data class ElementOf<T : Modifier.Node>(
    val factory: () -> T
) : ModifierNodeElement<T>() {
    override fun create(): T = factory()
    override fun update(node: T) {}
    override fun InspectorInfo.inspectableProperties() { name = "testNode" }
}
