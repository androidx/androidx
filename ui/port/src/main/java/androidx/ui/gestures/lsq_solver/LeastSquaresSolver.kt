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

// / Uses the least-squares algorithm to fit a polynomial to a set of data.
class LeastSquaresSolver(
    // / The x-coordinates of each data point.
    val x: DoubleArray,
    // / The y-coordinates of each data point.
    val y: DoubleArray,
    // / The weight to use for each data point.
    val w: DoubleArray
) {

    // / Fits a polynomial of the given degree to the data points.
    fun solve(degree: Int): PolynomialFit? {
        if (degree > x.size) {
            return null
        }

        val result = PolynomialFit(degree)

        // Shorthands for the purpose of notation equivalence to original C++ code.
        val m: Int = x.size
        val n: Int = degree + 1

        // Expand the X vector to a matrix A, pre-multiplied by the weights.
        val a = _Matrix(n, m)
        for (h in 0 until m) {
            a.set(0, h, w[h])
            for (i in 1 until n) {
                a.set(i, h, a.get(i - 1, h) * x[h])
            }
        }

        // Apply the Gram-Schmidt process to A to obtain its QR decomposition.

        // Orthonormal basis, column-major ordVectorer.
        val q = _Matrix(n, m)
        // Upper triangular matrix, row-major order.
        val r = _Matrix(n, n)
        for (j in 0 until n) {
            for (h in 0 until m) {
                q.set(j, h, a.get(j, h))
            }
            for (i in 0 until j) {
                val dot: Double = q.getRow(j) * q.getRow(i)
                for (h in 0 until m) {
                    q.set(j, h, q.get(j, h) - dot * q.get(i, h))
                }
            }

            val norm: Double = q.getRow(j).norm()
            if ((norm < 0.000001)) {
                // Vectors are linearly dependent or zero so no solution.
                return null
            }

            val inverseNorm: Double = 1.0 / norm
            for (h in 0 until m) {
                q.set(j, h, q.get(j, h) * inverseNorm)
            }
            for (i in 0 until n) {
                r.set(j, i, if (i < j) 0.0 else q.getRow(j) * a.getRow(i))
            }
        }

        // Solve R B = Qt W Y to find B. This is easy because R is upper triangular.
        // We just work from bottom-right to top-left calculating B's coefficients.
        val wy = _Vector(m)
        for (h in 0 until m) {
            wy[h] = y[h] * w[h]
        }
        for (i in n - 1 downTo 0) {
            result.coefficients[i] = q.getRow(i) * wy
            for (j in n - 1 downTo i + 1) {
                result.coefficients[i] -= r.get(i, j) * result.coefficients[j]
            }
            result.coefficients[i] /= r.get(i, i)
        }

        // Calculate the coefficient of determination (confidence) as:
        //   1 - (sumSquaredError / sumSquaredTotal)
        // ...where sumSquaredError is the residual sum of squares (variance of the
        // error), and sumSquaredTotal is the total sum of squares (variance of the
        // data) where each has been weighted.
        var yMean = 0.0
        for (h in 0 until m) {
            yMean += y[h]
        }
        yMean /= m

        var sumSquaredError = 0.0
        var sumSquaredTotal = 0.0
        for (h in 0 until m) {
            var term = 1.0
            var err: Double = y[h] - result.coefficients[0]
            for (i in 1 until n) {
                term *= x[h]
                err -= term * result.coefficients[i]
            }
            sumSquaredError += w[h] * w[h] * err * err
            val v = y[h] - yMean
            sumSquaredTotal += w[h] * w[h] * v * v
        }

        result.confidence =
                if (sumSquaredTotal <= 0.000001) 1.0 else 1.0 - (sumSquaredError / sumSquaredTotal)

        return result
    }
}