/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.unit.Constraints
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import com.google.common.truth.Truth.assertThat

@RunWith(JUnit4::class)
class DelegatingNodeTest {

    @Test
    fun testKindSetIncludesDelegates() {
        assertThat(DelegatedWrapper { DrawMod() }.kindSet)
            .isEqualTo(Nodes.Any or Nodes.Draw)
    }

    @Test
    fun testKindSetUpdatesAfter() {
        val a = DrawMod("a")
        val b = DelegatedWrapper { LayoutMod("b") }
        val c = object : DelegatingNode() {}
        val chain = layout(a, b, c)
        assert(chain.has(Nodes.Draw))
        assert(chain.has(Nodes.Layout))
        assert(!chain.has(Nodes.Semantics))

        assert(a.kindSet == Nodes.Any or Nodes.Draw)
        assert(a.aggregateChildKindSet == Nodes.Any or Nodes.Draw or Nodes.Layout)

        assert(b.kindSet == Nodes.Any or Nodes.Layout)
        assert(b.aggregateChildKindSet == Nodes.Any or Nodes.Layout)

        assert(c.kindSet == Nodes.Any.mask)
        assert(c.aggregateChildKindSet == Nodes.Any.mask)

        c.delegateUnprotected(SemanticsMod("c"))
        assert(chain.has(Nodes.Semantics))

        assert(a.kindSet == Nodes.Any or Nodes.Draw)
        assert(a.aggregateChildKindSet ==
            Nodes.Any or Nodes.Draw or Nodes.Layout or Nodes.Semantics)

        assert(b.kindSet == Nodes.Any or Nodes.Layout)
        assert(b.aggregateChildKindSet == Nodes.Any or Nodes.Layout or Nodes.Semantics)

        assert(c.kindSet == Nodes.Any or Nodes.Semantics)
        assert(c.aggregateChildKindSet == Nodes.Any or Nodes.Semantics)
    }

    @Test
    fun testAsKindReturnsDelegate() {
        val node = DelegatedWrapper { DrawMod() }
        assert(node.isKind(Nodes.Draw))
        assert(node.asKind(Nodes.Draw) is DrawMod)
        assert(node.asKind(Nodes.Draw) === node.wrapped)
    }

    @Test
    fun testAsKindReturnsNestedDelegate() {
        val node = DelegatedWrapper { DelegatedWrapper { DrawMod() } }
        assert(node.isKind(Nodes.Draw))
        assert(node.asKind(Nodes.Draw) is DrawMod)
        assert(node.asKind(Nodes.Draw) === node.wrapped.wrapped)
    }

    @Test
    fun testAsKindReturnsSelf() {
        val node = object : DrawModifierNode, DelegatingNode() {
            val wrapped = delegate(DrawMod())
            override fun ContentDrawScope.draw() {
                with(wrapped) { draw() }
            }
        }
        assert(node.isKind(Nodes.Draw))
        assert(node.asKind(Nodes.Draw) is DrawModifierNode)
        assert(node.asKind(Nodes.Draw) === node)
    }

    @Test
    fun testAsKindMultipleDelegatesReturnsLast() {
        val node = object : DelegatingNode() {
            val first = delegate(DrawMod())
            val second = delegate(DrawMod())
        }
        assert(node.isKind(Nodes.Draw))
        assert(node.asKind(Nodes.Draw) is DrawModifierNode)
        assert(node.asKind(Nodes.Draw) === node.second)
    }

    @Test
    fun testDispatchForMultipleDelegatesSameKind() {
        val node = object : DelegatingNode() {
            val first = delegate(DelegatedWrapper { DrawMod("first") })
            val second = delegate(DrawMod("second"))
        }
        assertDispatchOrder(node, Nodes.Draw, node.first.wrapped, node.second)
    }

    @Test
    fun testDispatchForSelfOnlyDispatchesToSelf() {
        val node = object : DrawModifierNode, DelegatingNode() {
            val wrapped = delegate(DrawMod())
            override fun ContentDrawScope.draw() {
                with(wrapped) { draw() }
            }
        }
        assertDispatchOrder(node, Nodes.Draw, node)
    }

