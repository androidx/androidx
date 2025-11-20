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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.SubspaceNodeEntity

/** Test-only implementation of [androidx.xr.scenecore.runtime.SubspaceNodeEntity]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSubspaceNodeEntity(
    /**
     * The size of the [androidx.xr.scenecore.runtime.SubspaceNodeEntity] in meters, in unscaled
     * local space.
     */
    public override var size: Dimensions = Dimensions(2f, 1f, 0f)
) : SubspaceNodeEntity, FakeEntity()
