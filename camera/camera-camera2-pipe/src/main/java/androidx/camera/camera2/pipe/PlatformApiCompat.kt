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

package androidx.camera.camera2.pipe

import androidx.annotation.RestrictTo

/**
 * PlatformApiCompat is an interface intended for experimental feature integration where the
 * implementation will be done outside CameraPipe's source tree.
 *
 * All methods must provide a default implementation. This is needed for source compatibility.
 * Additionally, all methods must specify and be backed by a flag which can be used to determine
 * whether said methods are available.
 *
 * Before graduating any of these methods, all usages outside CameraPipe must be properly removed to
 * ensure continuous source compatibility.
 *
 * This interface is integrated and configured as part of [CameraPipe.Config].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public interface PlatformApiCompat
