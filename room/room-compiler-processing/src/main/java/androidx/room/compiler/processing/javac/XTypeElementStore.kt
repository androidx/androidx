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
internal class XTypeElementStore<BackingType, T : XTypeElement>(
    private val findElement: (qName: String) -> BackingType?,
    private val getQName: (BackingType) -> String?,
    private val wrap: (type: BackingType) -> T
) {
    // instead of something like a Guava cache, we use a map of weak references here because our
    // main goal is avoiding to re-parse type elements as we go up & down in the hierarchy while
    // not necessarily wanting to preserve type elements after we are done with them. Doing that
    // could possibly hold a lot more information than we desire.
    private val typeCache = mutableMapOf<String, WeakReference<T>>()

    operator fun get(backingType: BackingType): T {
        val qName = getQName(backingType)
        @Suppress("FoldInitializerAndIfToElvis")
        if (qName == null) {
            // just wrap without caching, likely an error or local type in kotlin
            return wrap(backingType)
        }
        get(qName)?.let {
            return it
        }
        val wrapped = wrap(backingType)
        return cache(qName, wrapped)
    }

    operator fun get(qName: String): T? {
        typeCache[qName]?.get()?.let {
            return it
        }
        val result = findElement(qName)?.let(wrap) ?: return null
        return cache(qName, result)
    }

    private fun cache(qName: String, element: T): T {
        typeCache[qName] = WeakReference(element)
        return element
    }
}