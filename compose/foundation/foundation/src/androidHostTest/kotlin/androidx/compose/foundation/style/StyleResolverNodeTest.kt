/*
 * Copyright 2026 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class StyleResolverNodeTest {

    @Test
    fun styleResolverElement_createAndEquals() {
        val resolverA = StyleResolver(CommonStyle)
        val resolverB = StyleResolver(CommonStyle)

        val elementA1 = StyleResolverElement(resolverA)
        val elementA2 = StyleResolverElement(resolverA)
        val elementB = StyleResolverElement(resolverB)

        assertEquals(elementA1, elementA2)
        assertEquals(elementA1.hashCode(), elementA2.hashCode())
        assertNotEquals(elementA1, elementB)

        val node = elementA1.create()
        assertSame(resolverA, node.styleResolverField)
        assertFalse(node.isAttached)
    }

    @Test
    fun styleResolverElement_updateSetsStyleResolver() {
        val resolverA = StyleResolver(CommonStyle)
        val resolverB = StyleResolver(CommonStyle)

        val node = StyleResolverNode(resolverA, null)
        assertSame(resolverA, node.styleResolverField)

        val elementB = StyleResolverElement(resolverB)
        elementB.update(node)
        assertSame(resolverB, node.styleResolverField)
    }

    @Test
    fun unattachedNode_updateResolver_bindsOnAttach() {
        val prop = stylePropertyOf("testProp") { "default" }
        val resolverA = StyleResolver({ prop.provide("valueA") })
        val resolverB = StyleResolver({ prop.provide("valueB") })

        val node = StyleResolverNode(resolverA, null)

        // Update when not attached
        node.styleResolver = resolverB
        assertSame(resolverB, node.styleResolverField)

        // Neither was bound yet because node has not been attached
        assertFailsWith<IllegalStateException> { resolverA.resolve { prop.value } }
        assertFailsWith<IllegalStateException> { resolverB.resolve { prop.value } }

        // Attach the node: onAttach should bind the updated resolverB
        node.onAttach()

        // Detach should dispose the active resolverB
        node.onDetach()
        assertFailsWith<IllegalStateException> { resolverB.resolve { prop.value } }
    }
}
