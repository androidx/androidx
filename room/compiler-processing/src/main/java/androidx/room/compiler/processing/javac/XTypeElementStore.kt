/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XTypeElement
import java.lang.ref.WeakReference

/**
 * Utility class to cache type element wrappers.
 */
internal class XTypeElementStore<T : XTypeElement>(
    val findElement: (qName: String) -> T?
) {
    // instead of something like a Guava cache, we use a map of weak references here because our
    // main goal is avoiding to re-parse type elements as we go up & down in the hierarchy while
    // not necessarily wanting to preserve type elements after we are done with them. Doing that
    // could possibly hold a lot more information than we desire.
    private val typeCache = mutableMapOf<String, WeakReference<T>>()

    operator fun get(qName: String): T? {
        typeCache[qName]?.get()?.let {
            return it
        }
        val result = findElement(qName) ?: return null
        typeCache[qName] = WeakReference(result)
        return result
    }
}