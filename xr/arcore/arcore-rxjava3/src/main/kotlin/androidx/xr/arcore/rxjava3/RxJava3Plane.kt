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

@file:JvmName("RxJava3Plane")

package androidx.xr.arcore.rxjava3

import androidx.xr.arcore.Plane
import androidx.xr.runtime.Session
import io.reactivex.rxjava3.core.Flowable
import kotlinx.coroutines.rx3.asFlowable

/** The current state of the [Plane]. */
public val Plane.stateAsFlowable: Flowable<Plane.State>
    get() = state.asFlowable()

/**
 * Emits the planes that are currently being tracked in the [session].
 *
 * Only [Plane]s that are [TrackingState.TRACKING] will be emitted in the [Collection]. Instances of
 * the same [Plane] will remain between subsequent emits to the [StateFlow] as long as they remain
 * tracking.
 *
 * @param session The active ARCore [Session] from which to track plane updates.
 * @return a Flowable<Collection<Plane>>. That emits collections of [Plane] objects representing
 *   currently tracked planes.
 * @throws [IllegalStateException] if [Session.config] is set to [Config.PlaneTrackingMode.DISABLED]
 */
public fun subscribeAsFlowable(session: Session): Flowable<Collection<Plane>> =
    Plane.subscribe(session).asFlowable()
