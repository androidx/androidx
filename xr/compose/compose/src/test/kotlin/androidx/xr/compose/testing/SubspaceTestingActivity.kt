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

package androidx.xr.compose.testing

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Custom test class that should be used for testing
 * [androidx.xr.compose.subspace.SubspaceComposable] content.
 */
class SubspaceTestingActivity : ComponentActivity() {
    private val _packageManager: PackageManager = mock<PackageManager>()

    init {
        whenever(_packageManager.hasSystemFeature(FEATURE_XR_API_SPATIAL)).thenReturn(true)
    }

    override fun getPackageManager() = _packageManager

    fun disableXr() {
        whenever(_packageManager.hasSystemFeature(FEATURE_XR_API_SPATIAL)).thenReturn(false)
    }
}
