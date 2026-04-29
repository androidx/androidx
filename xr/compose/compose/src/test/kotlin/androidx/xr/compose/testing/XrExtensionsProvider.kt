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

// TODO (b/502381626) - Use SceneCoreTestRule instead
@file:Suppress("DEPRECATION")

package androidx.xr.compose.testing

import androidx.xr.scenecore.runtime.XrExtensionsHolder
import androidx.xr.scenecore.testing.FakeXrExtensionsHolderProvider
import com.android.extensions.xr.XrExtensions

/**
 * Helper for singleton XrExtensions and set it for other test by
 * [androidx.xr.scenecore.runtime.extensions.XrExtensionsHolderAccessor]
 */
internal object XrExtensionsProvider {
    /** Prepare the test environment with XrExtensions. */
    @SuppressWarnings("RestrictedApiAndroidX")
    internal fun getXrExtensions(): XrExtensions =
        XrExtensions().also {
            FakeXrExtensionsHolderProvider.Companion.fakeHolderLegacy =
                XrExtensionsHolder(it, XrExtensions::class.java)
            FakeXrExtensionsHolderProvider.Companion.fakeHolder =
                XrExtensionsHolder(
                    it.underlyingObject,
                    android.extensions.xr.XrExtensions::class.java,
                )
        }
}