    @Test
    fun testDispatchNestedSelfStops() {
        val node = object : DelegatingNode() {
            val first = delegate(DrawMod())
            val second = delegate(DrawMod())
            val third = delegate(object : DrawModifierNode, DelegatingNode() {
                val first = delegate(DrawMod())
                val second = delegate(DrawMod())
                override fun ContentDrawScope.draw() {
                    with(first) { draw() }
                }
            })
        }
        assertDispatchOrder(node, Nodes.Draw,
            node.first,
            node.second,
            node.third
        )
    }

    @Test
    fun testHeadToTailNoDelegates() {
        val a = DrawMod("a")
        val b = DrawMod("b")
        val chain = layout(a, b)
        val recorder = Recorder()
        chain.headToTail(Nodes.Draw, recorder)
        assertThat(recorder.recorded).isEqualTo(listOf(a, b))
    }

    @Test
    fun testHeadToTailWithDelegate() {
        val a = DelegatedWrapper { DrawMod() }
        val b = DrawMod()
        val chain = layout(a, b)
        val recorder = Recorder()
        chain.headToTail(Nodes.Draw, recorder)
        assertThat(recorder.recorded).isEqualTo(listOf(a.wrapped, b))
    }

    @Test
    fun testVisitSubtreeWithDelegates() {
        val x = DrawMod("x")
        val a = DelegatedWrapper { DrawMod("a") }
        val b = DrawMod("b")
        val c = DelegatedWrapper { DrawMod("c") }
        val d = DrawMod("d")
        layout(x, a, b) {
            layout(c)
            layout(d)
        }
        val recorder = Recorder()
        x.visitSubtree(Nodes.Draw, recorder)
        assertThat(recorder.recorded)
            .isEqualTo(listOf(
                a.wrapped,
                b,
                d,
                c.wrapped,
            ))
    }

    @Test
    fun testVisitAncestorsWithDelegates() {
        val x = DrawMod("x")
        val a = DelegatedWrapper { DrawMod("a") }
        val b = DrawMod("b")
        val c = DelegatedWrapper { DrawMod("c") }
        val d = DrawMod("d")
        layout(a) {
            layout(b) {
                layout(c) {
                    layout(d, x)
                }
            }
        }
        val recorder = Recorder()
        x.visitAncestors(Nodes.Draw, block = recorder)
        assertThat(recorder.recorded)
            .isEqualTo(listOf(
                d,
                c.wrapped,
                b,
                a.wrapped,
            ))
    }

    @Test
    fun testUndelegate() {
        val node = object : DelegatingNode() {}
        val chain = layout(node)

        assert(!node.isKind(Nodes.Draw))
        assert(!chain.has(Nodes.Draw))

        val draw = node.delegateUnprotected(DrawMod())

        assert(node.isKind(Nodes.Draw))
        assert(chain.has(Nodes.Draw))
        assert(node.asKind(Nodes.Draw) === draw)

        node.undelegateUnprotected(draw)

        assert(!draw.isAttached)
        assert(node.isAttached)

        assert(!node.isKind(Nodes.Draw))
        assert(!chain.has(Nodes.Draw))
    }

    @Test
    fun testUndelegateWithMultipleDelegates() {
        val node = object : DelegatingNode() {}
        val chain = layout(node)

        assert(!node.isKind(Nodes.Draw))
        assert(!chain.has(Nodes.Draw))

        val draw = node.delegateUnprotected(DrawMod())
        val layout = node.delegateUnprotected(LayoutMod())
        val semantics = node.delegateUnprotected(SemanticsMod())
        val draw2 = node.delegateUnprotected(DrawMod())

        assert(node.isKind(Nodes.Semantics))
        assert(chain.has(Nodes.Semantics))
        assert(node.asKind(Nodes.Semantics) === semantics)

        assert(node.isKind(Nodes.Draw))
        assert(chain.has(Nodes.Draw))
        assert(node.asKind(Nodes.Draw) === draw2)

        assert(node.isKind(Nodes.Layout))
        assert(chain.has(Nodes.Layout))
        assert(node.asKind(Nodes.Layout) === layout)

        node.undelegateUnprotected(semantics)

        assert(!node.isKind(Nodes.Semantics))
        assert(!chain.has(Nodes.Semantics))

        assert(node.isKind(Nodes.Draw))
        assert(chain.has(Nodes.Draw))
        assert(node.asKind(Nodes.Draw) === draw2)

        assert(node.isKind(Nodes.Layout))
        assert(chain.has(Nodes.Layout))
        assert(node.asKind(Nodes.Layout) === layout)

        node.undelegateUnprotected(draw2)

        assert(node.isKind(Nodes.Draw))
        assert(chain.has(Nodes.Draw))
        assert(node.asKind(Nodes.Draw) === draw)

        assert(node.isKind(Nodes.Layout))
        assert(chain.has(Nodes.Layout))
        assert(node.asKind(Nodes.Layout) === layout)
    }

