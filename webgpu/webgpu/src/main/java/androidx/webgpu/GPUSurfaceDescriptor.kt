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

/** A descriptor for creating a surface, potentially extended with platform-specific options. */
public class GPUSurfaceDescriptor
@JvmOverloads
constructor(
    /** The label for the surface. */
    public var label: String? = null,
    /** Extension for configuring surface color management options. */
    public var surfaceColorManagement: GPUSurfaceColorManagement? = null,
    /** Extension for creating a surface from an Android native window. */
    public var surfaceSourceAndroidNativeWindow: GPUSurfaceSourceAndroidNativeWindow? = null,
)
