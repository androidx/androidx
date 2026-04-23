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

package androidx.xr.arcore.testing.internal

import android.content.Context
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.internal.PerceptionRuntimeFactory
import kotlin.coroutines.CoroutineContext

internal class FakePerceptionRuntimeFactory() : PerceptionRuntimeFactory {
    companion object {
        @JvmStatic var arCoreTestRule: ArCoreTestRule? = null
    }

    override val requirements: Set<Feature> = emptySet()

    override fun createRuntime(
        context: Context,
        coroutineContext: CoroutineContext,
    ): FakePerceptionRuntime {
        val runtime = FakePerceptionRuntime(FakePerceptionManager())
        arCoreTestRule?.registerWithRuntime(runtime)
        return runtime
    }
}