    @Test
    fun testDelegateUndelegateInChain() {
        val a = object : DelegatingNode() {}
        val b = object : DelegatingNode() {}
        val c = object : DelegatingNode() {}
        val chain = layout(a, b, c)
        assert(!chain.has(Nodes.Draw))
        assert(!chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))

        val draw = c.delegateUnprotected(DrawMod())
        assert(chain.has(Nodes.Draw))
        assert(!chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))
        assert(a.kindSet == Nodes.Any.mask)
        assert(b.kindSet == Nodes.Any.mask)
        assert(c.kindSet == Nodes.Any or Nodes.Draw)
        assert(a.aggregateChildKindSet == Nodes.Any or Nodes.Draw)
        assert(b.aggregateChildKindSet == Nodes.Any or Nodes.Draw)
        assert(c.aggregateChildKindSet == Nodes.Any or Nodes.Draw)

        val sem = b.delegateUnprotected(SemanticsMod())
        assert(chain.has(Nodes.Draw))
        assert(chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))
        assert(a.kindSet == Nodes.Any.mask)
        assert(b.kindSet == Nodes.Any or Nodes.Semantics)
        assert(c.kindSet == Nodes.Any or Nodes.Draw)
        assert(a.aggregateChildKindSet == Nodes.Any or Nodes.Draw or Nodes.Semantics)
        assert(b.aggregateChildKindSet == Nodes.Any or Nodes.Draw or Nodes.Semantics)
        assert(c.aggregateChildKindSet == Nodes.Any or Nodes.Draw)

        val lm = a.delegateUnprotected(LayoutMod())
        assert(chain.has(Nodes.Draw))
        assert(chain.has(Nodes.Semantics))
        assert(chain.has(Nodes.Layout))
        assert(a.kindSet == Nodes.Any or Nodes.Layout)
        assert(b.kindSet == Nodes.Any or Nodes.Semantics)
        assert(c.kindSet == Nodes.Any or Nodes.Draw)
        assert(a.aggregateChildKindSet ==
            Nodes.Any or Nodes.Draw or Nodes.Semantics or Nodes.Layout)
        assert(b.aggregateChildKindSet == Nodes.Any or Nodes.Draw or Nodes.Semantics)
        assert(c.aggregateChildKindSet == Nodes.Any or Nodes.Draw)

        c.undelegateUnprotected(draw)
        assert(!chain.has(Nodes.Draw))
        assert(chain.has(Nodes.Semantics))
        assert(chain.has(Nodes.Layout))
        assert(a.kindSet == Nodes.Any or Nodes.Layout)
        assert(b.kindSet == Nodes.Any or Nodes.Semantics)
        assert(c.kindSet == Nodes.Any.mask)
        assert(a.aggregateChildKindSet == Nodes.Any or Nodes.Semantics or Nodes.Layout)
        assert(b.aggregateChildKindSet == Nodes.Any or Nodes.Semantics)
        assert(c.aggregateChildKindSet == Nodes.Any.mask)

        b.undelegateUnprotected(sem)
        assert(!chain.has(Nodes.Draw))
        assert(!chain.has(Nodes.Semantics))
        assert(chain.has(Nodes.Layout))
        assert(a.kindSet == Nodes.Any or Nodes.Layout)
        assert(b.kindSet == Nodes.Any.mask)
        assert(c.kindSet == Nodes.Any.mask)
        assert(a.aggregateChildKindSet == Nodes.Any.mask or Nodes.Layout)
        assert(b.aggregateChildKindSet == Nodes.Any.mask)
        assert(c.aggregateChildKindSet == Nodes.Any.mask)

        a.undelegateUnprotected(lm)
        assert(!chain.has(Nodes.Draw))
        assert(!chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))
        assert(a.kindSet == Nodes.Any.mask)
        assert(b.kindSet == Nodes.Any.mask)
        assert(c.kindSet == Nodes.Any.mask)
        assert(a.aggregateChildKindSet == Nodes.Any.mask)
        assert(b.aggregateChildKindSet == Nodes.Any.mask)
        assert(c.aggregateChildKindSet == Nodes.Any.mask)
    }

    @Test
    fun testDelegateUndelegateInNode() {
        val node = object : DelegatingNode() {}
        val chain = layout(node)
        assert(!chain.has(Nodes.Draw))
        assert(!chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))

        val draw = node.delegateUnprotected(DrawMod())
        assert(chain.has(Nodes.Draw))
        assert(!chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))
        assert(node.kindSet == Nodes.Any or Nodes.Draw)
        assert(node.aggregateChildKindSet == Nodes.Any or Nodes.Draw)

        val sem = node.delegateUnprotected(SemanticsMod())
        assert(chain.has(Nodes.Draw))
        assert(chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))
        assert(node.kindSet == Nodes.Any or Nodes.Draw or Nodes.Semantics)
        assert(node.aggregateChildKindSet == Nodes.Any or Nodes.Draw or Nodes.Semantics)

        val lm = node.delegateUnprotected(LayoutMod())
        assert(chain.has(Nodes.Draw))
        assert(chain.has(Nodes.Semantics))
        assert(chain.has(Nodes.Layout))
        assert(node.kindSet == Nodes.Any or Nodes.Draw or Nodes.Semantics or Nodes.Layout)
        assert(node.aggregateChildKindSet ==
            Nodes.Any or Nodes.Draw or Nodes.Semantics or Nodes.Layout)

        node.undelegateUnprotected(draw)
        assert(!chain.has(Nodes.Draw))
        assert(chain.has(Nodes.Semantics))
        assert(chain.has(Nodes.Layout))
        assert(node.kindSet == Nodes.Any or Nodes.Semantics or Nodes.Layout)
        assert(node.aggregateChildKindSet == Nodes.Any or Nodes.Semantics or Nodes.Layout)

        node.undelegateUnprotected(sem)
        assert(!chain.has(Nodes.Draw))
        assert(!chain.has(Nodes.Semantics))
        assert(chain.has(Nodes.Layout))
        assert(node.kindSet == Nodes.Any or Nodes.Layout)
        assert(node.aggregateChildKindSet == Nodes.Any or Nodes.Layout)

        node.undelegateUnprotected(lm)
        assert(!chain.has(Nodes.Draw))
        assert(!chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))
        assert(node.kindSet == Nodes.Any.mask)
        assert(node.aggregateChildKindSet == Nodes.Any.mask)
    }

    @Test
    fun testDelegateUndelegateNested() {
        val node = object : DelegatingNode() {}
        val chain = layout(node)
        assert(!chain.has(Nodes.Draw))
        assert(!chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))

        val draw = node.delegateUnprotected(DelegatedWrapper { DrawMod() })
        assert(chain.has(Nodes.Draw))
        assert(!chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))
        assert(node.kindSet == Nodes.Any or Nodes.Draw)
        assert(node.aggregateChildKindSet == Nodes.Any or Nodes.Draw)

        val sem = draw.delegateUnprotected(DelegatedWrapper { SemanticsMod() })
        assert(chain.has(Nodes.Draw))
        assert(chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))
        assert(node.kindSet == Nodes.Any or Nodes.Draw or Nodes.Semantics)
        assert(node.aggregateChildKindSet == Nodes.Any or Nodes.Draw or Nodes.Semantics)

        val lm = sem.delegateUnprotected(LayoutMod())
        assert(chain.has(Nodes.Draw))
        assert(chain.has(Nodes.Semantics))
        assert(chain.has(Nodes.Layout))
        assert(node.kindSet == Nodes.Any or Nodes.Draw or Nodes.Semantics or Nodes.Layout)
        assert(node.aggregateChildKindSet ==
            Nodes.Any or Nodes.Draw or Nodes.Semantics or Nodes.Layout)

        sem.undelegateUnprotected(lm)
        assert(chain.has(Nodes.Draw))
        assert(chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))
        assert(node.kindSet == Nodes.Any or Nodes.Draw or Nodes.Semantics)
        assert(node.aggregateChildKindSet == Nodes.Any or Nodes.Draw or Nodes.Semantics)

        draw.undelegateUnprotected(sem)
        assert(chain.has(Nodes.Draw))
        assert(!chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))
        assert(node.kindSet == Nodes.Any or Nodes.Draw)
        assert(node.aggregateChildKindSet == Nodes.Any or Nodes.Draw)

        node.undelegateUnprotected(draw)
        assert(!chain.has(Nodes.Draw))
        assert(!chain.has(Nodes.Semantics))
        assert(!chain.has(Nodes.Layout))
        assert(node.kindSet == Nodes.Any.mask)
        assert(node.aggregateChildKindSet == Nodes.Any.mask)
    }

    @Test
    fun testUndelegateForNestedDelegate() {
        val a = object : DelegatingNode() {}
        val chain = layout(a)

        val b = a.delegateUnprotected(object : DelegatingNode() {})
        val c = a.delegateUnprotected(object : DelegatingNode() {})

        assert(!chain.has(Nodes.Draw))
        assert(!a.isKind(Nodes.Draw))

        val draw = c.delegateUnprotected(DrawMod())

        assert(chain.has(Nodes.Draw))
        assert(a.isKind(Nodes.Draw))
        assert(!b.isKind(Nodes.Draw))
        assert(c.isKind(Nodes.Draw))

        c.undelegateUnprotected(draw)

        assert(!chain.has(Nodes.Draw))
        assert(!a.isKind(Nodes.Draw))
        assert(!b.isKind(Nodes.Draw))
        assert(!c.isKind(Nodes.Draw))
    }

    @Test
    fun testInvalidateInsertedNode() {
        val node = object : DelegatingNode() {
            val draw = delegate(DrawMod())
            val layout = delegate(LayoutMod())
            val semantics = delegate(SemanticsMod())
        }
        val chain = layout(node)
        chain.clearInvalidations()

        autoInvalidateNodeIncludingDelegates(node, 0.inv(), 1)

        assert(chain.drawInvalidated())
        assert(chain.layoutInvalidated())
        assert(chain.semanticsInvalidated())
    }

    @Test
    fun testNestedNodeInvalidation() {
        val node = object : DelegatingNode() {
            val wrapped = delegate(
                DelegatedWrapper {
                    DelegatedWrapper { DrawMod() }
                }
            )
        }
        val chain = layout(node)
        chain.clearInvalidations()

        autoInvalidateNodeIncludingDelegates(node, 0.inv(), 1)

        assert(chain.drawInvalidated())
        assert(!chain.layoutInvalidated())
        assert(!chain.semanticsInvalidated())
    }

    @Test
    fun testDelegateUndelegateCausesInvalidationsForDelegateKindsOnly() {
        val node = object : DelegatingNode() {
            val semantics = delegate(SemanticsMod())
        }
        val chain = layout(node)
        chain.clearInvalidations()

        val draw = node.delegateUnprotected(DrawMod())
        assert(chain.drawInvalidated())
        assert(!chain.semanticsInvalidated())

        chain.clearInvalidations()
        node.undelegateUnprotected(draw)

        assert(chain.drawInvalidated())
        assert(!chain.semanticsInvalidated())
    }

    @Test
    fun testDelegatingToLayoutNodeUpdatesCoordinators() {
        val a = DrawMod()
        val b = object : DelegatingNode() {}
        val c = LayoutMod()
        layout(a, b, c)

        val aCoord = a.requireCoordinator(Nodes.Any)
        val bCoord = b.requireCoordinator(Nodes.Any)
        val cCoord = c.requireCoordinator(Nodes.Any)

        assert(cCoord === bCoord)
        assert(cCoord === aCoord)

        val lm = b.delegateUnprotected(LayoutMod())

        assert(cCoord === c.requireCoordinator(Nodes.Any))
        assert(cCoord !== b.requireCoordinator(Nodes.Any))
        assert(cCoord !== a.requireCoordinator(Nodes.Any))

        assert(a.requireCoordinator(Nodes.Any) === b.requireCoordinator(Nodes.Any))

        b.undelegateUnprotected(lm)

        assert(c.requireCoordinator(Nodes.Any) === b.requireCoordinator(Nodes.Any))
        assert(c.requireCoordinator(Nodes.Any) === a.requireCoordinator(Nodes.Any))
    }

    @Test
    fun testDelegateAttachDetach() {
        val a = object : DelegatingNode() {}
        val b = object : DelegatingNode() {}
        val c = DrawMod()
        a.delegateUnprotected(b)
        b.delegateUnprotected(c)

        // not attached yet, but the nodes should all point to a
        assert(!a.isAttached)
        assert(!b.isAttached)
        assert(!c.isAttached)

        assert(a.node === a)
        assert(b.node === a)
        assert(c.node === a)

        val chain = layout(a)

        // attached now, nodes should still point to a
        assert(a.isAttached)
        assert(b.isAttached)
        assert(c.isAttached)

        assert(a.node === a)
        assert(b.node === a)
        assert(c.node === a)

        // detached now, nodes should still point to a
        chain.detach()

        assert(!a.isAttached)
        assert(!b.isAttached)
        assert(!c.isAttached)

        assert(a.node === a)
        assert(b.node === a)
        assert(c.node === a)

        chain.attach()

        // attached now, nodes should still point to a
        assert(a.isAttached)
        assert(b.isAttached)
        assert(c.isAttached)

        assert(a.node === a)
        assert(b.node === a)
        assert(c.node === a)

        b.undelegateUnprotected(c)
        a.undelegateUnprotected(b)

        // delegates are detached. nodes should point to themselves
        assert(a.isAttached)
        assert(!b.isAttached)
        assert(!c.isAttached)

        assert(a.node === a)
        assert(b.node === b)
        assert(c.node === c)
    }

    @Test
    fun testDelegateInAttachUndelegateInDetach() {
        val b = DrawMod()
        val a = object : DelegatingNode() {
            override fun onAttach() {
                delegate(b)
            }

            override fun onDetach() {
                undelegate(b)
            }
        }

        // not attached yet or delegated yet
        assert(!a.isAttached)
        assert(!b.isAttached)

        assert(a.node === a)
        assert(b.node === b)

        val chain = layout(a)

        // attached now, nodes should now point to a
        assert(a.isAttached)
        assert(b.isAttached)

        assert(a.node === a)
        assert(b.node === a)

        chain.detach()

        // detached AND undelegated now
        assert(!a.isAttached)
        assert(!b.isAttached)

        assert(a.node === a)
        assert(b.node === b)

        chain.attach()

        // attached and delegated now
        assert(a.isAttached)
        assert(b.isAttached)

        assert(a.node === a)
        assert(b.node === a)
    }

    @Test
    fun testDelegateInAttach() {
        val b = DrawMod()
        val a = object : DelegatingNode() {
            override fun onAttach() {
                delegate(b)
            }
        }

        // not attached yet or delegated yet
        assert(!a.isAttached)
        assert(!b.isAttached)

        assert(a.node === a)
        assert(b.node === b)

        val chain = layout(a)

        // attached now, nodes should now point to a
        assert(a.isAttached)
        assert(b.isAttached)

        assert(a.node === a)
        assert(b.node === a)

        chain.detach()

        // detached now, still delegated
        assert(!a.isAttached)
        assert(!b.isAttached)

        assert(a.node === a)
        assert(b.node === a)

        chain.attach()

        // attached, still delegated
        assert(a.isAttached)
        assert(b.isAttached)

        assert(a.node === a)
        assert(b.node === a)
    }
}

