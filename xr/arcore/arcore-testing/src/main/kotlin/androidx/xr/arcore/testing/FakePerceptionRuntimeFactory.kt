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

package androidx.xr.arcore.testing

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.arcore.testing.internal.FakePerceptionRuntimeFactory as InternalFactory
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.internal.PerceptionRuntimeFactory
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// TODO b/500091606 Remove when no longer used in G3
/**
 * Factory for creating a [FakePerceptionRuntime] for testing purposes.
 *
 * @deprecated This will be removed in a future release. In order to test androidx.xr.arcore APIs,
 *   use an [ArCoreTestRule] in your tests.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Deprecated(
    "arcore-testing fakes have been moved internal and should no longer be used by unit tests."
)
public class FakePerceptionRuntimeFactory() : PerceptionRuntimeFactory {
    public companion object {
        /** Will be passed to the [FakeLifecycleManager] constructor during testing. */
        @JvmStatic
        @get:JvmName("hasCreatePermission")
        public var hasCreatePermission: Boolean = true

        /**
         * Exception that will be thrown when [FakePerceptionRuntime.initialize] is called.
         *
         * Setting this value will cause the next call to [FakePerceptionRuntime.initialize] to
         * throw this exception. Setting this value to null will clear the exception and allow the
         * next call to succeed.
         */
        internal var createNewFakeRuntime: Boolean = false

        public var lifecycleCreateException: Exception?
            get() {
                return InternalFactory.runtimeInitializeException
            }
            set(value) {
                InternalFactory.runtimeInitializeException = value
            }
    }

    override val requirements: Set<Feature> = emptySet()

    // TODO b/438853896 - migrate all tests to use the coroutine context
    @Suppress("DEPRECATION")
    public fun createRuntime(context: Context): PerceptionRuntime =
        createRuntime(context, EmptyCoroutineContext)

    /**
     * Creates a [FakePerceptionRuntime] instance for testing purposes.
     *
     * @param context The host [Context].
     * @param coroutineContext The [CoroutineContext] for the runtime to use during testing.
     */
    @Suppress("DEPRECATION")
    override fun createRuntime(
        context: Context,
        coroutineContext: CoroutineContext,
    ): PerceptionRuntime =
        if (createNewFakeRuntime) {
            InternalFactory().createRuntime(context, coroutineContext)
        } else {
            FakePerceptionRuntime(FakePerceptionManager(), hasCreatePermission)
        }
}
