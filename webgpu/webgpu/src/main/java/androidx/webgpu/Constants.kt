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

public object Constants {
    /** -1 to max int is resolved at compile time */
    private const val UINT32_MAX: Int = -1

    /** -1L to max long is resolved at compile time */
    private const val UINT64_MAX: Long = -1L
    private const val SIZE_MAX = UINT64_MAX

    /**
     * A constant value indicating that the texture array layer count is undefined and should use
     * the default value.
     */
    public const val ARRAY_LAYER_COUNT_UNDEFINED: Int = UINT32_MAX

    /**
     * A constant value indicating that the copy stride (bytesPerRow) is undefined and should use
     * the default value.
     */
    public const val COPY_STRIDE_UNDEFINED: Int = UINT32_MAX

    /** A constant value representing an undefined depth clear value. */
    public const val DEPTH_CLEAR_VALUE_UNDEFINED: Float = Float.NaN

    /** A constant value indicating that the depth slice index is undefined. */
    public const val DEPTH_SLICE_UNDEFINED: Int = UINT32_MAX

    /** A constant value indicating an undefined 32-bit integer limit. */
    public const val LIMIT_U32_UNDEFINED: Int = UINT32_MAX

    /** A constant value indicating an undefined 64-bit integer limit. */
    public const val LIMIT_U64_UNDEFINED: Long = UINT64_MAX

    /**
     * A constant value indicating that the texture mip level count is undefined and should use the
     * default value.
     */
    public const val MIP_LEVEL_COUNT_UNDEFINED: Int = UINT32_MAX

    /** A constant value indicating an undefined query set index. */
    public const val QUERY_SET_INDEX_UNDEFINED: Int = UINT32_MAX

    /**
     * A constant value for string length when the string is {@code null}-terminated or its length
     * is determined otherwise.
     */
    public const val STRLEN: Long = SIZE_MAX

    /** A constant value representing the whole size of a mappable buffer range. */
    public const val WHOLE_MAP_SIZE: Long = SIZE_MAX

    /** A constant value representing the whole size of a resource (e.g., buffer). */
    public const val WHOLE_SIZE: Long = UINT64_MAX
}
