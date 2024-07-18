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

package androidx.compose.ui.platform

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropManager
import androidx.compose.ui.draganddrop.DragAndDropModifierNode
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.RootNodeOwner

/**
 * The interface for platform implementations of [DragAndDropManager].
 *
 * This is needed because [DragAndDropManager] itself is `internal`, and therefore can't be
 * implemented by 3rd-party code. The solution is for [RootNodeOwner] to implement the internal
 * [DragAndDropManager] by delegating to a public [PlatformDragAndDropManager] provided by
 * [PlatformContext].
 *
 * For documentation of the methods of this interface refer to [DragAndDropManager].
 */
@InternalComposeUiApi
interface PlatformDragAndDropManager {
    val modifier: Modifier

    fun drag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit,
    ): Boolean

    fun registerNodeInterest(node: DragAndDropModifierNode)

    fun isInterestedNode(node: DragAndDropModifierNode): Boolean
}


/**
 * Returns a [DragAndDropManager] that delegates to `this` [PlatformDragAndDropManager].
 */
internal fun PlatformDragAndDropManager.asDragAndDropManager() = object : DragAndDropManager {
    override val modifier: Modifier
        get() = this@asDragAndDropManager.modifier

    override fun drag(
        transferData: DragAndDropTransferData,
        decorationSize: Size,
        drawDragDecoration: DrawScope.() -> Unit
    ): Boolean {
        return this@asDragAndDropManager.drag(transferData, decorationSize, drawDragDecoration)
    }

    override fun registerNodeInterest(node: DragAndDropModifierNode) {
        this@asDragAndDropManager.registerNodeInterest(node)
    }

    override fun isInterestedNode(node: DragAndDropModifierNode): Boolean {
        return this@asDragAndDropManager.isInterestedNode(node)
    }
}