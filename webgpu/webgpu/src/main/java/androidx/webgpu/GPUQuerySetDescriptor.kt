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

/** A descriptor for creating a query set. */
public class GPUQuerySetDescriptor
@JvmOverloads
constructor(
    /** The type of queries in the set (occlusion or timestamp). */
    @QueryType public var type: Int,
    /** The total number of queries in the set. */
    public var count: Int,
    /** The label for the query set. */
    public var label: String? = null,
)
