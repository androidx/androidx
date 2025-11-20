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

package androidx.appfunctions.service.internal

import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.service.AppFunctionConfiguration

/**
 * A factory that will incorporate [AppFunctionConfiguration] from [context] to create AppFunction
 * enclosing classes.
 *
 * If the application context from [context] overrides [AppFunctionConfiguration.Provider], the
 * customized factory method will be used to instantiate the enclosing class. Otherwise, it will use
 * [defaultFactory] to create the instance if available.
 *
 * [createEnclosingClass] will throw [AppFunctionInstantiationException] if unable to instantiate
 * the enclosing class.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ConfigurableAppFunctionFactory<T : Any>(
    private val context: Context,
    private val defaultFactory: (() -> T)? = null,
) {
    public fun createEnclosingClass(enclosingClass: Class<T>): T {
        val configurationProvider = context.applicationContext as? AppFunctionConfiguration.Provider
        val customFactory =
            configurationProvider
                ?.appFunctionConfiguration
                ?.enclosingClassFactories
                ?.get(enclosingClass)
        if (customFactory == null) {
            Log.d(APP_FUNCTIONS_TAG, "Unable to find custom factory for [$enclosingClass]")
            return createFromDefault(enclosingClass)
        }

        val instance = customFactory.invoke()
        @Suppress("UNCHECKED_CAST")
        return instance as T
    }

    private fun createFromDefault(enclosingClass: Class<T>): T {
        if (defaultFactory == null) {
            throw AppFunctionInstantiationException(
                "Unable to instantiate $enclosingClass. " +
                    "Either setup a custom factory with AppFunctionConfiguration or provide a " +
                    "public no-arg constructor."
            )
        }
        return defaultFactory.invoke()
    }

    /** Thrown when unable to instantiate the AppFunction enclosing class. */
    public class AppFunctionInstantiationException(errorMessage: String) :
        RuntimeException(errorMessage)
}
