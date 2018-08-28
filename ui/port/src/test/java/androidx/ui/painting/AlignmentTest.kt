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

package androidx.ui.painting

import androidx.ui.matchers.HasOneLineDescription
import androidx.ui.matchers.InInclusiveRange
import androidx.ui.matchers.MoreOrLessEquals
import androidx.ui.painting.alignment.Alignment
import androidx.ui.painting.alignment.AlignmentDirectional
import androidx.ui.painting.alignment.AlignmentGeometry
import androidx.ui.text.TextDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AlignmentTest {

    fun approxExpect(a: Alignment, b: Alignment) {
        assertThat(a.x, MoreOrLessEquals(b.x))
        assertThat(a.y, MoreOrLessEquals(b.y))
    }

    @Test
    fun `Alignment control test`() {
        val alignment = Alignment(0.5, 0.25)

        assertThat(alignment, HasOneLineDescription)
        assertEquals(alignment.hashCode(), Alignment(0.5, 0.25).hashCode())

        assertEquals(alignment / 2.0, Alignment(0.25, 0.125))
        assertEquals(alignment.truncDiv(2.0), Alignment(0.0, 0.0))
        assertEquals(alignment % 5.0, Alignment(0.5, 0.25))
    }

    @Test
    fun `Alignment_lerp()`() {
        val a = Alignment.topLeft
        val b = Alignment.topCenter
        assertEquals(Alignment.lerp(a, b, 0.25), Alignment(-0.75, -1.0))

        assertNull(Alignment.lerp(null, null, 0.25))
        assertEquals(Alignment.lerp(null, b, 0.25), Alignment(0.0, -0.25))
        assertEquals(Alignment.lerp(a, null, 0.25), Alignment(-0.75, -0.75))
    }

    @Test
    fun `AlignmentGeometry invariants`() {
        val topStart = AlignmentDirectional.topStart
        val topEnd = AlignmentDirectional.topEnd
        val center = Alignment.center
        val topLeft = Alignment.topLeft
        val topRight = Alignment.topRight
        val numbers = listOf(0.0, 1.0, -1.0, 2.0, 0.25, 0.5, 100.0, -999.75)

        assertEquals(center, (topEnd * 0.0).add(topRight * 0.0))
        assertEquals((topEnd * 0.0).add(topRight * 0.0), topEnd.add(topRight) * 0.0)
        assertEquals(topLeft.add(topStart), topStart.add(topLeft))
        assertEquals((topStart.resolve(TextDirection.LTR)) + topLeft,
                (topStart.add(topLeft)).resolve(TextDirection.LTR))
        assertEquals((topStart.resolve(TextDirection.RTL)) + topLeft,
                (topStart.add(topLeft)).resolve(TextDirection.RTL))
        assertEquals((topStart.resolve(TextDirection.LTR)).add(topLeft),
                (topStart.add(topLeft)).resolve(TextDirection.LTR))
        assertEquals((topStart.resolve(TextDirection.RTL)).add(topLeft),
                (topStart.add(topLeft)).resolve(TextDirection.RTL))
        assertEquals(topLeft, topStart.resolve(TextDirection.LTR))
        assertEquals(topRight, topStart.resolve(TextDirection.RTL))
        assertEquals(center, topEnd * 0.0)
        assertEquals(center, topLeft * 0.0)
        assertEquals(topStart, topStart * 1.0)
        assertEquals(topEnd, topEnd * 1.0)
        assertEquals(topLeft, topLeft * 1.0)
        assertEquals(topRight, topRight * 1.0)
        for (n in numbers) {
            assertEquals(topStart * (n + 1.0), (topStart * n).add(topStart))
            assertEquals(topEnd * (n + 1.0), (topEnd * n).add(topEnd))
            for (m in numbers)
                assertEquals(topStart * (n + m), (topStart * n).add(topStart * m))
        }
        assertEquals(topStart + topStart + topStart, topStart * 3.0) // without using "add"
        for (x in TextDirection.values()) {
            assertEquals(center.add(center).resolve(x),
                    (topEnd * 0.0).add(topRight * 0.0).resolve(x))
            assertEquals(center.add(topLeft).resolve(x), (topEnd * 0.0).add(topLeft).resolve(x))
            assertEquals((center.resolve(x)).add(topLeft.resolve(x)),
                    ((topEnd * 0.0).resolve(x)).add(topLeft.resolve(x)))
            assertEquals((center.resolve(x)).add(topLeft),
                    ((topEnd * 0.0).resolve(x)).add(topLeft))
            assertEquals(center.resolve(x), (topEnd * 0.0).resolve(x))
        }
        assertNotEquals(topLeft, topStart)
        assertNotEquals(topLeft, topEnd)
        assertNotEquals(topRight, topStart)
        assertNotEquals(topRight, topEnd)
        assertNotEquals(topLeft, topStart.add(topLeft))
        assertNotEquals(topStart, topStart.add(topLeft))
    }

    @Test
    fun `AlignmentGeometry_resolve()`() {
        assertEquals(AlignmentDirectional(0.25, 0.3).resolve(TextDirection.LTR),
                Alignment(0.25, 0.3))
        assertEquals(AlignmentDirectional(0.25, 0.3).resolve(TextDirection.RTL),
                Alignment(-0.25, 0.3))
        assertEquals(AlignmentDirectional(-0.25, 0.3).resolve(TextDirection.LTR),
                Alignment(-0.25, 0.3))
        assertEquals(AlignmentDirectional(-0.25, 0.3).resolve(TextDirection.RTL),
                Alignment(0.25, 0.3))
        assertEquals(AlignmentDirectional(1.25, 0.3).resolve(TextDirection.LTR),
                Alignment(1.25, 0.3))
        assertEquals(AlignmentDirectional(1.25, 0.3).resolve(TextDirection.RTL),
                Alignment(-1.25, 0.3))
        assertEquals(AlignmentDirectional(0.5, -0.3).resolve(TextDirection.LTR),
                Alignment(0.5, -0.3))
        assertEquals(AlignmentDirectional(0.5, -0.3).resolve(TextDirection.RTL),
                Alignment(-0.5, -0.3))
        assertEquals(AlignmentDirectional(0.0, 0.0).resolve(TextDirection.LTR),
                Alignment(0.0, 0.0))
        assertEquals(AlignmentDirectional(0.0, 0.0).resolve(TextDirection.RTL),
                Alignment(0.0, 0.0))
        assertEquals(AlignmentDirectional(1.0, 1.0).resolve(TextDirection.LTR),
                Alignment(1.0, 1.0))
        assertEquals(AlignmentDirectional(1.0, 1.0).resolve(TextDirection.RTL),
                Alignment(-1.0, 1.0))
        assertEquals(AlignmentDirectional(1.0, 2.0),
                AlignmentDirectional(1.0, 2.0))
        assertNotEquals(AlignmentDirectional(1.0, 2.0),
                AlignmentDirectional(2.0, 1.0))
        assertEquals(AlignmentDirectional(-1.0, 0.0).resolve(TextDirection.LTR),
                AlignmentDirectional(1.0, 0.0).resolve(TextDirection.RTL))
        assertNotEquals(AlignmentDirectional(-1.0, 0.0).resolve(TextDirection.LTR),
                AlignmentDirectional(1.0, 0.0).resolve(TextDirection.LTR))
        assertNotEquals(AlignmentDirectional(1.0, 0.0).resolve(TextDirection.LTR),
                AlignmentDirectional(1.0, 0.0).resolve(TextDirection.RTL))
    }

    @Test
    fun `AlignmentGeometry_lerp ad hoc tests`() {
        val mixed1 = Alignment(10.0, 20.0).add(AlignmentDirectional(30.0, 50.0))
        val mixed2 = Alignment(70.0, 110.0).add(AlignmentDirectional(130.0, 170.0))
        val mixed3 = Alignment(25.0, 42.5).add(AlignmentDirectional(55.0, 80.0))

        for (direction in TextDirection.values()) {
            assertEquals(mixed1.resolve(direction),
                    AlignmentGeometry.lerp(mixed1, mixed2, 0.0)!!.resolve(direction))
            assertEquals(mixed2.resolve(direction),
                    AlignmentGeometry.lerp(mixed1, mixed2, 1.0)!!.resolve(direction))
            assertEquals(mixed3.resolve(direction),
                    AlignmentGeometry.lerp(mixed1, mixed2, 0.25)!!.resolve(direction))
        }
    }

    @Test
    fun `lerp commutes with resolve`() {
        val offsets = listOf(
                Alignment.topLeft,
                Alignment.topCenter,
                Alignment.topRight,
                AlignmentDirectional.topStart,
                AlignmentDirectional.topCenter,
                AlignmentDirectional.topEnd,
                Alignment.centerLeft,
                Alignment.center,
                Alignment.centerRight,
                AlignmentDirectional.centerStart,
                AlignmentDirectional.center,
                AlignmentDirectional.centerEnd,
                Alignment.bottomLeft,
                Alignment.bottomCenter,
                Alignment.bottomRight,
                AlignmentDirectional.bottomStart,
                AlignmentDirectional.bottomCenter,
                AlignmentDirectional.bottomEnd,
                Alignment(-1.0, 0.65),
                AlignmentDirectional(-1.0, 0.45),
                AlignmentDirectional(0.125, 0.625),
                Alignment(0.25, 0.875),
                Alignment(0.0625, 0.5625).add(AlignmentDirectional(0.1875, 0.6875)),
                AlignmentDirectional(2.0, 3.0),
                Alignment(2.0, 3.0),
                Alignment(2.0, 3.0).add(AlignmentDirectional(5.0, 3.0)),
                Alignment(10.0, 20.0).add(AlignmentDirectional(30.0, 50.0)),
                Alignment(70.0, 110.0).add(AlignmentDirectional(130.0, 170.0)),
                Alignment(25.0, 42.5).add(AlignmentDirectional(55.0, 80.0)),
                null
        )

        val times = listOf(0.25, 0.5, 0.75)

        for (direction in TextDirection.values()) {
            val defaultValue = AlignmentDirectional.center.resolve(direction)
            for (a in offsets) {
                val resolvedA = a?.resolve(direction) ?: defaultValue
                for (b in offsets) {
                    val resolvedB = b?.resolve(direction) ?: defaultValue
                    approxExpect(Alignment.lerp(resolvedA, resolvedB, 0.0)!!, resolvedA)
                    approxExpect(Alignment.lerp(resolvedA, resolvedB, 1.0)!!, resolvedB)
                    approxExpect((AlignmentGeometry.lerp(a, b, 0.0) ?: defaultValue)
                            .resolve(direction),
                            resolvedA)
                    approxExpect(
                            (AlignmentGeometry.lerp(a, b, 1.0) ?: defaultValue).resolve(direction),
                            resolvedB)
                    for (t in times) {
                        assert(t > 0.0)
                        assert(t < 1.0)
                        val value = (AlignmentGeometry.lerp(a, b, t) ?: defaultValue).resolve(
                                direction)
                        approxExpect(value, Alignment.lerp(resolvedA, resolvedB, t)!!)
                        val minDX = Math.min(resolvedA.x, resolvedB.x)
                        val maxDX = Math.max(resolvedA.x, resolvedB.x)
                        val minDY = Math.min(resolvedA.y, resolvedB.y)
                        val maxDY = Math.max(resolvedA.y, resolvedB.y)
                        assertThat(value.x, InInclusiveRange(minDX, maxDX))
                        assertThat(value.y, InInclusiveRange(minDY, maxDY))
                    }
                }
            }
        }
    }

    @Test
    fun `AlignmentGeometry add_subtract`() {
        val directional = AlignmentDirectional(1.0, 2.0)
        val normal = Alignment(3.0, 5.0)
        assertEquals(directional.add(normal).resolve(TextDirection.LTR), Alignment(4.0, 7.0))
        assertEquals(directional.add(normal).resolve(TextDirection.RTL), Alignment(2.0, 7.0))
        assertEquals(normal * 2.0, normal.add(normal))
        assertEquals(directional * 2.0, directional.add(directional))
    }

    @Test
    fun `AlignmentGeometry operators`() {
        assertEquals(AlignmentDirectional(1.0, 2.0) * 2.0,
                AlignmentDirectional(2.0, 4.0))
        assertEquals(AlignmentDirectional(1.0, 2.0) / 2.0,
                AlignmentDirectional(0.5, 1.0))
        assertEquals(AlignmentDirectional(1.0, 2.0) % 2.0,
                AlignmentDirectional(1.0, 0.0))
        assertEquals(AlignmentDirectional(1.0, 2.0).truncDiv(2.0),
                AlignmentDirectional(0.0, 1.0))
        for (direction in TextDirection.values()) {
            assertEquals(
                    Alignment.center.add(AlignmentDirectional(1.0, 2.0) * 2.0).resolve(direction),
                    AlignmentDirectional(2.0, 4.0).resolve(direction))
            assertEquals(
                    Alignment.center.add(AlignmentDirectional(1.0, 2.0) / 2.0).resolve(direction),
                    AlignmentDirectional(0.5, 1.0).resolve(direction))
            assertEquals(
                    Alignment.center.add(AlignmentDirectional(1.0, 2.0) % 2.0).resolve(direction),
                    AlignmentDirectional(1.0, 0.0).resolve(direction))
            assertEquals(Alignment.center.add(AlignmentDirectional(1.0, 2.0)
                    .truncDiv(2.0)).resolve(
                    direction), AlignmentDirectional(0.0, 1.0).resolve(direction))
        }
        assertEquals(Alignment(1.0, 2.0) * 2.0, Alignment(2.0, 4.0))
        assertEquals(Alignment(1.0, 2.0) / 2.0, Alignment(0.5, 1.0))
        assertEquals(Alignment(1.0, 2.0) % 2.0, Alignment(1.0, 0.0))
        assertEquals(Alignment(1.0, 2.0).truncDiv(2.0), Alignment(0.0, 1.0))
    }

    @Test
    fun `AlignmentGeometry operators2`() {
        assertEquals(Alignment(1.0, 2.0) + Alignment(3.0, 5.0),
                Alignment(4.0, 7.0))
        assertEquals(Alignment(1.0, 2.0) - Alignment(3.0, 5.0),
                Alignment(-2.0, -3.0))
        assertEquals(AlignmentDirectional(1.0, 2.0) +
                AlignmentDirectional(3.0, 5.0),
                AlignmentDirectional(4.0, 7.0))
        assertEquals(AlignmentDirectional(1.0, 2.0) -
                AlignmentDirectional(3.0, 5.0),
                AlignmentDirectional(-2.0, -3.0))
    }

    @Test
    fun `AlignmentGeometry toString`() {
        assertEquals(Alignment(1.0001, 2.0001).toString(), "Alignment(1.0, 2.0)")
        assertEquals(Alignment(0.0, 0.0).toString(), "center")
        assertEquals(Alignment(-1.0, 1.0)
                .add(AlignmentDirectional(1.0, 0.0)).toString(),
                "bottomLeft + AlignmentDirectional.centerEnd")
        assertEquals(Alignment(0.0001, 0.0001).toString(), "Alignment(0.0, 0.0)")
        assertEquals(Alignment(0.0, 0.0).toString(), "center")
        assertEquals(AlignmentDirectional(0.0, 0.0).toString(),
                "AlignmentDirectional.center")
        assertEquals(Alignment(1.0, 1.0)
                .add(AlignmentDirectional(1.0, 1.0)).toString(),
                "Alignment(1.0, 2.0) + AlignmentDirectional.centerEnd")
    }
}