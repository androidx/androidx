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

package androidx.xr.arcore

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.Session
import java.util.Collections
import java.util.WeakHashMap

/** Get the Lifecycle associated with the [Activity] attached to the [Session]. */
private val Activity.lifecycle: Lifecycle
    get() = (this as LifecycleOwner).lifecycle
private val perceptionRuntimeCache =
    Collections.synchronizedMap(WeakHashMap<Session, PerceptionRuntime>())

internal val Session.perceptionRuntime: PerceptionRuntime
    get() = checkAndGetPerceptionRuntime(this)

private fun checkAndGetPerceptionRuntime(session: Session): PerceptionRuntime {
    check(session.activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
        "Session has been destroyed."
    }
    return perceptionRuntimeCache.getOrPut(session) {
        // This lambda is executed only once per session instance.
        session.runtimes.filterIsInstance<PerceptionRuntime>().single()
    }
}
