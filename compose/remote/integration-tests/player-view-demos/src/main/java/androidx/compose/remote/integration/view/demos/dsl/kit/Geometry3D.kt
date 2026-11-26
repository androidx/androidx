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

package androidx.compose.remote.integration.view.demos.dsl.kit

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Author-time geometry helpers: 3D path generators (xyz-triple `FloatArray`s, ready for
 * `primitiveTube` / `primitiveSweep`) and an orientation helper. Pure Kotlin, run once at build
 * time.
 */
public object Geometry3D {

    /**
     * A circle of [radius] in the XZ plane at height [y], as `x0,y0,z0, …` ([samples]+1 points).
     */
    public fun circlePath(radius: Float, samples: Int, y: Float = 0f): FloatArray {
        val out = FloatArray((samples + 1) * 3)
        for (i in 0..samples) {
            val a = (2.0 * Math.PI * i / samples).toFloat()
            out[i * 3] = radius * cos(a)
            out[i * 3 + 1] = y
            out[i * 3 + 2] = radius * sin(a)
        }
        return out
    }

    /** A helix about +Y: [coilRadius], rising [pitch] per turn for [turns] turns. */
    public fun helixPath(coilRadius: Float, pitch: Float, turns: Float, samples: Int): FloatArray {
        val out = FloatArray((samples + 1) * 3)
        val total = (2.0 * Math.PI * turns)
        for (i in 0..samples) {
            val a = (total * i / samples)
            out[i * 3] = (coilRadius * cos(a)).toFloat()
            out[i * 3 + 1] = (pitch * turns * (i.toDouble() / samples - 0.5)).toFloat()
            out[i * 3 + 2] = (coilRadius * sin(a)).toFloat()
        }
        return out
    }

    /**
     * A `(p, q)` **torus knot** path (closed), scaled by [scale]. Feed to `primitiveTube` for a
     * glossy knot. `(2,3)` is a trefoil; `(3,2)`, `(2,5)`, `(3,4)` are other nice knots.
     */
    public fun torusKnotPath(p: Int, q: Int, scale: Float, samples: Int): FloatArray {
        val out = FloatArray((samples + 1) * 3)
        for (i in 0..samples) {
            val t = (2.0 * Math.PI * i / samples)
            val r = cos(q * t) + 2.0
            out[i * 3] = (scale * r * cos(p * t)).toFloat()
            out[i * 3 + 1] = (scale * r * sin(p * t)).toFloat()
            out[i * 3 + 2] = (scale * -sin(q * t)).toFloat()
        }
        return out
    }

    /** A logarithmic-spiral arm in the XZ plane (galaxy arms). */
    public fun spiralXZ(
        turns: Float,
        maxRadius: Float,
        samples: Int,
        b: Float = 0.30f,
    ): FloatArray {
        val out = FloatArray((samples + 1) * 3)
        val maxA = 2.0 * Math.PI * turns
        for (i in 0..samples) {
            val a = maxA * i / samples
            val r = maxRadius * Math.exp(b * a).toFloat() / Math.exp(b * maxA).toFloat()
            out[i * 3] = (r * cos(a)).toFloat()
            out[i * 3 + 1] = 0f
            out[i * 3 + 2] = (r * sin(a)).toFloat()
        }
        return out
    }

    /**
     * Axis-angle that rotates the **+Y axis** onto the unit direction ([dx],[dy],[dz]). Returns
     * `[angleRad, ax, ay, az]` — pass straight to `rotate(angle, ax, ay, az)` to aim a +Y-built
     * mesh (cylinder, cone, capsule) down an arbitrary direction.
     */
    public fun aimYTo(dx: Float, dy: Float, dz: Float): FloatArray {
        val dot = dy.coerceIn(-1f, 1f)
        val angle = acos(dot.toDouble()).toFloat()
        // axis = normalize( cross((0,1,0), dir) ) = normalize( (dz, 0, -dx) )
        var ax = dz
        var az = -dx
        val l = sqrt(ax * ax + az * az)
        return if (l < 1e-4f) {
            floatArrayOf(angle, 1f, 0f, 0f) // parallel to Y: any perpendicular axis
        } else {
            floatArrayOf(angle, ax / l, 0f, az / l)
        }
    }
}
