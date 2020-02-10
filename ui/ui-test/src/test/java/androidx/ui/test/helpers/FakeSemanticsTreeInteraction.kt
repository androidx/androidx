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

package androidx.ui.test.helpers

import android.graphics.Bitmap
import androidx.ui.core.SemanticsTreeProvider
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.test.InputDispatcher
import androidx.ui.test.SemanticsPredicate
import androidx.ui.test.SemanticsTreeInteraction
import androidx.ui.test.SemanticsTreeNodeStub
import androidx.ui.unit.PxBounds

internal class FakeSemanticsTreeInteraction internal constructor(
    selector: SemanticsPredicate
) : SemanticsTreeInteraction(selector) {
    private lateinit var semanticsToUse: List<SemanticsNode>

    fun withProperties(
        vararg properties: SemanticsConfiguration.() -> Unit
    ): FakeSemanticsTreeInteraction {
        semanticsToUse = properties.map { configBlock ->
            val config = SemanticsConfiguration().also {
                it.isSemanticBoundary = true
                it.configBlock()
            }
            SemanticsTreeNodeStub(config)
        }.toList()
        return this
    }

    fun withSemantics(vararg nodes: SemanticsNode): FakeSemanticsTreeInteraction {
        semanticsToUse = nodes.toList()
        return this
    }

    override fun getAllSemanticsNodes(): List<SemanticsNode> {
        return semanticsToUse.filter {
            selector.condition(it.config)
        }
    }

    override fun performAction(action: (SemanticsTreeProvider) -> Unit) {
        TODO("replace with host side interaction")
    }

    override fun sendInput(action: (InputDispatcher) -> Unit) {
        TODO("replace with host side interaction")
    }

    override fun isInScreenBounds(rectangle: PxBounds): Boolean {
        TODO("catalintudor: implement")
    }

    override fun captureNodeToBitmap(node: SemanticsNode): Bitmap {
        TODO("not implemented")
    }
}