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

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Feature
import androidx.xr.runtime.internal.RuntimeFactory

/** Factory for creating test-only instances of [Runtime]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeRuntimeFactory() : RuntimeFactory {
    public companion object {
        /** Will be passed to the [FakeLifecycleManager] constructor during testing */
        @JvmStatic
        @get:JvmName("hasCreatePermission")
        public var hasCreatePermission: Boolean = true
        /**
         * Exception that will be thrown when [FakeLifecycleManager.create] is called. Setting this
         * value will cause the next call to [FakeLifecycleManager.create] to throw this exception.
         * Setting this value to null will clear the exception and allow the next call to succeed.
         */
        public var lifecycleCreateException: Exception? = null
    }

    override val requirements: Set<Feature> = emptySet()

    override fun createRuntime(activity: Activity): FakeRuntime =
        FakeRuntime(FakeLifecycleManager(hasCreatePermission), FakePerceptionManager())
}
