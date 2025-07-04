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

@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.implementedInJetBrainsFork

actual typealias SnapshotId = Long

internal actual const val SnapshotIdZero: SnapshotId = 0L
internal actual const val SnapshotIdMax: SnapshotId = Long.MAX_VALUE
internal actual const val SnapshotIdSize: Int = Long.SIZE_BITS
internal actual const val SnapshotIdInvalidValue: SnapshotId = -1

internal actual operator fun SnapshotId.compareTo(other: SnapshotId): Int =
    implementedInJetBrainsFork()

internal actual operator fun SnapshotId.compareTo(other: Int): Int = implementedInJetBrainsFork()

internal actual operator fun SnapshotId.plus(other: Int): SnapshotId = implementedInJetBrainsFork()

internal actual operator fun SnapshotId.minus(other: SnapshotId): SnapshotId =
    implementedInJetBrainsFork()

internal actual operator fun SnapshotId.minus(other: Int): SnapshotId = implementedInJetBrainsFork()

internal actual operator fun SnapshotId.div(other: Int): SnapshotId = implementedInJetBrainsFork()

internal actual operator fun SnapshotId.times(other: Int): SnapshotId = implementedInJetBrainsFork()

actual fun SnapshotId.toInt(): Int = implementedInJetBrainsFork()

actual fun SnapshotId.toLong(): Long = implementedInJetBrainsFork()

actual typealias SnapshotIdArray = LongArray

internal actual fun snapshotIdArrayWithCapacity(capacity: Int): SnapshotIdArray =
    implementedInJetBrainsFork()

internal actual operator fun SnapshotIdArray.get(index: Int): SnapshotId =
    implementedInJetBrainsFork()

internal actual operator fun SnapshotIdArray.set(index: Int, value: SnapshotId) {
    implementedInJetBrainsFork()
}

internal actual val SnapshotIdArray.size: Int
    get() = implementedInJetBrainsFork()

internal actual fun SnapshotIdArray.copyInto(other: SnapshotIdArray) {
    implementedInJetBrainsFork()
}

internal actual fun SnapshotIdArray.first(): SnapshotId = implementedInJetBrainsFork()

internal actual fun SnapshotIdArray.binarySearch(id: SnapshotId): Int = implementedInJetBrainsFork()

internal actual inline fun SnapshotIdArray.forEach(block: (SnapshotId) -> Unit): Unit =
    implementedInJetBrainsFork()

internal actual fun SnapshotIdArray.withIdInsertedAt(index: Int, id: SnapshotId): SnapshotIdArray =
    implementedInJetBrainsFork()

internal actual fun SnapshotIdArray.withIdRemovedAt(index: Int): SnapshotIdArray? =
    implementedInJetBrainsFork()

internal actual class SnapshotIdArrayBuilder actual constructor(array: SnapshotIdArray?) {
    actual fun add(id: SnapshotId): Unit = implementedInJetBrainsFork()

    actual fun toArray(): SnapshotIdArray? = implementedInJetBrainsFork()
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun snapshotIdArrayOf(id: SnapshotId): SnapshotIdArray =
    implementedInJetBrainsFork()

internal actual fun Int.toSnapshotId(): SnapshotId = implementedInJetBrainsFork()
