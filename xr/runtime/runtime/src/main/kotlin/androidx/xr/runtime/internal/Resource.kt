/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo

/** Interface for a resource. A resource represents a loadable resource. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public interface Resource {}

/**
 * Interface for an EXR resource. These HDR images can be used for image based lighting and
 * skyboxes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public interface ExrImageResource : Resource {}

/** Interface for a glTF resource. This can be used for creating glTF entities. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public interface GltfModelResource : Resource {}

/** Interface for a texture resource. This can be used alongside materials. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public interface TextureResource : Resource {}

/** Interface for a material resource. This can be used to override materials on meshes. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public interface MaterialResource : Resource {}
