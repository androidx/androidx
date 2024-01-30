/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.connect.client.records

internal inline fun <reified T> getAllIntDefEnums(pattern: String): Collection<Int> {
    val regex = pattern.toRegex()

    return T::class.java.fields
        .asSequence()
        .filter { it.name.matches(regex) }
        .filter { it.type == Int::class.javaPrimitiveType }
        .map { it.get(null) }
        .filterIsInstance<Int>()
        .toHashSet()
}
