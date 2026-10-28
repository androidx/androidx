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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.draw

import android.util.Log
import androidx.compose.remote.core.Operation
import java.util.Collections

/** What the embedded player does with a paint operation its draw dispatcher has no branch for. */
internal enum class UnknownOperationPolicy {
    /** Log a warning once per operation class and skip the operation. */
    Log,

    /** Throw, so a missing branch fails loudly. Used by tests. */
    Fail,
}

/**
 * Applies [policy] to paint operations the embedded draw dispatcher does not handle. The dispatcher
 * decides which operations are deliberately ignored; anything reaching here is a gap relative to
 * the View player.
 */
internal object UnknownOperations {
    private const val TAG = "RcPlayer"

    @Volatile internal var policy: UnknownOperationPolicy = UnknownOperationPolicy.Log

    private val logged: MutableSet<Class<out Operation>> = Collections.synchronizedSet(HashSet())

    fun onUnknown(op: Operation) {
        when (policy) {
            UnknownOperationPolicy.Fail ->
                throw IllegalStateException("Embedded RcPlayer cannot execute ${op.javaClass.name}")
            UnknownOperationPolicy.Log ->
                if (logged.add(op.javaClass)) {
                    Log.w(TAG, "Embedded RcPlayer cannot execute ${op.javaClass.simpleName}")
                }
        }
    }
}
