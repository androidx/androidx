/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.graphics

/**
 * Represents an object which may be able to be linearly interpolated with another object. Usually
 * used during animation.
 */
interface Interpolatable {
    // TODO: The API shape of this interface is likely to create an allocation per call. We may
    //  want to think about alternative API shapes which may allow for a "cached mutable object" or
    //  something which could avoid the allocations. In practice however, this is difficult, as most
    //  of the types likely to implement this interface are thought of as immutable and having
    //  mutable versions of them is likely to be error prone.
    /**
     * This method is defined such that a value of [t] being 1.0 means that the interpolation has
     * finished, meaning that this method should return either [other] (or something equivalent to
     * [other]), 0.0 meaning that the interpolation has not started, returning `this` (or something
     * equivalent to `this`), and values in between meaning that the interpolation is at the
     * relevant point on the timeline between `this` and [other]. The interpolation can be
     * extrapolated beyond 0.0 and 1.0, so negative values and values greater than 1.0 are valid.
     *
     * Note that the type of [other] may not be the same as the type of `this`, however if the
     * implementation knows how to losslessly convert from one type to another, the conversion can
     * be made in order to optimistically provide a valid interpolation.
     *
     * If [other] is `null`, this can be interpreted as interpolating between "nothing" or, "no
     * effect", into whatever the value of `this` is. In this case, if it is possible to construct a
     * version of this type that is visually or semantically equivalent to "no effect", then it
     * might make sense to return an interpolation between that value and `this`.
     *
     * If there is no known way to interpolate between the two values, the implementation should
     * return null.
     *
     * @param other The other object to be intorpolated with this one.
     * @param t The position on the timeline. This is usually between 0 and 1, but it is valid for
     *   it to be outside of this range.
     * @return The interpolated object.
     * @see Interpolatable#lerp
     */
    fun lerp(other: Any?, t: Float): Any?

    companion object {
        /**
         * Attempt to Linearly interpolates between two values. If either of the values are
         * [Interpolatable], this will attempt to use their `lerp` functions. If the interpolation
         * is not possible, this will return [a] if [t] is less than 0.5 and [b] otherwise.
         *
         * If [a] implements [Interpolatable] it will be attempted first.
         *
         * @param a The start value.
         * @param b The end value.
         * @param t The fraction to interpolate between the two values. This is usually between 0
         *   and 1, but it is valid for it to be outside of this range.
         * @return The interpolated value.
         */
        fun lerp(a: Any?, b: Any?, t: Float): Any? {
            if (a == b) return if (t < 0.5f) a else b
            var result: Any? = null
            if (a is Interpolatable) {
                result = a.lerp(b, t)
            }
            if (result == null && b is Interpolatable) {
                result = b.lerp(a, 1 - t)
            }
            return result ?: if (t < 0.5f) a else b
        }
    }
}
