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
@file:JvmName("AppDataKeyUtil")

package androidx.wear.protolayout.expression

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicDuration
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicType

/**
 * Represent a [DynamicDataKey] that references app/tile pushed state data.
 *
 * @param T The data type of the dynamic values that this key is bound to.
 * @param key The key in the state to bind to.
 */
public class AppDataKey<T : DynamicType>(key: String) : DynamicDataKey<T>(DEFAULT_NAMESPACE, key) {
    private companion object {
        private const val DEFAULT_NAMESPACE = ""
    }
}

/**
 * Create a [AppDataKey] with the specified key that references a boolean dynamic data.
 *
 * @param key The key in the state to bind to.
 */
public fun boolAppDataKey(key: String): AppDataKey<DynamicBool> = AppDataKey<DynamicBool>(key)

/**
 * Create a [AppDataKey] with the specified key that references a color dynamic data.
 *
 * @param key The key in the state to bind to.
 */
public fun colorAppDataKey(key: String): AppDataKey<DynamicColor> = AppDataKey<DynamicColor>(key)

/**
 * Create a [AppDataKey] with the specified key that references a [java.time.Duration] dynamic data.
 *
 * @param key The key in the state to bind to.
 */
public fun durationAppDataKey(key: String): AppDataKey<DynamicDuration> =
    AppDataKey<DynamicDuration>(key)

/**
 * Create a [AppDataKey] with the specified key that references a float dynamic data.
 *
 * @param key The key in the state to bind to.
 */
public fun floatAppDataKey(key: String): AppDataKey<DynamicFloat> = AppDataKey<DynamicFloat>(key)

/**
 * Create a [AppDataKey] with the specified key that references an [java.time.Instant] dynamic data.
 *
 * @param key The key in the state to bind to.
 */
public fun instantAppDataKey(key: String): AppDataKey<DynamicInstant> =
    AppDataKey<DynamicInstant>(key)

/**
 * Create a [AppDataKey] with the specified key that references an int dynamic data.
 *
 * @param key The key in the state to bind to.
 */
public fun intAppDataKey(key: String): AppDataKey<DynamicInt32> = AppDataKey<DynamicInt32>(key)

/**
 * Create a [AppDataKey] with the specified key that references a string dynamic data.
 *
 * @param key The key in the state to bind to.
 */
public fun stringAppDataKey(key: String): AppDataKey<DynamicString> = AppDataKey<DynamicString>(key)
