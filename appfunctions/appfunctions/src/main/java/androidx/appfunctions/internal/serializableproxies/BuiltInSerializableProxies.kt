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

package androidx.appfunctions.internal.serializableproxies

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.internal.AppFunctionSerializableFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Registry of built-in [androidx.appfunctions.AppFunctionSerializableProxy] implementations and
 * their factories.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object BuiltInSerializableProxies {
    /**
     * Factories of the built-in proxy types, keyed by the simple name of each proxy class.
     *
     * Lookups must not rely on [Class] identity: when an app enables Java library desugaring, the
     * `java.time` class references in this library are rewritten to backported classes, while
     * callers still resolve e.g. `java.time.LocalDateTime` by name. Simple names are unaffected.
     */
    // LINT.IfChange(supported_proxies)
    private val FACTORY_MAP: Map<String, () -> AppFunctionSerializableFactory<*>> =
        mapOf(
            LocalDate::class.java.simpleName to { `$LocalDateFactory`() },
            LocalTime::class.java.simpleName to { `$LocalTimeFactory`() },
            LocalDateTime::class.java.simpleName to { `$LocalDateTimeFactory`() },
            Uri::class.java.simpleName to { `$UriFactory`() },
            Instant::class.java.simpleName to { `$InstantFactory`() },
            ZoneId::class.java.simpleName to { `$ZoneIdFactory`() },
        )

    // LINT.ThenChange(/appfunctions/appfunctions-compiler/src/main/java/androidx/appfunctions/compiler/core/AppFunctionTypeReference.kt:supported_proxies, /appfunctions/appfunctions/src/main/java/androidx/appfunctions/AppFunctionSerializable.kt:supported_proxies)

    /** Checks whether the provided [clazz] is supported as a built-in proxy type. */
    public fun isSupportedProxy(clazz: Class<*>): Boolean =
        FACTORY_MAP.containsKey(clazz.simpleName)

    /**
     * Returns an [AppFunctionSerializableFactory] for the provided [clazz] if it is a built-in
     * proxy type, or `null` otherwise.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> getFactory(clazz: Class<T>): AppFunctionSerializableFactory<T>? =
        FACTORY_MAP[clazz.simpleName]?.invoke() as? AppFunctionSerializableFactory<T>
}
