/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.common.internal

import androidx.compose.runtime.NoLiveLiterals

// a copy from compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Expect.kt

private var nextHash = 1
private const val IDENTITY_HASHCODE_FIELD = "kotlinIdentityHashcodeValue$"

@NoLiveLiterals
private fun memoizeIdentityHashCode(instance: Any?): Int {
    val value = nextHash++

    val descriptor = js("new Object()")
    descriptor.value = value
    descriptor.writable = false
    descriptor.configurable = false
    descriptor.enumerable = false

    js("Object").defineProperty(instance, IDENTITY_HASHCODE_FIELD, descriptor)

    return value
}
internal actual fun identityHashCode(instance: Any?): Int {
    if (instance == null) {
        return 0
    }

    val hashCode = instance.asDynamic()[IDENTITY_HASHCODE_FIELD]
    if (hashCode != null) {
        return hashCode
    }

    return when (jsTypeOf(instance)) {
        "object", "function" -> memoizeIdentityHashCode(instance)
        else -> throw IllegalArgumentException(
            "identity hash code for ${jsTypeOf(instance)} is not supported"
        )
    }
}
