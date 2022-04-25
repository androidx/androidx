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

package androidx.glance

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class ApplierTest {

    @Test
    fun onClear() {
        val root = RootEmittable()
        val applier = Applier(root)
        updateApplier(applier)
        applier.clear()

        assertThat(applier.current).isEqualTo(root)
        assertThat(root.children).isEmpty()
    }

    @Test
    fun insertTopDown() {
        val root = RootEmittable()
        val applier = Applier(root)

        updateApplier(applier)

        assertThat(root.children).hasSize(3)
        assertThat(root.children[0]).isInstanceOf(LeafEmittable::class.java)
        assertThat(root.children[2]).isInstanceOf(LeafEmittable::class.java)
        assertThat(root.children[1]).isInstanceOf(MiddleEmittable::class.java)
        assertThat((root.children[1] as MiddleEmittable).children).hasSize(1)
    }

    @Test
    fun insertBottomUp() {
        val root = RootEmittable()
        val applier = Applier(root)

        applier.insertBottomUp(0, LeafEmittable())

        assertThat(root.children).isEmpty()
    }

    @Test
    fun move() {
        val root = RootEmittable()
        val applier = Applier(root)
        updateApplier(applier)
        applier.up()

        applier.move(0, 2, 1)

        assertThat(root.children).hasSize(3)
        assertThat(root.children[1]).isInstanceOf(LeafEmittable::class.java)
        assertThat(root.children[2]).isInstanceOf(LeafEmittable::class.java)
        assertThat(root.children[0]).isInstanceOf(MiddleEmittable::class.java)
        assertThat((root.children[0] as MiddleEmittable).children).hasSize(1)
    }

    @Test
    fun remove() {
        val root = RootEmittable()
        val applier = Applier(root)
        updateApplier(applier)
        applier.up()

        applier.remove(1, 2)

        assertThat(root.children).hasSize(1)
        assertThat(root.children[0]).isInstanceOf(LeafEmittable::class.java)
    }

    @Test
    fun limitDepth() {
        val root = RootEmittable(maxDepth = 1)
        val applier = Applier(root)

        applier.insertTopDown(0, LeafEmittable())
        val m = MiddleEmittable()
        applier.insertTopDown(1, m)
        applier.down(m)
        val ex = assertFailsWith<IllegalArgumentException> {
            applier.insertTopDown(0, LeafEmittable())
        }
        assertThat(ex.message).isEqualTo(
            "Too many embedded views for the current surface. " +
                "The maximum depth is: 1"
        )
    }

    @Test
    fun resetDepth() {
        val root = RootEmittable(maxDepth = 1)
        val applier = Applier(root)

        applier.insertTopDown(0, LeafEmittable())
        val r = ResetsDepthEmittable()
        applier.insertTopDown(1, r)
        applier.down(r)
        val m = MiddleEmittable()
        applier.insertTopDown(0, m)
        applier.down(m)

        val ex = assertFailsWith<IllegalArgumentException> {
            applier.insertTopDown(0, LeafEmittable())
        }

        assertThat(ex.message).isEqualTo(
            "Too many embedded views for the current surface. The maximum depth is: 1"
        )
    }

    private companion object {
        fun updateApplier(applier: Applier) {
            val middle = MiddleEmittable()
            applier.insertTopDown(0, LeafEmittable())
            applier.insertTopDown(1, middle)
            applier.insertTopDown(2, LeafEmittable())
            applier.down(middle)
            applier.insertTopDown(0, LeafEmittable())
        }
    }
}

private class RootEmittable(maxDepth: Int = Int.MAX_VALUE) : EmittableWithChildren(maxDepth) {
    override var modifier: GlanceModifier = GlanceModifier
}

private class MiddleEmittable : EmittableWithChildren() {
    override var modifier: GlanceModifier = GlanceModifier
}

private class LeafEmittable : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
}

private class ResetsDepthEmittable : EmittableWithChildren(resetsDepthForChildren = true) {
    override var modifier: GlanceModifier = GlanceModifier
}
