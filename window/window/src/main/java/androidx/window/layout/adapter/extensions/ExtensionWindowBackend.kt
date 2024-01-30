/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.layout.adapter.extensions

import android.app.Activity
import androidx.annotation.UiContext
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExtensionsUtil
import androidx.window.extensions.layout.WindowLayoutComponent
import androidx.window.layout.adapter.WindowBackend

/**
 * A wrapper around [WindowLayoutComponent] that ensures
 * [WindowLayoutComponent.addWindowLayoutInfoListener] is called at most once per context while
 * there are active listeners. Context has to be an [Activity] or a [UiContext] created with
 * [Context#createWindowContext] or InputMethodService.
 */
internal class ExtensionWindowBackend(
    private val backend: WindowBackend
) : WindowBackend by backend {

    companion object {

        /**
         * Returns a new instance that is made to handle the vendor API level on the device. There
         * should be a single instance per app and this is handled in
         * [androidx.window.layout.WindowInfoTracker.getOrCreate].
         */
        fun newInstance(
            component: WindowLayoutComponent,
            adapter: ConsumerAdapter
        ): WindowBackend {
            val safeVendorApiLevel = ExtensionsUtil.safeVendorApiLevel
            return when {
                safeVendorApiLevel >= 2 -> ExtensionWindowBackendApi2(component)
                safeVendorApiLevel == 1 -> ExtensionWindowBackendApi1(component, adapter)
                else -> ExtensionWindowBackendApi0()
            }
        }
    }
}
