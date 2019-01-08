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

package androidx.ui.painting.matrixutils

import androidx.ui.engine.geometry.Offset
import androidx.ui.matchers.MoreOrLessEquals
import androidx.ui.vectormath64.Matrix4
import androidx.ui.vectormath64.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MatrixUtilsTest {

    @Test
    fun `MatrixUtils matrixEquals`() {
        val values = floatArrayOf(0.0f, 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f,
            3.5f, 3.0f, 2.5f, 2.0f, 1.5f, 1.0f, 0.5f, 0.0f)
        val mat1 = Matrix4.of(*values)
        val mat2 = Matrix4.of(*values)
        assertEquals(mat1, mat2)

        mat2[0][0] = 0.5f
        assertNotEquals(mat1, mat2)
    }

    @Test
    fun `MatrixUtils getAsTranslation`() {
        var test: Matrix4?
        test = Matrix4.identity()
        assertEquals(Offset.zero, test.getAsTranslation())
        test = Matrix4.zero()
        assertNull(test.getAsTranslation())
        test = Matrix4.rotationX(1.0f)
        assertNull(test.getAsTranslation())
        test = Matrix4.rotationZ(1.0f)
        assertNull(test.getAsTranslation())
        test = Matrix4.translationValues(1.0f, 2.0f, 0.0f)
        assertEquals(Offset(1.0f, 2.0f), test.getAsTranslation())
        test = Matrix4.translationValues(1.0f, 2.0f, 3.0f)
        assertNull(test.getAsTranslation())

        test = Matrix4.identity()
        assertEquals(Offset.zero, test.getAsTranslation())
        test.rotateZ(2.0f)
        assertNull(test.getAsTranslation())

        test = Matrix4.identity()
        assertEquals(Offset.zero, test.getAsTranslation())
        test.scale(2.0f)
        assertNull(test.getAsTranslation())

        test = Matrix4.identity()
        assertEquals(Offset.zero, test.getAsTranslation())
        test.translate(2.0f, -2.0f)
        assertEquals(Offset(2.0f, -2.0f), test.getAsTranslation())
        test.translate(4.0f, 8.0f)
        assertEquals(Offset(6.0f, 6.0f), test.getAsTranslation())
    }

    @Test
    fun `cylindricalProjectionTransform identity`() {
        val initialState = createCylindricalProjectionTransform(0.0f, 0.0f, 0.0f)
        assertEquals(Matrix4.identity(), initialState)
    }

    @Test
    fun `cylindricalProjectionTransform rotate with no radius`() {
        val simpleRotate = createCylindricalProjectionTransform(0.0f, PI / 2.0f, 0.0f)
        assertEquals(Matrix4.rotationX(PI / 2.0f), simpleRotate)
    }

    @Test
    fun `cylindricalProjectionTransform radius does not change scale`() {
        val noRotation = createCylindricalProjectionTransform(1000000.0f, 0.0f, 0.0f)
        assertEquals(Matrix4.identity(), noRotation)
    }

    @Test
    fun `cylindricalProjectionTransform calculation spot check`() {
        val actual = createCylindricalProjectionTransform(100.0f, PI / 3.0f, 0.001f).m4storage

        val expected = listOf(1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.5f, 0.8660254037844386f, -0.0008660254037844386f,
                0.0f, -0.8660254037844386f, 0.5f, -0.0005f,
                0.0f, -86.60254037844386f, -50.0f, 1.05f)

        assertEquals(16, actual.size)
        for (i in 0..15) {
            assertThat(expected[i], MoreOrLessEquals(actual[i]))
        }
    }
}