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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.SpatialEnvironment
import java.util.concurrent.Executor
import java.util.function.Consumer

// TODO: b/405218432 - Implement this correctly instead of stubbing it out.
/** Test-only implementation of [SpatialEnvironment] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSpatialEnvironment : SpatialEnvironment {

    override val currentPassthroughOpacity: Float = 0.0f

    override var preferredSpatialEnvironment: SpatialEnvironment.SpatialEnvironmentPreference? =
        null

    override var preferredPassthroughOpacity: Float =
        SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE

    override fun addOnPassthroughOpacityChangedListener(
        executor: Executor,
        listener: Consumer<Float>,
    ) {}

    override fun removeOnPassthroughOpacityChangedListener(listener: Consumer<Float>) {}

    override val isPreferredSpatialEnvironmentActive: Boolean = false

    override fun addOnSpatialEnvironmentChangedListener(
        executor: Executor,
        listener: Consumer<Boolean>,
    ) {}

    override fun removeOnSpatialEnvironmentChangedListener(listener: Consumer<Boolean>) {}
}
