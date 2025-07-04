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

@file:JvmName("GuavaScenePose")

package androidx.xr.scenecore.guava

import androidx.xr.runtime.Session
import androidx.xr.runtime.guava.toFuture
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.HitTestResult
import androidx.xr.scenecore.ScenePose
import com.google.common.util.concurrent.ListenableFuture

/**
 * Creates a [HitTestResult] from the specified origin in the specified direction into the scene.
 *
 * @param session The session the [ScenePose] is in.
 * @param origin The translation of the origin of the hit test relative to this ScenePose.
 * @param direction The direction for the hit test ray from the origin.
 * @return a ListenableFuture<HitResult>. The HitResult describes if it hit something and where
 *   relative to this [ScenePose]. Listeners will be called on the main thread if Runnable::run is
 *   supplied.
 */
public fun ScenePose.hitTestAsync(
    session: Session,
    origin: Vector3,
    direction: Vector3,
): ListenableFuture<HitTestResult> =
    this.hitTestAsync(session, origin, direction, ScenePose.HitTestFilter.SELF_SCENE)

/**
 * Creates a [HitTestResult] from the specified origin in the specified direction into the scene.
 *
 * @param session The session the [ScenePose] is in.
 * @param origin The translation of the origin of the hit test relative to this ScenePose.
 * @param direction The direction for the hit test ray from the origin
 * @param hitTestFilter Filter for which scenes to hit test. Hitting other scenes is only allowed
 *   for apps with the `com.android.extensions.xr.ACCESS_XR_OVERLAY_SPACE` permission.
 * @return a ListenableFuture<HitResult>. The HitResult describes if it hit something and where
 *   relative to this [ScenePose]. Listeners will be called on the main thread if Runnable::run is
 *   supplied.
 */
public fun ScenePose.hitTestAsync(
    session: Session,
    origin: Vector3,
    direction: Vector3,
    @ScenePose.HitTestFilterValue hitTestFilter: Int,
): ListenableFuture<HitTestResult> = toFuture(session) { hitTest(origin, direction, hitTestFilter) }
