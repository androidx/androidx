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

package androidx.webgpu

import dalvik.annotation.optimization.FastNative

/** A set of GPU query objects used to record performance or visibility information. */
public class GPUQuerySet private constructor(public val handle: Long) : AutoCloseable {
    /** Immediately destroys the query set. */
    @FastNative @JvmName("destroy") public external fun destroy(): Unit

    /**
     * Gets the total number of queries in the set.
     *
     * @return The number of queries.
     */
    @FastNative @JvmName("getCount") public external fun getCount(): Int

    @get:JvmName("count")
    public val count: Int
        get() = getCount()

    /**
     * Gets the type of query this set records (e.g., occlusion, timestamp).
     *
     * @return The type of the queries in the set.
     */
    @FastNative @JvmName("getType") @QueryType public external fun getType(): Int

    @get:JvmName("type")
    public val type: Int
        get() = getType()

    /**
     * Sets a debug label for the query set.
     *
     * @param label The label to assign to the query set.
     */
    @FastNative @JvmName("setLabel") public external fun setLabel(label: String): Unit

    external override fun close()

    override fun equals(other: Any?): Boolean = other is GPUQuerySet && other.handle == handle

    override fun hashCode(): Int = handle.hashCode()
}
