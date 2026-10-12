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

package androidx.camera.effects.playservices.lowlight

import android.graphics.SurfaceTexture
import android.os.DeadObjectException
import android.os.Parcel
import android.os.RemoteException
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The [Surface] that receives boosted frames from a Low Light Boost session, wrapping the
 * intermediate OpenGL [SurfaceTexture].
 *
 * A session's target surface must never be sent to Google Play Services again after the session
 * ends. If Google Play Services restarts, its client can try to register an ended session again
 * when it reconnects. A target surface sent that way makes the service fail and tear down the
 * newer, healthy session a few seconds later, which leaves the preview black (b/448524973).
 *
 * Therefore, [writeToParcel] throws [DeadObjectException] (a [RemoteException]) once the surface is
 * terminated, is no longer valid (e.g. released), or has already been parceled. The client treats
 * it as a failed IPC, so the stale registration never reaches the service.
 */
internal class SessionTargetSurface(surfaceTexture: SurfaceTexture) : Surface(surfaceTexture) {
    private val parceled = AtomicBoolean(false)
    private val consumedForSession = AtomicBoolean(false)
    private val terminated = AtomicBoolean(false)

    private val hasBeenParceled: Boolean
        get() = parceled.get()

    private val isConsumedForSession: Boolean
        get() = consumedForSession.get()

    internal val isTerminated: Boolean
        get() = terminated.get()

    /** Whether this surface is valid and hasn't been used to create a session yet. */
    internal val isUnusedForSession: Boolean
        get() = isValid && !isTerminated && !hasBeenParceled && !isConsumedForSession

    internal fun markConsumedForCreateSession(): Boolean {
        return consumedForSession.compareAndSet(false, true)
    }

    internal fun markTerminated() {
        terminated.set(true)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        // This surface is only parceled for Low Light Boost session creation, never into saved
        // state. Releasing the surface isn't enough, because Surface.writeToParcel() serializes a
        // released Surface without an error. Throwing a RemoteException makes the client abort
        // the IPC before it is sent.
        if (terminated.get() || !isValid) {
            throw DeadObjectException()
        }
        if (!parceled.compareAndSet(false, true)) {
            throw DeadObjectException()
        }
        super.writeToParcel(dest, flags)
    }
}
