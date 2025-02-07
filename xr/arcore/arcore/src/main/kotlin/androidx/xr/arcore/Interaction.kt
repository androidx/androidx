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

@file:JvmName("Interaction")

package androidx.xr.arcore

import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Ray

/**
 * Performs a hit-test using the given [ray].
 *
 * A hit-test is a method of calculating the intersection of a ray with objects tracked by the
 * session. Conducting a hit-test results in a list of hit objects, in other words, a hit-test does
 * not stop at the first object hit.
 *
 * @return A list of [HitResult] objects, sorted by distance from the origin of the ray. The nearest
 *   hit is at the beginning of the list.
 */
public fun hitTest(session: Session, ray: Ray): List<HitResult> {
    val perceptionStateExtender =
        session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
    val perceptionManager = perceptionStateExtender.perceptionManager
    val trackableMap = perceptionStateExtender.xrResourcesManager.trackablesMap
    return perceptionManager.hitTest(ray).map {
        val trackable =
            requireNotNull(trackableMap[it.trackable]) {
                "No Active Trackable found for the given hit result."
            }
        HitResult(it.distance, it.hitPose, trackable)
    }
}