private fun NodeChain.clearInvalidations() {
    val owner = layoutNode.owner
    check(owner is MockOwner)
    owner.onRequestMeasureParams.clear()
    owner.invalidatedLayers.clear()
    owner.semanticsChanged = false
}

private fun NodeChain.layoutInvalidated(): Boolean {
    val owner = layoutNode.owner
    check(owner is MockOwner)
    return owner.onRequestMeasureParams.isNotEmpty()
}

private fun NodeChain.drawInvalidated(): Boolean {
    val owner = layoutNode.owner
    check(owner is MockOwner)
    return owner.invalidatedLayers.isNotEmpty()
}

private fun NodeChain.semanticsInvalidated(): Boolean {
    val owner = layoutNode.owner
    check(owner is MockOwner)
    return owner.semanticsChanged
}

internal fun layout(
    vararg modifiers: Modifier.Node,
    block: LayoutScope.() -> Unit = {}
): NodeChain {
    val owner = MockOwner()
    val root = LayoutNode()
    val ln = unattachedLayout(*modifiers)
    LayoutScopeImpl(ln).block()
    root.insertAt(0, ln)
    root.attach(owner)
    root.innerCoordinator.updateLayerBlock({})
    return ln.nodes
}

private fun unattachedLayout(vararg modifiers: Modifier.Node): LayoutNode {
    val ln = LayoutNode()
    var m: Modifier = Modifier
    for (node in modifiers) {
        m = m.then(NodeElement(node))
    }
    ln.nodes.updateFrom(m)
    return ln
}

