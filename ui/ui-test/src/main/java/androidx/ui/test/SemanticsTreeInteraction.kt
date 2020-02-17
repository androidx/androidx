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

package androidx.ui.test

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.ui.core.SemanticsTreeProvider
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.test.android.AndroidSemanticsTreeInteraction
import androidx.ui.unit.PxBounds

/**
 * Provides abstraction for writing queries, actions and asserts over Compose semantics tree using
 * extension functions. This class is expected to have Android and host side specific
 * implementations.
 */
internal abstract class SemanticsTreeInteraction(
    val selector: SemanticsPredicate
) {

    internal abstract fun performAction(action: (SemanticsTreeProvider) -> Unit)

    internal abstract fun sendInput(action: (InputDispatcher) -> Unit)

    internal abstract fun isInScreenBounds(rectangle: PxBounds): Boolean

    internal abstract fun getAllSemanticsNodes(): List<SemanticsNode>

    @RequiresApi(Build.VERSION_CODES.O)
    internal abstract fun captureNodeToBitmap(node: SemanticsNode): Bitmap

    fun findAllMatching(): List<SemanticsNodeInteraction> {
        return getAllSemanticsNodes()
            .filter { node ->
                selector.condition(node.config)
            }.map {
                SemanticsNodeInteraction(it, this)
            }
    }

    internal fun getNodesByIds(ids: List<Int>): List<SemanticsNode> {
        return getAllSemanticsNodes().filter { it.id in ids }
    }

    internal fun findOne(): SemanticsNodeInteraction {
        val nodes = getAllSemanticsNodes()
            .filter { node ->
                selector.condition(node.config)
            }
        return SemanticsNodeInteraction(nodes, this)
    }

    internal fun contains(nodeId: Int): Boolean {
        return getAllSemanticsNodes().any { it.id == nodeId }
    }
}

internal var semanticsTreeInteractionFactory: (
    selector: SemanticsPredicate
) -> SemanticsTreeInteraction = { selector ->
    AndroidSemanticsTreeInteraction(selector)
}
