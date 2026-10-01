/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGAffineTransformMake
import platform.CoreGraphics.CGPointApplyAffineTransform
import platform.CoreGraphics.CGPointMake

@OptIn(ExperimentalForeignApi::class)
class AffineTransformExtensionsTest {
    @Test
    fun toCgAffineTransform_resultsInEquivalentVecTransformations() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)

        val inputVec = ImmutableVec(1f, 2f)
        val outputVec = MutableVec()
        affineTransform.applyTransform(inputVec, outputVec)

        val cgAffineTransform = affineTransform.toCgAffineTransform()
        val cgPoint = CGPointMake(inputVec.x.toDouble(), inputVec.y.toDouble())
        val outputCgPoint = CGPointApplyAffineTransform(cgPoint, cgAffineTransform)
        outputCgPoint.useContents {
            assertThat(outputVec).isEqualTo(ImmutableVec(x.toFloat(), y.toFloat()))
        }
    }

    @Test
    fun ImmutableAffineTransform_from_resultsInEquivalentVecTransformations() {
        val cgAffineTransform =
            CGAffineTransformMake(
                A.toDouble(),
                B.toDouble(),
                C.toDouble(),
                D.toDouble(),
                E.toDouble(),
                F.toDouble(),
            )

        val inputVec = ImmutableVec(1f, 2f)
        val cgPoint = CGPointMake(inputVec.x.toDouble(), inputVec.y.toDouble())
        val outputCgPoint = CGPointApplyAffineTransform(cgPoint, cgAffineTransform)

        val affineTransform = ImmutableAffineTransform.from(cgAffineTransform)
        val outputVec = MutableVec()
        affineTransform.applyTransform(inputVec, outputVec)

        outputCgPoint.useContents {
            assertThat(outputVec).isEqualTo(ImmutableVec(x.toFloat(), y.toFloat()))
        }
    }

    @Test
    fun MutableAffineTransform_populateFrom_resultsInEquivalentVecTransformations() {
        val cgAffineTransform =
            CGAffineTransformMake(
                A.toDouble(),
                B.toDouble(),
                C.toDouble(),
                D.toDouble(),
                E.toDouble(),
                F.toDouble(),
            )

        val inputVec = ImmutableVec(1f, 2f)
        val cgPoint = CGPointMake(inputVec.x.toDouble(), inputVec.y.toDouble())
        val outputCgPoint = CGPointApplyAffineTransform(cgPoint, cgAffineTransform)

        val affineTransform = MutableAffineTransform()
        affineTransform.populateFrom(cgAffineTransform)
        val outputVec = MutableVec()
        affineTransform.applyTransform(inputVec, outputVec)

        outputCgPoint.useContents {
            assertThat(outputVec).isEqualTo(ImmutableVec(x.toFloat(), y.toFloat()))
        }
    }

    companion object {
        private const val A = 1f
        private const val B = 2f
        private const val C = -3f
        private const val D = -4f
        private const val E = 5f
        private const val F = 6f
    }
}
