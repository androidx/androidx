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

@file:JvmName("SavedStateWriterKt")
@file:JvmMultifileClass
@file:Suppress("NOTHING_TO_INLINE")

package androidx.savedstate

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

@JvmInline
public actual value class SavedStateWriter
@PublishedApi
internal actual constructor(private actual val source: SavedState) {

    public actual fun putBoolean(key: String, value: Boolean) {
        source.map[key] = value
    }

    public actual fun putChar(key: String, value: Char) {
        source.map[key] = value
    }

    public actual fun putCharSequence(key: String, value: CharSequence) {
        source.map[key] = value
    }

    public actual fun putDouble(key: String, value: Double) {
        source.map[key] = value
    }

    public actual fun putFloat(key: String, value: Float) {
        source.map[key] = value
    }

    public actual fun putInt(key: String, value: Int) {
        source.map[key] = value
    }

    public actual fun putLong(key: String, value: Long) {
        source.map[key] = value
    }

    public actual fun putNull(key: String) {
        source.map[key] = null
    }

    public actual fun putString(key: String, value: String) {
        source.map[key] = value
    }

    public actual fun putCharSequenceList(key: String, value: List<CharSequence>) {
        source.map[key] = value
    }

    public actual fun putIntList(key: String, value: List<Int>) {
        source.map[key] = value
    }

    public actual fun putSavedStateList(key: String, value: List<SavedState>) {
        source.map[key] = value
    }

    public actual fun putStringList(key: String, value: List<String>) {
        source.map[key] = value
    }

    public actual fun putBooleanArray(key: String, value: BooleanArray) {
        source.map[key] = value
    }

    public actual fun putCharArray(key: String, value: CharArray) {
        source.map[key] = value
    }

    public actual fun putCharSequenceArray(key: String, value: Array<CharSequence>) {
        source.map[key] = value
    }

    public actual fun putDoubleArray(key: String, value: DoubleArray) {
        source.map[key] = value
    }

    public actual fun putFloatArray(key: String, value: FloatArray) {
        source.map[key] = value
    }

    public actual fun putIntArray(key: String, value: IntArray) {
        source.map[key] = value
    }

    public actual fun putLongArray(key: String, value: LongArray) {
        source.map[key] = value
    }

    public actual fun putSavedStateArray(key: String, value: Array<SavedState>) {
        source.map[key] = value
    }

    public actual fun putStringArray(key: String, value: Array<String>) {
        source.map[key] = value
    }

    public actual fun putSavedState(key: String, value: SavedState) {
        source.map[key] = value
    }

    public actual fun putAll(from: SavedState) {
        source.map.putAll(from.map)
    }

    public actual fun remove(key: String) {
        source.map.remove(key)
    }

    public actual fun clear() {
        source.map.clear()
    }
}
