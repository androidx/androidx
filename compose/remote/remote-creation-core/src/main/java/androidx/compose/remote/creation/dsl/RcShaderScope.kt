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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RemoteComposeShader

/**
 * Typed Kotlin DSL scope wrapping the legacy Java [RemoteComposeShader] uniform builder.
 *
 * Each `uniform(...)` overload picks the right wire encoding based on the value type — `Int` →
 * `setIntUniform`, `Float` / `FloatArray` → `setFloatUniform`, `RcImage` → `setBitmapUniform`,
 * `RcPoint` → 2-element float vector. No reflection, no `Any`. The shader's underlying type system
 * is still string/index-based, but the caller side is now type-checked at compile time.
 *
 * ```
 * createShader(myAgsl) {
 *     uniform("uTime", 0.5f)
 *     uniform("uResolution", 480f, 800f)
 *     uniform("uTexture", myImage)
 * }
 * ```
 *
 * Use [raw] for any uniform setter not yet wrapped.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RcDslMarker
public interface RcShaderScope {

    /** Underlying mutable [RemoteComposeShader] for un-wrapped uniform shapes. */
    public val raw: RemoteComposeShader

    // ---- Float uniforms ----

    /** Sets a scalar `float` uniform. */
    public fun uniform(name: String, value: Float)

    /** Sets a `vec2` uniform from two floats. */
    public fun uniform(name: String, x: Float, y: Float)

    /** Sets a `vec3` uniform from three floats. */
    public fun uniform(name: String, x: Float, y: Float, z: Float)

    /** Sets a `vec4` uniform from four floats. */
    public fun uniform(name: String, x: Float, y: Float, z: Float, w: Float)

    /** Sets a `vecN` uniform from a float array. */
    public fun uniform(name: String, values: FloatArray)

    /** Sets a `vec2` uniform from a typed [RcPoint] (literal coordinates only). */
    public fun uniform(name: String, point: RcPoint)

    // ---- Int uniforms ----

    /** Sets a scalar `int` uniform. */
    public fun uniform(name: String, value: Int)

    /** Sets an `ivec2` uniform from two ints. */
    public fun uniform(name: String, x: Int, y: Int)

    /** Sets an `ivec3` uniform from three ints. */
    public fun uniform(name: String, x: Int, y: Int, z: Int)

    /** Sets an `ivec4` uniform from four ints. */
    public fun uniform(name: String, x: Int, y: Int, z: Int, w: Int)

    /** Sets a `bool` uniform — wire-encoded as int (0 or 1). */
    public fun uniform(name: String, value: Boolean): Unit = uniform(name, if (value) 1 else 0)

    // ---- Bitmap uniforms ----

    /** Sets a sampler-backing bitmap uniform from a typed [RcImage] reference. */
    public fun uniform(name: String, image: RcImage)
}

/** Internal implementation that forwards every method to a wrapped legacy [RemoteComposeShader]. */
internal class RcShaderScopeImpl(override val raw: RemoteComposeShader) : RcShaderScope {

    override fun uniform(name: String, value: Float) {
        raw.setFloatUniform(name, value)
    }

    override fun uniform(name: String, x: Float, y: Float) {
        raw.setFloatUniform(name, x, y)
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float) {
        raw.setFloatUniform(name, x, y, z)
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float, w: Float) {
        raw.setFloatUniform(name, x, y, z, w)
    }

    override fun uniform(name: String, values: FloatArray) {
        raw.setFloatUniform(name, values)
    }

    override fun uniform(name: String, point: RcPoint) {
        // RcPoint coordinates are RcFloat — extract via .toFloat() for the literal case
        // (will return NaN if the coordinate is a writer-less expression, which is the
        // documented limitation: shader uniforms are pre-evaluated, not animated).
        raw.setFloatUniform(name, point.x.toFloat(), point.y.toFloat())
    }

    override fun uniform(name: String, value: Int) {
        raw.setIntUniform(name, value)
    }

    override fun uniform(name: String, x: Int, y: Int) {
        raw.setIntUniform(name, x, y)
    }

    override fun uniform(name: String, x: Int, y: Int, z: Int) {
        raw.setIntUniform(name, x, y, z)
    }

    override fun uniform(name: String, x: Int, y: Int, z: Int, w: Int) {
        raw.setIntUniform(name, x, y, z, w)
    }

    override fun uniform(name: String, image: RcImage) {
        raw.setBitmapUniform(name, image.id)
    }
}
