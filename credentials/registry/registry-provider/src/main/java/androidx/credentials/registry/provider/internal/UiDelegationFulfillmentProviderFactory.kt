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

package androidx.credentials.registry.provider.internal

import android.content.Intent
import android.util.Log
import androidx.credentials.registry.provider.UiDelegationFulfillmentProvider

internal class UiDelegationFulfillmentProviderFactory {
    fun getBestAvailableProvider(intent: Intent): UiDelegationFulfillmentProvider? {
        val className =
            intent.extras?.getString(UiDelegationFulfillmentProvider.EXTRA_STUB_IMPL_CLASS_NAME)
        if (className != null) {
            return instantiateProvider(className)
        }
        return null
    }

    private fun instantiateProvider(className: String): UiDelegationFulfillmentProvider? {
        try {
            val klass = Class.forName(className)
            return klass.getConstructor().newInstance() as UiDelegationFulfillmentProvider
        } catch (e: Throwable) {
            Log.e(TAG, "Exception thrown while instantiating provider class", e)
        }
        return null
    }

    companion object {
        private const val TAG = "UiDelegationFulfillmentProviderFactory"
    }
}
