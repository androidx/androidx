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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.credentials.providerevents.ProviderEventsApiProvider

internal interface ProviderFactory {
    fun getBestAvailableProvider(intent: Intent, key: String): Any? {
        val className = intent.extras?.getString(key)
        if (className != null) {
            return instantiateClosedSourceProvider(className)
        }
        return null
    }

    fun getBestAvailableProvider(context: Context, key: String): ProviderEventsApiProvider? {
        val classNames = getAllowedProvidersFromManifest(context, key)
        return if (classNames.isEmpty()) {
            null
        } else {
            instantiateClosedSourceProvider(classNames, context)
        }
    }

    @Suppress("deprecation")
    private fun getAllowedProvidersFromManifest(context: Context, key: String): List<String> {
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_META_DATA or PackageManager.GET_SERVICES,
            )

        val classNames = mutableListOf<String>()
        if (packageInfo.services != null) {
            for (serviceInfo in packageInfo.services!!) {
                if (serviceInfo.metaData != null) {
                    val className = serviceInfo.metaData.getString(key)
                    if (className != null) {
                        classNames.add(className)
                    }
                }
            }
        }
        return classNames.toList()
    }

    private fun instantiateClosedSourceProvider(
        classNames: List<String>,
        context: Context,
    ): ProviderEventsApiProvider? {
        var provider: ProviderEventsApiProvider? = null
        for (className in classNames) {
            try {
                val klass = Class.forName(className)
                val p =
                    klass.getConstructor(Context::class.java).newInstance(context)
                        as ProviderEventsApiProvider
                if (p.isAvailable()) {
                    if (provider != null) { // Only one active OEM CredentialProvider allowed
                        return null
                    }
                    provider = p
                }
            } catch (_: Throwable) {}
        }
        return provider
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
