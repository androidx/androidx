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

/** Configuration for writing GPU timestamps during a compute or render pass. */
public class GPUPassTimestampWrites
@JvmOverloads
constructor(
    public var querySet: GPUQuerySet,
    public var beginningOfPassWriteIndex: Int = Constants.QUERY_SET_INDEX_UNDEFINED,
    public var endOfPassWriteIndex: Int = Constants.QUERY_SET_INDEX_UNDEFINED,
)
