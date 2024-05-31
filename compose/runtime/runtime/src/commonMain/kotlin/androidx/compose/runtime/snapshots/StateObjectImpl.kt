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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.Composition
import androidx.compose.runtime.internal.AtomicInt
import kotlin.jvm.JvmInline

/**
 * A [StateObject] that allows to record reader type when observed to optimize recording of
 * modifications. Currently only reads in [Composition] and [SnapshotStateObserver] is supported.
 * The methods are intentionally restricted to the internal types, as the API is expected to change.
 */
internal abstract class StateObjectImpl internal constructor() : StateObject {
    private val readerKind = AtomicInt(0)

    internal fun recordReadIn(reader: ReaderKind) {
        do {
            val old = ReaderKind(readerKind.get())
            if (old.isReadIn(reader)) return

            val new = old.withReadIn(reader)
        } while (!readerKind.compareAndSet(old.mask, new.mask))
    }

    internal fun isReadIn(reader: ReaderKind): Boolean =
        ReaderKind(readerKind.get()).isReadIn(reader)
}

@JvmInline
internal value class ReaderKind(val mask: Int = 0) {
    @Suppress("NOTHING_TO_INLINE")
    inline fun withReadIn(reader: ReaderKind): ReaderKind = ReaderKind(mask or reader.mask)

    @Suppress("NOTHING_TO_INLINE")
    inline fun isReadIn(reader: ReaderKind): Boolean = mask and reader.mask != 0

    internal companion object {
        inline val Composition
            get() = ReaderKind(mask = 1 shl 0)

        inline val SnapshotStateObserver
            get() = ReaderKind(mask = 1 shl 1)

        inline val SnapshotFlow
            get() = ReaderKind(mask = 1 shl 2)
    }
}