internal data class NodeElement(val node: Modifier.Node) : ModifierNodeElement<Modifier.Node>() {
    override fun create(): Modifier.Node = node
    override fun update(node: Modifier.Node) {}
}

class Recorder : (Any) -> Unit {
    val recorded = mutableListOf<Any>()
    override fun invoke(p1: Any) {
        recorded.add(p1)
    }
}
internal class LayoutScopeImpl(val layout: LayoutNode) : LayoutScope {
    override fun layout(vararg modifiers: Modifier.Node, block: LayoutScope.() -> Unit) {
        val ln = unattachedLayout(*modifiers)
        LayoutScopeImpl(ln).block()
        layout.insertAt(layout.children.size, ln)
    }
}
interface LayoutScope {
    fun layout(vararg modifiers: Modifier.Node) = layout(*modifiers) {}
    fun layout(vararg modifiers: Modifier.Node, block: LayoutScope.() -> Unit)
}

internal inline fun <reified T> assertDispatchOrder(
    node: Modifier.Node,
    kind: NodeKind<T>,
    vararg expected: T
) {
    val dispatches = mutableListOf<T>()
    node.dispatchForKind(kind) {
        dispatches.add(it)
    }
    assertThat(dispatches.toTypedArray()).isEqualTo(expected)
}

