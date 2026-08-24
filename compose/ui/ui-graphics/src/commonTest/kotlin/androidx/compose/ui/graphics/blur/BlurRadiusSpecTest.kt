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

package androidx.compose.ui.graphics.blur

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * Unit tests for the pure-Kotlin layer of the progressive blur API, including [BlurStop] and every
 * [BlurRadiusSpec] factory variant.
 *
 * These tests focus on structural equality, hash code consistency, and factory validation rules.
 * They also verify defensive copies and radial gradient extent resolution without performing any
 * rendering.
 */
class BlurRadiusSpecTest {

    private val density = Density(2f)
    private val size = Size(200f, 100f)

    private fun stops(count: Int): List<BlurStop> =
        List(count) { i -> BlurStop(fraction = i / (count - 1f), radius = (i * 4).dp) }

    /** Asserts [a] equals [b] symmetrically and their hash codes are consistent. */
    private fun assertEqualWithConsistentHashCode(a: Any, b: Any) {
        assertEquals(a, b)
        assertEquals(b, a)
        assertEquals(a.hashCode(), b.hashCode())
    }

    private fun assertThrowsWithMessage(expectedMessage: String, block: () -> Unit) {
        val exception = assertFailsWith<IllegalArgumentException>(block = block)
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun blurStop_structuralEquality() {
        assertEqualWithConsistentHashCode(
            BlurStop(fraction = 0.5f, radius = 8.dp),
            BlurStop(fraction = 0.5f, radius = 8.dp),
        )
    }

    @Test
    fun blurStop_inequality() {
        val base = BlurStop(fraction = 0.5f, radius = 8.dp)
        assertNotEquals(base, BlurStop(fraction = 0.25f, radius = 8.dp))
        assertNotEquals(base, BlurStop(fraction = 0.5f, radius = 12.dp))
    }

    @Test
    fun blurStop_toString() {
        assertEquals("BlurStop(fraction=0.5, radius=8.0.dp)", BlurStop(0.5f, 8.dp).toString())
    }

    @Test
    fun blurStop_fractionAboveOne_throws() {
        assertThrowsWithMessage("fraction must be in 0..1 but was 1.2") {
            BlurStop(fraction = 1.2f, radius = 4.dp)
        }
    }

    @Test
    fun blurStop_fractionBelowZero_throws() {
        assertThrowsWithMessage("fraction must be in 0..1 but was -0.5") {
            BlurStop(fraction = -0.5f, radius = 4.dp)
        }
    }

    @Test
    fun blurStop_negativeRadius_throws() {
        assertThrowsWithMessage("radius must be >= 0 but was -1.0.dp") {
            BlurStop(fraction = 0.5f, radius = (-1).dp)
        }
    }

    @Test
    fun uniform_structuralEquality() {
        assertEqualWithConsistentHashCode(
            BlurRadiusSpec.uniform(8.dp),
            BlurRadiusSpec.uniform(8.dp),
        )
        assertNotEquals(BlurRadiusSpec.uniform(8.dp), BlurRadiusSpec.uniform(4.dp))
    }

    @Test
    fun verticalGradient_structuralEquality() {
        val base = BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp)
        assertEqualWithConsistentHashCode(
            base,
            BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp),
        )
        assertNotEquals(
            base,
            BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 10.dp),
        )
        assertNotEquals(
            base,
            BlurRadiusSpec.verticalGradient(startRadius = 5.dp, endRadius = 20.dp),
        )
    }

    @Test
    fun horizontalGradient_structuralEquality() {
        val base = BlurRadiusSpec.horizontalGradient(startRadius = 0.dp, endRadius = 20.dp)
        assertEqualWithConsistentHashCode(
            base,
            BlurRadiusSpec.horizontalGradient(startRadius = 0.dp, endRadius = 20.dp),
        )
        assertNotEquals(
            base,
            BlurRadiusSpec.horizontalGradient(startRadius = 0.dp, endRadius = 10.dp),
        )
        assertNotEquals(
            base,
            BlurRadiusSpec.horizontalGradient(startRadius = 5.dp, endRadius = 20.dp),
        )
    }

    @Test
    fun verticalGradientStops_structuralEquality() {
        assertEqualWithConsistentHashCode(
            BlurRadiusSpec.verticalGradient(stops(4)),
            BlurRadiusSpec.verticalGradient(stops(4)),
        )
        assertNotEquals(
            BlurRadiusSpec.verticalGradient(stops(3)),
            BlurRadiusSpec.verticalGradient(stops(5)),
        )
    }

    @Test
    fun horizontalGradientStops_structuralEquality() {
        assertEqualWithConsistentHashCode(
            BlurRadiusSpec.horizontalGradient(stops(4)),
            BlurRadiusSpec.horizontalGradient(stops(4)),
        )
        assertNotEquals(
            BlurRadiusSpec.horizontalGradient(stops(3)),
            BlurRadiusSpec.horizontalGradient(stops(5)),
        )
    }

    @Test
    fun linearGradient_structuralEquality() {
        fun build(
            start: DpOffset = DpOffset(0.dp, 0.dp),
            end: DpOffset = DpOffset(100.dp, 100.dp),
            startRadius: Dp = 0.dp,
            endRadius: Dp = 20.dp,
        ) = BlurRadiusSpec.linearGradient(start, end, startRadius, endRadius)

        assertEqualWithConsistentHashCode(build(), build())
        assertNotEquals(build(), build(start = DpOffset(1.dp, 0.dp)))
        assertNotEquals(build(), build(end = DpOffset(100.dp, 50.dp)))
        assertNotEquals(build(), build(startRadius = 2.dp))
        assertNotEquals(build(), build(endRadius = 10.dp))
    }

    @Test
    fun linearGradientStops_structuralEquality() {
        val start = DpOffset(0.dp, 0.dp)
        val end = DpOffset(100.dp, 100.dp)
        assertEqualWithConsistentHashCode(
            BlurRadiusSpec.linearGradient(start, end, stops(3)),
            BlurRadiusSpec.linearGradient(start, end, stops(3)),
        )
        assertNotEquals(
            BlurRadiusSpec.linearGradient(start, end, stops(3)),
            BlurRadiusSpec.linearGradient(start, end, stops(4)),
        )
        assertNotEquals(
            BlurRadiusSpec.linearGradient(start, end, stops(3)),
            BlurRadiusSpec.linearGradient(DpOffset(1.dp, 0.dp), end, stops(3)),
        )
    }

    @Test
    fun radialGradient_structuralEquality() {
        fun build(
            startRadius: Dp = 0.dp,
            endRadius: Dp = 20.dp,
            center: DpOffset = DpOffset(50.dp, 50.dp),
            fallOffRadius: Dp = 80.dp,
        ) = BlurRadiusSpec.radialGradient(startRadius, endRadius, center, fallOffRadius)

        assertEqualWithConsistentHashCode(build(), build())
        // The default center and infinite falloff values must compare structurally.
        assertEqualWithConsistentHashCode(
            BlurRadiusSpec.radialGradient(startRadius = 0.dp, endRadius = 20.dp),
            BlurRadiusSpec.radialGradient(startRadius = 0.dp, endRadius = 20.dp),
        )
        assertNotEquals(build(), build(startRadius = 2.dp))
        assertNotEquals(build(), build(endRadius = 10.dp))
        assertNotEquals(build(), build(center = DpOffset(0.dp, 0.dp)))
        assertNotEquals(build(), build(fallOffRadius = 40.dp))
    }

    @Test
    fun radialGradientStops_structuralEquality() {
        fun build(
            stopList: List<BlurStop> = stops(3),
            center: DpOffset = DpOffset(50.dp, 50.dp),
            fallOffRadius: Dp = 80.dp,
        ) = BlurRadiusSpec.radialGradient(stopList, center, fallOffRadius)

        assertEqualWithConsistentHashCode(build(), build())
        assertNotEquals(build(), build(stopList = stops(4)))
        assertNotEquals(build(), build(center = DpOffset(0.dp, 0.dp)))
        assertNotEquals(build(), build(fallOffRadius = 40.dp))
    }

    @Test
    fun sameShapeDifferentImplementations_areUnequal() {
        // Axis gradients with identical endpoints represent distinct types and must not compare
        // equal.
        val vertical = BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp)
        val horizontal = BlurRadiusSpec.horizontalGradient(startRadius = 0.dp, endRadius = 20.dp)
        assertNotEquals<BlurRadiusSpec>(vertical, horizontal)
        assertEquals(
            vertical,
            BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp),
        )
        assertNotEquals(
            vertical,
            BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 10.dp),
        )

        // A two-point gradient and its explicit 2-stop equivalent produce the same visual result
        // but remain distinct types.
        val twoStops =
            BlurRadiusSpec.verticalGradient(listOf(BlurStop(0f, 0.dp), BlurStop(1f, 20.dp)))
        assertNotEquals(vertical, twoStops)
    }

    @Test
    fun radiusShader_isOpaque_neverEqualAcrossInstances() {
        // Shader-based radii are opaque. Two instances built from the same block reference never
        // compare equal, ensuring value semantics match across platforms.
        val block: Density.(Size) -> Shader = { throw UnsupportedOperationException() }
        val a = BlurRadiusSpec.shader(24.dp, block)
        val b = BlurRadiusSpec.shader(24.dp, block)
        assertNotEquals(a, b)
        assertEquals(a, a)
    }

    @Test
    fun mask_negativeMaxRadius_throws() {
        assertThrowsWithMessage("maxRadius must be >= 0 but was -1.0.dp") {
            BlurRadiusSpec.shader((-1).dp) { throw UnsupportedOperationException() }
        }
    }

    @Test
    fun stopCountBelowTwo_throws() {
        assertThrowsWithMessage("expected between 2 and ${BlurStop.MaxStops} stops but was 1") {
            BlurRadiusSpec.verticalGradient(listOf(BlurStop(0f, 4.dp)))
        }
        assertThrowsWithMessage("expected between 2 and ${BlurStop.MaxStops} stops but was 0") {
            BlurRadiusSpec.horizontalGradient(emptyList())
        }
    }

    @Test
    fun stopCountAboveMax_throws() {
        // The mask shaders declare fixed-size uniform arrays, so we must reject excess stops at
        // construction time.
        val seventeen =
            List(BlurStop.MaxStops + 1) { BlurStop(it / BlurStop.MaxStops.toFloat(), (it * 2).dp) }
        assertThrowsWithMessage(
            "expected between 2 and ${BlurStop.MaxStops} stops but was ${BlurStop.MaxStops + 1}"
        ) {
            BlurRadiusSpec.verticalGradient(seventeen)
        }
    }

    @Test
    fun stopCountBounds_areInclusive() {
        // Every stops factory accepts between 2 and MaxStops inclusive.
        val two = stops(2)
        val sixteen =
            List(BlurStop.MaxStops) { BlurStop(it / (BlurStop.MaxStops - 1f), (it * 2).dp) }
        BlurRadiusSpec.verticalGradient(two)
        BlurRadiusSpec.verticalGradient(sixteen)
        BlurRadiusSpec.horizontalGradient(two)
        BlurRadiusSpec.linearGradient(DpOffset(0.dp, 0.dp), DpOffset(1.dp, 1.dp), two)
        BlurRadiusSpec.radialGradient(two)
    }

    @Test
    fun unsortedStops_areSortedByFraction() {
        // Construction sorts stops by fraction, meaning an unsorted list builds the exact same
        // value as its sorted equivalent.
        val unsorted =
            listOf(
                BlurStop(fraction = 0.6f, radius = 4.dp),
                BlurStop(fraction = 0.2f, radius = 8.dp),
                BlurStop(fraction = 1f, radius = 12.dp),
            )
        val sorted =
            listOf(
                BlurStop(fraction = 0.2f, radius = 8.dp),
                BlurStop(fraction = 0.6f, radius = 4.dp),
                BlurStop(fraction = 1f, radius = 12.dp),
            )
        assertEquals(
            BlurRadiusSpec.verticalGradient(sorted),
            BlurRadiusSpec.verticalGradient(unsorted),
        )
        assertEquals(
            BlurRadiusSpec.horizontalGradient(sorted),
            BlurRadiusSpec.horizontalGradient(unsorted),
        )
    }

    @Test
    fun equalFractions_keepTheirGivenRelativeOrderWhenSorted() {
        // The sorting is stable. Stops sharing a fraction encode a hard visual step depending on
        // their relative order, so the sort must preserve them.
        val withStep =
            listOf(
                BlurStop(fraction = 1f, radius = 20.dp),
                BlurStop(fraction = 0.5f, radius = 4.dp),
                BlurStop(fraction = 0.5f, radius = 12.dp),
                BlurStop(fraction = 0f, radius = 0.dp),
            )
        val expected =
            listOf(
                BlurStop(fraction = 0f, radius = 0.dp),
                BlurStop(fraction = 0.5f, radius = 4.dp),
                BlurStop(fraction = 0.5f, radius = 12.dp),
                BlurStop(fraction = 1f, radius = 20.dp),
            )
        assertEquals(
            BlurRadiusSpec.verticalGradient(expected),
            BlurRadiusSpec.verticalGradient(withStep),
        )
    }

    @Test
    fun equalAdjacentFractions_accepted() {
        // Equal adjacent fractions encode a hard visual step and are perfectly valid.
        val stops =
            listOf(
                BlurStop(fraction = 0f, radius = 0.dp),
                BlurStop(fraction = 0.5f, radius = 4.dp),
                BlurStop(fraction = 0.5f, radius = 12.dp),
                BlurStop(fraction = 1f, radius = 20.dp),
            )
        // Does not throw.
        BlurRadiusSpec.verticalGradient(stops)
    }

    @Test
    fun uniform_negativeRadius_throws() {
        assertThrowsWithMessage("radius must be >= 0 but was -1.0.dp") {
            BlurRadiusSpec.uniform((-1).dp)
        }
    }

    @Test
    fun axisGradients_negativeRadii_throw() {
        assertThrowsWithMessage("startRadius must be >= 0 but was -1.0.dp") {
            BlurRadiusSpec.verticalGradient(startRadius = (-1).dp, endRadius = 20.dp)
        }
        assertThrowsWithMessage("endRadius must be >= 0 but was -4.0.dp") {
            BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = (-4).dp)
        }
        assertThrowsWithMessage("startRadius must be >= 0 but was -1.0.dp") {
            BlurRadiusSpec.horizontalGradient(startRadius = (-1).dp, endRadius = 20.dp)
        }
        assertThrowsWithMessage("endRadius must be >= 0 but was -4.0.dp") {
            BlurRadiusSpec.horizontalGradient(startRadius = 0.dp, endRadius = (-4).dp)
        }
    }

    @Test
    fun linearGradient_negativeRadii_throw() {
        val start = DpOffset(0.dp, 0.dp)
        val end = DpOffset(100.dp, 100.dp)
        assertThrowsWithMessage("startRadius must be >= 0 but was -1.0.dp") {
            BlurRadiusSpec.linearGradient(start, end, startRadius = (-1).dp, endRadius = 20.dp)
        }
        assertThrowsWithMessage("endRadius must be >= 0 but was -4.0.dp") {
            BlurRadiusSpec.linearGradient(start, end, startRadius = 0.dp, endRadius = (-4).dp)
        }
    }

    @Test
    fun radialGradient_negativeRadii_throw() {
        assertThrowsWithMessage("startRadius must be >= 0 but was -1.0.dp") {
            BlurRadiusSpec.radialGradient(startRadius = (-1).dp, endRadius = 20.dp)
        }
        assertThrowsWithMessage("endRadius must be >= 0 but was -4.0.dp") {
            BlurRadiusSpec.radialGradient(startRadius = 0.dp, endRadius = (-4).dp)
        }
    }

    @Test
    fun radialGradient_nonPositiveFiniteFallOffRadius_throws() {
        assertThrowsWithMessage("fallOffRadius must be positive when finite but was 0.0.dp") {
            BlurRadiusSpec.radialGradient(
                startRadius = 0.dp,
                endRadius = 20.dp,
                fallOffRadius = 0.dp,
            )
        }
        assertThrowsWithMessage("fallOffRadius must be positive when finite but was -80.0.dp") {
            BlurRadiusSpec.radialGradient(
                startRadius = 0.dp,
                endRadius = 20.dp,
                fallOffRadius = (-80).dp,
            )
        }
        // Same rule on the multi-stop overload.
        assertThrowsWithMessage("fallOffRadius must be positive when finite but was 0.0.dp") {
            BlurRadiusSpec.radialGradient(stops(3), fallOffRadius = 0.dp)
        }
        assertThrowsWithMessage("fallOffRadius must be positive when finite but was -80.0.dp") {
            BlurRadiusSpec.radialGradient(stops(3), fallOffRadius = (-80).dp)
        }
    }

    @Test
    fun radialGradient_infiniteFallOffRadius_accepted() {
        // The infinite falloff radius serves as the auto-fit default and must be accepted.
        BlurRadiusSpec.radialGradient(
            startRadius = 0.dp,
            endRadius = 20.dp,
            fallOffRadius = Dp.Infinity,
        )
        BlurRadiusSpec.radialGradient(stops(3), fallOffRadius = Dp.Infinity)
    }

    @Test
    fun mutatingSourceListAfterConstruction_doesNotAffectResult() {
        val source =
            mutableListOf(
                BlurStop(fraction = 0f, radius = 0.dp),
                BlurStop(fraction = 1f, radius = 20.dp),
            )
        val snapshot = source.toList()
        val radius = BlurRadiusSpec.verticalGradient(source)

        // We mutate the caller's list after construction to verify the defensive copy.
        source.add(BlurStop(fraction = 0.5f, radius = 40.dp))
        source[0] = BlurStop(fraction = 0f, radius = 99.dp)

        // The radius correctly reflects the state at construction instead of the subsequent
        // mutation.
        assertEquals(BlurRadiusSpec.verticalGradient(snapshot), radius)
    }

    @Test
    fun resolveGradientCenter_defaultsToSurfaceCenter() {
        val radial = BlurRadialGradient(0.dp, 20.dp, DpOffset.Unspecified, Dp.Infinity)
        assertEquals(Offset(100f, 50f), radial.resolveBlurCenter(Size(200f, 100f), density))
        assertEquals(Offset(50f, 50f), radial.resolveBlurCenter(Size(100f, 100f), density))
    }

    @Test
    fun resolveGradientCenter_explicitCenterResolvesAgainstDensity() {
        // An explicit Dp center resolves to pixels using the density (2x here).
        val radial = BlurRadialGradient(0.dp, 20.dp, DpOffset(10.dp, 20.dp), Dp.Infinity)
        assertEquals(Offset(20f, 40f), radial.resolveBlurCenter(Size(200f, 100f), density))
    }

    @Test
    fun resolveGradientRadius_finiteResolvesAgainstDensity() {
        // A finite Dp falloff resolves to pixels using the density (2x here).
        val radial = BlurRadialGradient(0.dp, 20.dp, DpOffset.Unspecified, 80.dp)
        assertEquals(160f, radial.resolveFallOffRadius(Size(200f, 100f), density))
        // The radius resolves even when it exceeds the surface boundaries.
        assertEquals(160f, radial.resolveFallOffRadius(Size(20f, 20f), density))
    }

    @Test
    fun resolveGradientRadius_infiniteResolvesToHalfMinDimension() {
        val radial = BlurRadialGradient(0.dp, 20.dp, DpOffset.Unspecified, Dp.Infinity)
        // Square surface: min(100, 100) / 2 = 50.
        assertEquals(50f, radial.resolveFallOffRadius(Size(100f, 100f), density))
        // Wide surface: min(200, 100) / 2 = 50, driven by the height.
        assertEquals(50f, radial.resolveFallOffRadius(Size(200f, 100f), density))
        // Tall surface: min(80, 200) / 2 = 40, driven by the width.
        assertEquals(40f, radial.resolveFallOffRadius(Size(80f, 200f), density))
    }

    @Test
    fun radialStops_resolveDelegatorsMatchRadial() {
        // RadialStops shares the same resolution helpers as Radial.
        val defaulted = BlurRadialStops(stops(3), DpOffset.Unspecified, Dp.Infinity)
        assertEquals(Offset(100f, 50f), defaulted.resolveBlurCenter(Size(200f, 100f), density))
        assertEquals(50f, defaulted.resolveFallOffRadius(Size(200f, 100f), density))

        val explicit = BlurRadialStops(stops(3), DpOffset(10.dp, 20.dp), 80.dp)
        assertEquals(Offset(20f, 40f), explicit.resolveBlurCenter(Size(200f, 100f), density))
        assertEquals(160f, explicit.resolveFallOffRadius(Size(200f, 100f), density))
    }

    @Test
    fun structurallyEqualParameters_areEqual() {
        // The blur node relies on this equality to hit its cache. Two independently built values
        // over equal parameters must compare equal to reuse the platform effect.
        val a =
            BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp)
                .createRenderEffect(size, density)
        val b =
            BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp)
                .createRenderEffect(size, density)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun differingEdgeTreatment_breaksEquality() {
        val radius = BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp)
        val a = radius.createRenderEffect(size, density, TileMode.Clamp)
        val b = radius.createRenderEffect(size, density, TileMode.Decal)
        assertNotEquals(a, b)
    }

    @Test
    fun differingRadius_breaksEquality() {
        val a =
            BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp)
                .createRenderEffect(size, density)
        val b =
            BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 10.dp)
                .createRenderEffect(size, density)
        assertNotEquals(a, b)
    }

    @Test
    fun differingSize_breaksEquality() {
        val radius = BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp)
        val a = radius.createRenderEffect(Size(200f, 100f), density)
        val b = radius.createRenderEffect(Size(200f, 200f), density)
        assertNotEquals(a, b)
    }

    @Test
    fun differingDensity_breaksEquality() {
        val radius = BlurRadiusSpec.verticalGradient(startRadius = 0.dp, endRadius = 20.dp)
        val a = radius.createRenderEffect(size, Density(2f))
        val b = radius.createRenderEffect(size, Density(3f))
        assertNotEquals(a, b)
    }

    @Test
    fun sameShaderRadiusInstance_effectsAreNeverEqual() {
        // A shader-based radius wraps mutable user-owned shader state. Two effects wrapping the
        // identical mask instance must never compare equal, forcing the cache to miss.
        // Reflexivity still holds via identity.
        val maskRadius = BlurRadiusSpec.shader(24.dp) { throw UnsupportedOperationException() }
        val a = maskRadius.createRenderEffect(size, density)
        val b = maskRadius.createRenderEffect(size, density)
        assertNotEquals(a, b)
        assertEquals(a, a)
    }
}
