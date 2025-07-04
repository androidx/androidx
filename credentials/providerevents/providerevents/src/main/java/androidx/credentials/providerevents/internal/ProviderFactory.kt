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

package androidx.credentials.providerevents.internal

import android.content.Intent
import android.util.Log

internal interface ProviderFactory {
    fun getBestAvailableProvider(intent: Intent, key: String): Any? {
        val className = intent.extras?.getString(key)
        if (className != null) {
            return instantiateClosedSourceProvider(className)
        }
        return null
    }

    private fun instantiateClosedSourceProvider(className: String): Any? {
        try {
            val klass = Class.forName(className)
            return klass.getConstructor().newInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "Exception thrown while instantiating provider class", e)
        }
        return null
    }

    companion object {
        private const val TAG = "ProviderFactory"
    }
}
