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
@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.integration.view.demos.dsl.kit

import androidx.compose.remote.creation.dsl.RcFloat

/**
 * Small reusable building blocks for **reactive** ([RcFloat]) math used across the 3D demos. These
 * are written with `RcFloat` member ops only (e.g. `x.sin()`, `a.min(b)`) so they compose cleanly
 * inside `scene3D { }`, where the top-level `sin`/`min`/`max` functions are shadowed by scope
 * members.
 *
 * They're also a teaching reference: the player re-evaluates these expressions every frame, so any
 * value built from `continuousSeconds()` animates on-device with no document re-upload.
 */

/** Clamp to `[0, 1]` using only member ops. `max(m, 0)` is written `m - m.min(0)`. */
public fun RcFloat.clamp01(): RcFloat {
    val m = this.min(1f)
    return m - m.min(0f)
}

/** Clamp to `[lo, hi]`. */
public fun RcFloat.clampTo(lo: Float, hi: Float): RcFloat {
    val m = this.min(hi)
    val low = m * -1f + lo // lo - m
    return m + (low - low.min(0f)) // m + max(lo - m, 0)
}

/** A 0..1 oscillation: treats `this` as an angle in radians and maps `sin` into `[0, 1]`. */
public fun RcFloat.osc01(): RcFloat = this.sin() * 0.5f + 0.5f

/** Linear remap from `[inMin, inMax]` to `[outMin, outMax]` (no clamping). */
public fun RcFloat.remap(inMin: Float, inMax: Float, outMin: Float, outMax: Float): RcFloat =
    (this - inMin) * ((outMax - outMin) / (inMax - inMin)) + outMin

/**
 * A smooth bob in `[0, 1]` driven by time [t] (seconds): `sin(t·speed + phase)` mapped to `[0,1]`.
 * Handy for staggered grids (vary [phase] per element).
 */
public fun bob01(t: RcFloat, speed: Float, phase: Float = 0f): RcFloat = (t * speed + phase).osc01()