class DrawMod(val id: String = "") : DrawModifierNode, Modifier.Node() {
    override fun ContentDrawScope.draw() {}
    override fun toString(): String {
        return "DrawMod($id)"
    }
}

class SemanticsMod(val id: String = "") : SemanticsModifierNode, Modifier.Node() {
    override val semanticsConfiguration: SemanticsConfiguration
        get() = SemanticsConfiguration()
    override fun toString(): String {
        return "SemanticsMod($id)"
    }
}

class LayoutMod(val id: String = "") : LayoutModifierNode, Modifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
    override fun toString(): String {
        return "LayoutMod($id)"
    }
}

class DelegatedWrapper<T : Modifier.Node>(fn: () -> T) : DelegatingNode() {
    val wrapped = delegate(fn())
    override fun toString(): String = "Wrapped<$wrapped>"
}

internal inline fun <reified T> Modifier.Node.asKind(kind: NodeKind<T>): T? {
    if (!isKind(kind)) return null
    if (this is T) return this
    if (this is DelegatingNode) {
        forEachDelegateBreadthFirst {
            if (it is T) return it
        }
    }
    return null
}

internal inline fun DelegatingNode.forEachDelegateBreadthFirst(block: (Modifier.Node) -> Unit) {
    var node: Modifier.Node? = delegate
    var queue: ArrayDeque<Modifier.Node>? = null
    while (node != null) {
        block(node)
        if (node is DelegatingNode) {
            queue = queue.enqueue(node.delegate)
        }
        node = node.child ?: queue?.removeFirst()
    }
}

private fun ArrayDeque<Modifier.Node>?.enqueue(node: Modifier.Node?): ArrayDeque<Modifier.Node>? {
    if (node == null) return this
    val queue = this ?: ArrayDeque(8)
    queue.addLast(node)
    return queue
}