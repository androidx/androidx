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

<<<<<<<< HEAD:collection/collection/src/jbMain/kotlin/androidx/collection/internal/Lock.skiko.kt
package androidx.collection.internal

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal actual class Lock {
    private val synchronizedObject = SynchronizedObject()

    actual inline fun <T> synchronizedImpl(block: () -> T): T =
        synchronized(synchronizedObject, block)
========
package androidx.paging

/**
 * Fetcher-side callbacks for presenter-side access events communicated through [PagingData].
 */
internal interface HintReceiver {
    fun accessHint(viewportHint: ViewportHint)
>>>>>>>> sync-androidx/revert/revert-1.6.0-alpha01_merge-1.6.0-alpha02:paging/paging-common/src/commonMain/kotlin/androidx/paging/HintReceiver.kt
}
