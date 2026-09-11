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

package androidx.lifecycle.internal

import android.os.Binder
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.lifecycle.SavedStateHandle
import androidx.savedstate.SavedState
import androidx.savedstate.read
import java.io.Serializable

internal actual fun isAcceptableType(value: Any?): Boolean =
    value == null || ACCEPTABLE_CLASSES.any { classRef -> classRef.isInstance(value) }

internal actual fun <T> unwrapSavedStateValue(value: T): T {
    if (value is SavedState) {
        val classLoader = SavedStateHandle::class.java.classLoader
        value.classLoader = classLoader
        if (value.read { contains(SAVED_STATE_VALUE_KEY) }) {
            val innerValue = value.read { toMap()[SAVED_STATE_VALUE_KEY] }
            if (innerValue is SavedState) {
                innerValue.classLoader = classLoader
            }
            @Suppress("UNCHECKED_CAST")
            return innerValue as T
        }
    }
    return value
}

// doesn't have Integer, Long etc box types because they are "Serializable"
private val ACCEPTABLE_CLASSES =
    arrayOf(
            // baseBundle
            Boolean::class.javaPrimitiveType,
            BooleanArray::class.java,
            Double::class.javaPrimitiveType,
            DoubleArray::class.java,
            Int::class.javaPrimitiveType,
            IntArray::class.java,
            Long::class.javaPrimitiveType,
            LongArray::class.java,
            String::class.java,
            Array<String>::class.java, // bundle
            Binder::class.java,
            Bundle::class.java,
            Byte::class.javaPrimitiveType,
            ByteArray::class.java,
            Char::class.javaPrimitiveType,
            CharArray::class.java,
            CharSequence::class.java,
            Array<CharSequence>::class.java,
            // type erasure ¯\_(ツ)_/¯, we won't eagerly check elements contents
            ArrayList::class.java,
            Float::class.javaPrimitiveType,
            FloatArray::class.java,
            Parcelable::class.java,
            Array<Parcelable>::class.java,
            Serializable::class.java,
            Short::class.javaPrimitiveType,
            ShortArray::class.java,
            SparseArray::class.java,
            Size::class.java,
            SizeF::class.java,
        )
        .filterNotNull()
