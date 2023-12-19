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

@file:JvmName("KClassUtil")
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.room.util

import androidx.annotation.RestrictTo
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

/**
 * Determines if the class or interface represented by this object is the same as the class or
 * interface represented by the specified [KClass] parameter. Such case is only true if
 * the qualified name of both classes match.
 */
internal actual fun KClass<*>.isAssignableFrom(other: KClass<*>): Boolean {
    return this.qualifiedName == other.qualifiedName
}
