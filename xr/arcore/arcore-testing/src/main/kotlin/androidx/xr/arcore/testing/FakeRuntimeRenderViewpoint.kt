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

// TODO(b/494286565) - Remove deprecation suppression when androidx.xr.runtime.FieldOfView is
// removed.
@file:Suppress("DEPRECATION")

package androidx.xr.arcore.testing

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.RenderViewpoint
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.math.Pose

/** Fake implementation of [RenderViewpoint] for testing purposes */
// TODO: b/326481788 - Add more functionality to FakeRuntimeArDevice
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
public class FakeRuntimeRenderViewpoint(
    override var pose: Pose = Pose(),
    @Deprecated(message = "Convert to androidx.xr.runtime.math.FieldOfView")
    override var fieldOfView: FieldOfView = FieldOfView(0f, 0f, 0f, 0f),
) : RenderViewpoint {}
