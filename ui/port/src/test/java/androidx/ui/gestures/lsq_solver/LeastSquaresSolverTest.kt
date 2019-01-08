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

package androidx.ui.gestures.lsq_solver

import androidx.ui.matchers.MoreOrLessEquals
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LeastSquaresSolverTest {

    @Test
    fun `Least-squares fit, linear polynomial to line`() {
        val x = floatArrayOf(0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f)
        val y = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
        val w = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)

        val solver = LeastSquaresSolver(x, y, w)
        val fit = solver.solve(1)

        assertThat(fit, `is`(notNullValue()))
        assertThat(fit!!.coefficients.size, `is`(2))
        assertThat(fit.coefficients[0], MoreOrLessEquals(1.0f))
        assertThat(fit.coefficients[1], MoreOrLessEquals(0.0f))
        assertThat(fit.confidence, MoreOrLessEquals(1.0f))
    }

    @Test
    fun `Least-squares fit, linear polynomial to sloped line`() {
        val x = floatArrayOf(0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f)
        val y = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f)
        val w = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)

        val solver = LeastSquaresSolver(x, y, w)
        val fit = solver.solve(1)

        assertThat(fit, `is`(notNullValue()))
        assertThat(fit!!.coefficients.size, `is`(2))
        assertThat(fit.coefficients[0], MoreOrLessEquals(1.0f))
        assertThat(fit.coefficients[1], MoreOrLessEquals(1.0f))
        assertThat(fit.confidence, MoreOrLessEquals(1.0f))
    }

    @Test
    fun `Least-squares fit, quadratic polynomial to line`() {
        val x = floatArrayOf(0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f)
        val y = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
        val w = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)

        val solver = LeastSquaresSolver(x, y, w)
        val fit = solver.solve(2)

        assertThat(fit, `is`(notNullValue()))
        assertThat(fit!!.coefficients.size, `is`(3))
        assertThat(fit.coefficients[0], MoreOrLessEquals(1.0f))
        assertThat(fit.coefficients[1], MoreOrLessEquals(0.0f))
        assertThat(fit.coefficients[2], MoreOrLessEquals(0.0f))
        assertThat(fit.confidence, MoreOrLessEquals(1.0f))
    }

    @Test
    fun `Least-squares fit, quadratic polynomial to sloped line`() {
        val x = floatArrayOf(0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f)
        val y = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f)
        val w = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)

        val solver = LeastSquaresSolver(x, y, w)
        val fit = solver.solve(2)

        assertThat(fit, `is`(notNullValue()))
        assertThat(fit!!.coefficients.size, `is`(3))
        assertThat(fit.coefficients[0], MoreOrLessEquals(1.0f))
        assertThat(fit.coefficients[1], MoreOrLessEquals(1.0f))
        assertThat(fit.coefficients[2], MoreOrLessEquals(0.0f))
        assertThat(fit.confidence, MoreOrLessEquals(1.0f))
    }
}