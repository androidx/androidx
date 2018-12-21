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
import androidx.ui.engine.text.TextDirection
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
        val alignment = Alignment(0.5f, 0.25f)

        assertThat(alignment, HasOneLineDescription)
        assertEquals(alignment.hashCode(), Alignment(0.5f, 0.25f).hashCode())

        assertEquals(alignment / 2.0f, Alignment(0.25f, 0.125f))
        assertEquals(alignment.truncDiv(2.0f), Alignment(0.0f, 0.0f))
        assertEquals(alignment % 5.0f, Alignment(0.5f, 0.25f))
    }

    @Test
    fun `Alignment_lerp()`() {
        val a = Alignment.topLeft
        val b = Alignment.topCenter
        assertEquals(Alignment.lerp(a, b, 0.25f), Alignment(-0.75f, -1.0f))

        assertNull(Alignment.lerp(null, null, 0.25f))
        assertEquals(Alignment.lerp(null, b, 0.25f), Alignment(0.0f, -0.25f))
        assertEquals(Alignment.lerp(a, null, 0.25f), Alignment(-0.75f, -0.75f))
    }

    @Test
    fun `AlignmentGeometry invariants`() {
        val topStart = AlignmentDirectional.topStart
        val topEnd = AlignmentDirectional.topEnd
        val center = Alignment.center
        val topLeft = Alignment.topLeft
        val topRight = Alignment.topRight
        val numbers = listOf(0.0f, 1.0f, -1.0f, 2.0f, 0.25f, 0.5f, 100.0f, -999.75f)

        assertEquals(center, (topEnd * 0.0f).add(topRight * 0.0f))
        assertEquals((topEnd * 0.0f).add(topRight * 0.0f), topEnd.add(topRight) * 0.0f)
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
        assertEquals(center, topEnd * 0.0f)
        assertEquals(center, topLeft * 0.0f)
        assertEquals(topStart, topStart * 1.0f)
        assertEquals(topEnd, topEnd * 1.0f)
        assertEquals(topLeft, topLeft * 1.0f)
        assertEquals(topRight, topRight * 1.0f)
        for (n in numbers) {
            assertEquals(topStart * (n + 1.0f), (topStart * n).add(topStart))
            assertEquals(topEnd * (n + 1.0f), (topEnd * n).add(topEnd))
            for (m in numbers)
                assertEquals(topStart * (n + m), (topStart * n).add(topStart * m))
        }
        assertEquals(topStart + topStart + topStart, topStart * 3.0f) // without using "add"
        for (x in TextDirection.values()) {
            assertEquals(center.add(center).resolve(x),
                    (topEnd * 0.0f).add(topRight * 0.0f).resolve(x))
            assertEquals(center.add(topLeft).resolve(x), (topEnd * 0.0f).add(topLeft).resolve(x))
            assertEquals((center.resolve(x)).add(topLeft.resolve(x)),
                    ((topEnd * 0.0f).resolve(x)).add(topLeft.resolve(x)))
            assertEquals((center.resolve(x)).add(topLeft),
                    ((topEnd * 0.0f).resolve(x)).add(topLeft))
            assertEquals(center.resolve(x), (topEnd * 0.0f).resolve(x))
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
        assertEquals(AlignmentDirectional(0.25f, 0.3f).resolve(TextDirection.LTR),
                Alignment(0.25f, 0.3f))
        assertEquals(AlignmentDirectional(0.25f, 0.3f).resolve(TextDirection.RTL),
                Alignment(-0.25f, 0.3f))
        assertEquals(AlignmentDirectional(-0.25f, 0.3f).resolve(TextDirection.LTR),
                Alignment(-0.25f, 0.3f))
        assertEquals(AlignmentDirectional(-0.25f, 0.3f).resolve(TextDirection.RTL),
                Alignment(0.25f, 0.3f))
        assertEquals(AlignmentDirectional(1.25f, 0.3f).resolve(TextDirection.LTR),
                Alignment(1.25f, 0.3f))
        assertEquals(AlignmentDirectional(1.25f, 0.3f).resolve(TextDirection.RTL),
                Alignment(-1.25f, 0.3f))
        assertEquals(AlignmentDirectional(0.5f, -0.3f).resolve(TextDirection.LTR),
                Alignment(0.5f, -0.3f))
        assertEquals(AlignmentDirectional(0.5f, -0.3f).resolve(TextDirection.RTL),
                Alignment(-0.5f, -0.3f))
        assertEquals(AlignmentDirectional(0.0f, 0.0f).resolve(TextDirection.LTR),
                Alignment(0.0f, 0.0f))
        assertEquals(AlignmentDirectional(0.0f, 0.0f).resolve(TextDirection.RTL),
                Alignment(0.0f, 0.0f))
        assertEquals(AlignmentDirectional(1.0f, 1.0f).resolve(TextDirection.LTR),
                Alignment(1.0f, 1.0f))
        assertEquals(AlignmentDirectional(1.0f, 1.0f).resolve(TextDirection.RTL),
                Alignment(-1.0f, 1.0f))
        assertEquals(AlignmentDirectional(1.0f, 2.0f),
                AlignmentDirectional(1.0f, 2.0f))
        assertNotEquals(AlignmentDirectional(1.0f, 2.0f),
                AlignmentDirectional(2.0f, 1.0f))
        assertEquals(AlignmentDirectional(-1.0f, 0.0f).resolve(TextDirection.LTR),
                AlignmentDirectional(1.0f, 0.0f).resolve(TextDirection.RTL))
        assertNotEquals(AlignmentDirectional(-1.0f, 0.0f).resolve(TextDirection.LTR),
                AlignmentDirectional(1.0f, 0.0f).resolve(TextDirection.LTR))
        assertNotEquals(AlignmentDirectional(1.0f, 0.0f).resolve(TextDirection.LTR),
                AlignmentDirectional(1.0f, 0.0f).resolve(TextDirection.RTL))
    }

    @Test
    fun `AlignmentGeometry_lerp ad hoc tests`() {
        val mixed1 = Alignment(10.0f, 20.0f).add(AlignmentDirectional(30.0f, 50.0f))
        val mixed2 = Alignment(70.0f, 110.0f).add(AlignmentDirectional(130.0f, 170.0f))
        val mixed3 = Alignment(25.0f, 42.5f).add(AlignmentDirectional(55.0f, 80.0f))

        for (direction in TextDirection.values()) {
            assertEquals(mixed1.resolve(direction),
                    AlignmentGeometry.lerp(mixed1, mixed2, 0.0f)!!.resolve(direction))
            assertEquals(mixed2.resolve(direction),
                    AlignmentGeometry.lerp(mixed1, mixed2, 1.0f)!!.resolve(direction))
            assertEquals(mixed3.resolve(direction),
                    AlignmentGeometry.lerp(mixed1, mixed2, 0.25f)!!.resolve(direction))
        }
    }

    @Test
    fun `lerp commutes with resolve`() {
        val ofsets = listOf(
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
                Alignment(-1.0f, 0.65f),
                AlignmentDirectional(-1.0f, 0.45f),
                AlignmentDirectional(0.125f, 0.625f),
                Alignment(0.25f, 0.875f),
                Alignment(0.0625f, 0.5625f).add(AlignmentDirectional(0.1875f, 0.6875f)),
                AlignmentDirectional(2.0f, 3.0f),
                Alignment(2.0f, 3.0f),
                Alignment(2.0f, 3.0f).add(AlignmentDirectional(5.0f, 3.0f)),
                Alignment(10.0f, 20.0f).add(AlignmentDirectional(30.0f, 50.0f)),
                Alignment(70.0f, 110.0f).add(AlignmentDirectional(130.0f, 170.0f)),
                Alignment(25.0f, 42.5f).add(AlignmentDirectional(55.0f, 80.0f)),
                null
        )

        val times = listOf(0.25f, 0.5f, 0.75f)

        for (direction in TextDirection.values()) {
            val defaultValue = AlignmentDirectional.center.resolve(direction)
            for (a in ofsets) {
                val resolvedA = a?.resolve(direction) ?: defaultValue
                for (b in ofsets) {
                    val resolvedB = b?.resolve(direction) ?: defaultValue
                    approxExpect(Alignment.lerp(resolvedA, resolvedB, 0.0f)!!, resolvedA)
                    approxExpect(Alignment.lerp(resolvedA, resolvedB, 1.0f)!!, resolvedB)
                    approxExpect((AlignmentGeometry.lerp(a, b, 0.0f) ?: defaultValue)
                            .resolve(direction),
                            resolvedA)
                    approxExpect(
                            (AlignmentGeometry.lerp(a, b, 1.0f) ?: defaultValue).resolve(direction),
                            resolvedB)
                    for (t in times) {
                        assert(t > 0.0f)
                        assert(t < 1.0f)
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
        val directional = AlignmentDirectional(1.0f, 2.0f)
        val normal = Alignment(3.0f, 5.0f)
        assertEquals(directional.add(normal).resolve(TextDirection.LTR), Alignment(4.0f, 7.0f))
        assertEquals(directional.add(normal).resolve(TextDirection.RTL), Alignment(2.0f, 7.0f))
        assertEquals(normal * 2.0f, normal.add(normal))
        assertEquals(directional * 2.0f, directional.add(directional))
    }

    @Test
    fun `AlignmentGeometry operators`() {
        assertEquals(AlignmentDirectional(1.0f, 2.0f) * 2.0f,
                AlignmentDirectional(2.0f, 4.0f))
        assertEquals(AlignmentDirectional(1.0f, 2.0f) / 2.0f,
                AlignmentDirectional(0.5f, 1.0f))
        assertEquals(AlignmentDirectional(1.0f, 2.0f) % 2.0f,
                AlignmentDirectional(1.0f, 0.0f))
        assertEquals(AlignmentDirectional(1.0f, 2.0f).truncDiv(2.0f),
                AlignmentDirectional(0.0f, 1.0f))
        for (direction in TextDirection.values()) {
            assertEquals(
                    Alignment.center.add(AlignmentDirectional(1f, 2f) * 2f).resolve(direction),
                    AlignmentDirectional(2.0f, 4.0f).resolve(direction))
            assertEquals(
                    Alignment.center.add(AlignmentDirectional(1f, 2f) / 2f).resolve(direction),
                    AlignmentDirectional(0.5f, 1.0f).resolve(direction))
            assertEquals(
                    Alignment.center.add(AlignmentDirectional(1f, 2f) % 2f).resolve(direction),
                    AlignmentDirectional(1.0f, 0.0f).resolve(direction))
            assertEquals(Alignment.center.add(AlignmentDirectional(1.0f, 2.0f)
                    .truncDiv(2.0f)).resolve(
                    direction), AlignmentDirectional(0.0f, 1.0f).resolve(direction))
        }
        assertEquals(Alignment(1.0f, 2.0f) * 2.0f, Alignment(2.0f, 4.0f))
        assertEquals(Alignment(1.0f, 2.0f) / 2.0f, Alignment(0.5f, 1.0f))
        assertEquals(Alignment(1.0f, 2.0f) % 2.0f, Alignment(1.0f, 0.0f))
        assertEquals(Alignment(1.0f, 2.0f).truncDiv(2.0f), Alignment(0.0f, 1.0f))
    }

    @Test
    fun `AlignmentGeometry operators2`() {
        assertEquals(Alignment(1.0f, 2.0f) + Alignment(3.0f, 5.0f),
                Alignment(4.0f, 7.0f))
        assertEquals(Alignment(1.0f, 2.0f) - Alignment(3.0f, 5.0f),
                Alignment(-2.0f, -3.0f))
        assertEquals(AlignmentDirectional(1.0f, 2.0f) +
                AlignmentDirectional(3.0f, 5.0f),
                AlignmentDirectional(4.0f, 7.0f))
        assertEquals(AlignmentDirectional(1.0f, 2.0f) -
                AlignmentDirectional(3.0f, 5.0f),
                AlignmentDirectional(-2.0f, -3.0f))
    }

    @Test
    fun `AlignmentGeometry toString`() {
        assertEquals(Alignment(1.0001f, 2.0001f).toString(), "Alignment(1.0, 2.0)")
        assertEquals(Alignment(0.0f, 0.0f).toString(), "center")
        assertEquals(Alignment(-1.0f, 1.0f)
                .add(AlignmentDirectional(1.0f, 0.0f)).toString(),
                "bottomLeft + AlignmentDirectional.centerEnd")
        assertEquals(Alignment(0.0001f, 0.0001f).toString(), "Alignment(0.0, 0.0)")
        assertEquals(Alignment(0.0f, 0.0f).toString(), "center")
        assertEquals(AlignmentDirectional(0.0f, 0.0f).toString(),
                "AlignmentDirectional.center")
        assertEquals(Alignment(1.0f, 1.0f)
                .add(AlignmentDirectional(1.0f, 1.0f)).toString(),
                "Alignment(1.0, 2.0) + AlignmentDirectional.centerEnd")
    }
}