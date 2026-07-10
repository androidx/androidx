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

package androidx.xr.scenecore.testing.internal

import android.app.Activity
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.internal.SceneRuntimeFactory
import androidx.xr.scenecore.testing.FakeScheduledExecutorService

/** Factory for creating test-only instances of [androidx.xr.scenecore.runtime.SceneRuntime]. */
internal class FakeSceneRuntimeFactory : SceneRuntimeFactory {
    override val requirements: Set<Feature> = emptySet()

    override fun create(activity: Activity): FakeSceneRuntime =
        FakeSceneRuntime(FakeScheduledExecutorService())

    override fun create(
        activity: Activity,
        unscaledGravityAlignedActivitySpace: Boolean,
    ): FakeSceneRuntime = create(activity)
}
