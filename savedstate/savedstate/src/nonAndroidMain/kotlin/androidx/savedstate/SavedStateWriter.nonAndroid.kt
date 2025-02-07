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
internal actual constructor(
    @PublishedApi internal actual val source: SavedState,
) {

    public actual inline fun putBoolean(key: String, value: Boolean) {
        source.map[key] = value
    }

    public actual inline fun putChar(key: String, value: Char) {
        source.map[key] = value
    }

    public actual inline fun putCharSequence(key: String, value: CharSequence) {
        source.map[key] = value
    }

    public actual inline fun putDouble(key: String, value: Double) {
        source.map[key] = value
    }

    public actual inline fun putFloat(key: String, value: Float) {
        source.map[key] = value
    }

    public actual inline fun putInt(key: String, value: Int) {
        source.map[key] = value
    }

    public actual inline fun putLong(key: String, value: Long) {
        source.map[key] = value
    }

    public actual inline fun putNull(key: String) {
        source.map[key] = null
    }

    public actual inline fun putString(key: String, value: String) {
        source.map[key] = value
    }

    public actual inline fun putCharSequenceList(key: String, value: List<CharSequence>) {
        source.map[key] = value
    }

    public actual inline fun putIntList(key: String, value: List<Int>) {
        source.map[key] = value
    }

    public actual inline fun putSavedStateList(key: String, value: List<SavedState>) {
        source.map[key] = value
    }

    public actual inline fun putStringList(key: String, value: List<String>) {
        source.map[key] = value
    }

    public actual inline fun putBooleanArray(key: String, value: BooleanArray) {
        source.map[key] = value
    }

    public actual inline fun putCharArray(key: String, value: CharArray) {
        source.map[key] = value
    }

    public actual inline fun putCharSequenceArray(key: String, value: Array<CharSequence>) {
        source.map[key] = value
    }

    public actual inline fun putDoubleArray(key: String, value: DoubleArray) {
        source.map[key] = value
    }

    public actual inline fun putFloatArray(key: String, value: FloatArray) {
        source.map[key] = value
    }

    public actual inline fun putIntArray(key: String, value: IntArray) {
        source.map[key] = value
    }

    public actual inline fun putLongArray(key: String, value: LongArray) {
        source.map[key] = value
    }

    public actual inline fun putSavedStateArray(key: String, value: Array<SavedState>) {
        source.map[key] = value
    }

    public actual inline fun putStringArray(key: String, value: Array<String>) {
        source.map[key] = value
    }

    public actual inline fun putSavedState(key: String, value: SavedState) {
        source.map[key] = value
    }

    public actual inline fun putAll(from: SavedState) {
        source.map.putAll(from.map)
    }

    public actual inline fun remove(key: String) {
        source.map.remove(key)
    }

    public actual inline fun clear() {
        source.map.clear()
    }
}
