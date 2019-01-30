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
package androidx.ui.core

import android.view.MotionEvent
import androidx.ui.core.semantics.SemanticsConfiguration

/**
 * Represent a node in the semantics tree together with information about its parent and children.
 *
 * @param parent Parent of this node or null if none
 * @param data The actual semantics data of this node
 */
interface SemanticsTreeNode {
    val parent: SemanticsTreeNode?
    val data: SemanticsConfiguration
    val children: Set<SemanticsTreeNode>
    val globalPosition: PxPosition?
}

/**
 * Element responsible for providing the semantics tree of the hierarchy. Typically the root container.
 */
interface SemanticsTreeProvider {
    fun getAllSemanticNodes(): List<SemanticsTreeNode>
    fun sendEvent(event: MotionEvent)
}