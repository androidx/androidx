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

package androidx.xr.runtime.testing.internal

import android.content.Context
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.internal.SessionResultProvider
import androidx.xr.runtime.internal.SessionResultProviderFactory
import androidx.xr.runtime.testing.SessionTestRule
import kotlinx.coroutines.CoroutineScope

internal class FakeSessionResultProviderFactory : SessionResultProviderFactory {
    internal companion object {
        @JvmStatic internal var sessionTestRule: SessionTestRule? = null
    }

    override fun createProvider(
        context: Context,
        coroutineScope: CoroutineScope,
    ): SessionResultProvider {
        val provider = FakeSessionResultProvider()
        sessionTestRule?.registerWithProvider(provider)
        return provider
    }

    override val requirements: Set<Feature> = emptySet()
}
