/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.graphics.shapes

/**
 * Convert cubics to Features in a 1:1 mapping of [Cubic.asFeature] unless
 * - two subsequent cubics are not continuous, in which case an empty corner needs to be added in
 *   between. Example for C1, C2: /C1\/C2\ -> /C\C/C\.
 * - multiple subsequent cubics can be expressed as a single feature. Example for C1, C2:
 *   --C1----C2-- -> -----E----. One exception to the latter rule is for the first and last cubic,
 *   that remain the same in order to persist the start position. Assumes the list of cubics is
 *   continuous.
 */
internal fun detectFeatures(cubics: List<Cubic>): List<Feature> {
    if (cubics.isEmpty()) return emptyList()

    // TODO: b/372651969 Try different heuristics for corner grouping
    return buildList {
        var current = cubics.first()

        // Do one roundabout in which (current == last, next == first) is the last iteration.
        // Just like a snowball, subsequent cubics that align to one feature merge until
        // the streak breaks, the result is added, and a new streak starts.
        for (i in cubics.indices) {
            val next = cubics[(i + 1) % (cubics.size)]

            if (i < cubics.lastIndex && current.alignsIshWith(next)) {
                current = Cubic.extend(current, next)
                continue
            }

            add(current.asFeature(next))

            if (!current.smoothesIntoIsh(next)) {
                add(Cubic.empty(current.anchor1X, current.anchor1Y).asFeature(next))
            }

            current = next
        }
    }
}

/**
 * Convert to [Feature.Edge] if this cubic describes a straight line, otherwise to a
 * [Feature.Corner]. Corner convexity is determined by [convex].
 */
internal fun Cubic.asFeature(next: Cubic): Feature =
    if (straightIsh()) Feature.Edge(listOf(this)) else Feature.Corner(listOf(this), convexTo(next))

/** Determine if the cubic is close to a straight line. Empty cubics don't count as straightIsh. */
internal fun Cubic.straightIsh(): Boolean =
    !zeroLength() &&
        collinearIsh(
            anchor0X,
            anchor0Y,
            anchor1X,
            anchor1Y,
            control0X,
            control0Y,
            RelaxedDistanceEpsilon
        ) &&
        collinearIsh(
            anchor0X,
            anchor0Y,
            anchor1X,
            anchor1Y,
            control1X,
            control1Y,
            RelaxedDistanceEpsilon
        )

/**
 * Determines if next is a smooth continuation of this cubic. Smooth meaning that the first control
 * point of next is a reflection of this' second control point, similar to the S/s or t/T command in
 * svg paths https://developer.mozilla.org/en-US/docs/Web/SVG/Tutorial/Paths#b%C3%A9zier_curves
 */
internal fun Cubic.smoothesIntoIsh(next: Cubic): Boolean =
    collinearIsh(
        control1X,
        control1Y,
        next.control0X,
        next.control0Y,
        anchor1X,
        anchor1Y,
        RelaxedDistanceEpsilon
    )

/**
 * Determines if all of this' points align with next's points. For straight lines, this is the same
 * as if next was a continuation of this.
 */
internal fun Cubic.alignsIshWith(next: Cubic): Boolean =
    straightIsh() && next.straightIsh() && smoothesIntoIsh(next) ||
        zeroLength() ||
        next.zeroLength()

/** Create a new cubic by extending A to B's second anchor point */
private fun Cubic.Companion.extend(a: Cubic, b: Cubic): Cubic {
    return if (a.zeroLength())
        Cubic(
            a.anchor0X,
            a.anchor0Y,
            b.control0X,
            b.control0Y,
            b.control1X,
            b.control1Y,
            b.anchor1X,
            b.anchor1Y
        )
    else
        Cubic(
            a.anchor0X,
            a.anchor0Y,
            a.control0X,
            a.control0Y,
            a.control1X,
            a.control1Y,
            b.anchor1X,
            b.anchor1Y
        )
}
