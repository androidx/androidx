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
import androidx.xr.runtime.internal.SystemSpaceEntity
import java.util.concurrent.Executor

/** A test double for [SystemSpaceEntity], designed for use in unit or integration tests. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FakeSystemSpaceEntity() : FakeEntity(), SystemSpaceEntity {
    private var onSpaceUpdatedListener: Runnable? = null
    private var onSpaceUpdatedExecutor: Executor? = null

    /**
     * Registers a listener to be called when the underlying space has moved or changed.
     *
     * @param listener The listener to register if non-null, else stops listening if null.
     * @param executor The executor to run the listener on. The listener will be called on the main
     *   thread if null.
     */
    @Suppress("ExecutorRegistration")
    override fun setOnSpaceUpdatedListener(listener: Runnable?, executor: Executor?) {
        onSpaceUpdatedListener = listener
        onSpaceUpdatedExecutor = executor
    }

    /**
     * Test function to invoke the onSpaceUpdated listener callback.
     *
     * This function is used to simulate the update of the underlying space, triggering the
     * registered listener. In tests, you can call this function to manually trigger the listener
     * and verify that your code responds correctly to space updates.
     */
    public fun onSpaceUpdated() {
        onSpaceUpdatedListener?.let { listener ->
            onSpaceUpdatedExecutor?.execute(listener) ?: listener.run()
        }
    }
}
