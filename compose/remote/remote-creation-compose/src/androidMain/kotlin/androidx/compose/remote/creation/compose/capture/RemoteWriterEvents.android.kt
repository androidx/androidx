/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.capture

import android.app.PendingIntent
import androidx.annotation.RestrictTo
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.compose.remote.creation.compose.util.fastForEachIndexed

/**
 * Collects side-band objects (such as [PendingIntent]s) during document capture that cannot be
 * serialized directly into the remote document.
 */
public class RemoteWriterEvents {
    private val pendingIntentList: MutableList<PendingIntent> = mutableListOf()
    private val lambdaMap: MutableIntObjectMap<() -> Unit> = MutableIntObjectMap()

    internal fun storePendingIntent(pendingIntent: PendingIntent): Int {
        val existingIndex = pendingIntentList.indexOfFirst { it === pendingIntent }
        if (existingIndex != -1) {
            return existingIndex
        }

        pendingIntentList.add(pendingIntent)
        return pendingIntentList.lastIndex
    }

    /**
     * An [IntObjectMap] of [PendingIntent]s collected during document capture, indexed by their
     * action IDs.
     */
    public val pendingIntents: IntObjectMap<PendingIntent>
        get() =
            MutableIntObjectMap<PendingIntent>().apply {
                pendingIntentList.fastForEachIndexed { index, pendingIntent ->
                    this[index] = pendingIntent
                }
            }

    internal fun storeLambda(id: Int, lambda: () -> Unit) {
        lambdaMap[id] = lambda
    }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val lambdas: IntObjectMap<() -> Unit>
        get() = lambdaMap
}

/** Bridge for modules pinned to a prebuilt that still references `WriterEvents`. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public class WriterEvents
