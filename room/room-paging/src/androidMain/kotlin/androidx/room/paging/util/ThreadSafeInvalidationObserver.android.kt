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
package androidx.room.paging.util

import androidx.annotation.RestrictTo
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("UNCHECKED_CAST")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ThreadSafeInvalidationObserver(
    tables: Array<out String>,
    public val onInvalidated: () -> Unit,
) : InvalidationTracker.Observer(tables = tables as Array<String>) {
    private val registered: AtomicBoolean = AtomicBoolean(false)

    override fun onInvalidated(tables: Set<String>) {
        onInvalidated()
    }

    public fun registerIfNecessary(db: RoomDatabase) {
        if (registered.compareAndSet(false, true)) {
            db.invalidationTracker.addWeakObserver(this)
        }
    }
}
