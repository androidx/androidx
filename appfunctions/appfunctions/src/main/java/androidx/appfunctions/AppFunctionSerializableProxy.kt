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

package androidx.appfunctions

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import kotlin.reflect.KClass

/**
 * Marks an [AppFunctionSerializable] compatible class as a *serialization proxy* for a Java/Android
 * object.
 *
 * This annotation designates an [AppFunctionSerializable] compatible class as a *serialization
 * proxy* for a specified Java/Android object class ([targetClass]). This proxy will be used to
 * represent/serialize instances of [targetClass] in
 * [androidx.appfunctions.metadata.AppFunctionMetadata].
 *
 * The [targetClass] parameter specifies the Java/Android object being represented.
 *
 * The annotated class *must* adhere to these rules:
 * - It *must* be a valid [AppFunctionSerializable] (Even though it does not need this annotation).
 * - It *must* provide a public member function `to[targetClass]` that returns an instance of
 *   [targetClass].
 * - It *must* provide a public `companion object` function `from[targetClass]` that accepts a
 *   [targetClass] instance and returns the annotated proxy object. This function acts as a factory
 *   method.
 *
 * For example, to create a proxy for [java.time.LocalDateTime]:
 * ```
 * @AppFunctionSerializableProxy(targetClass = LocalDateTime::class)
 * public data class AppFunctionLocalDateTime(
 *     val year: Int,
 *     val month: Int,
 *     val dayOfMonth: Int,
 *     val hour: Int,
 *     val minute: Int,
 *     val second: Int,
 *     val nanoOfSecond: Int,
 * ) {
 *     public fun toLocalDateTime(): LocalDateTime {
 *         return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond)
 *     }
 *
 *     public companion object {
 *         public fun fromLocalDateTime(localDateTime: LocalDateTime): AppFunctionLocalDateTime {
 *             return AppFunctionLocalDateTime(
 *                 localDateTime.year,
 *                 localDateTime.monthValue,
 *                 localDateTime.dayOfMonth,
 *                 localDateTime.hour,
 *                 localDateTime.minute,
 *                 localDateTime.second,
 *                 localDateTime.nano
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * When [androidx.appfunctions.metadata.AppFunctionMetadata] is generated, any instance of
 * [java.time.LocalDateTime] will be represented by the
 * [androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata] of the `AppFunctionLocalDateTime`
 * proxy.
 *
 * This annotation is for internal library use only.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
public annotation class AppFunctionSerializableProxy(val targetClass: KClass<*>)
