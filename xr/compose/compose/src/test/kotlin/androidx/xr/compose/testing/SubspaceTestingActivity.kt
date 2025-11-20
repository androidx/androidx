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

import androidx.activity.ComponentActivity
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider
import com.android.extensions.xr.ShadowConfig

/**
 * Custom test class that should be used for testing
 * [androidx.xr.compose.subspace.SubspaceComposable] content.
 */
class SubspaceTestingActivity : ComponentActivity() {
    init {
        // TODO(b/447211302) Remove once direct dependency on XrExtensions in Compose XR is removed.
        ShadowConfig.extract(XrExtensionsProvider.getXrExtensions()!!.config!!)
            .setDefaultDpPerMeter(1000f)
    }
}
