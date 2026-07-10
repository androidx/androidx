/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.spec

/**
 * Identifies a "semantic state" of a [MotionValue].
 *
 * Semantic states can be supplied by a [MotionSpec], and allows expose semantic information on the
 * logical state a [MotionValue] is in.
 */
class SemanticKey<T>(val debugLabel: String, val identity: Any = Any()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SemanticKey<*>) return false

        return identity == other.identity
    }

    override fun hashCode(): Int {
        return identity.hashCode()
    }

    override fun toString(): String {
        return "Semantics($debugLabel)"
    }
}

/** Creates a new semantic key of type [T], identified by [identity]. */
inline fun <reified T> SemanticKey(identity: Any = Any()) =
    SemanticKey<T>(T::class.simpleName.orEmpty(), identity)

/** Pair of semantic [key] and [value]. */
data class SemanticValue<T>(val key: SemanticKey<T>, val value: T)

/**
 * Creates a [SemanticValue] tuple from [SemanticKey] `this` with [value].
 *
 * This can be useful for creating [SemanticValue] literals with less noise.
 */
infix fun <T> SemanticKey<T>.with(value: T) = SemanticValue(this, value)

/**
 * Defines semantics values for [key], one per segment.
 *
 * This [values] are required to align with the segments of the [DirectionalMotionSpec] the instance
 * will be passed to. The class has no particular value outside of a [DirectionalMotionSpec].
 */
class SegmentSemanticValues<T>(val key: SemanticKey<T>, val values: List<T>) {

    /** Retrieves the [SemanticValue] at [segmentIndex]. */
    operator fun get(segmentIndex: Int): SemanticValue<T> {
        return SemanticValue(key, values[segmentIndex])
    }

    override fun toString() = "Semantics($key): [$values]"
}
