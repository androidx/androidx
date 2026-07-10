/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.datastore.core.util

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * Utility function to get a [CoroutineContext] for "old" DataStoreFactory functions that take in a
 * [CoroutineScope] instead of the new [androidx.datastore.core.DataStore.Builder] API that takes in
 * a [CoroutineContext].
 *
 * Creating a Job is not ideal because DataStore has no close API so it will never get canceled, but
 * it maintains existing behavior for users who pass a global-like CoroutineScope.
 */
internal fun getContextFromScope(scope: CoroutineScope): CoroutineContext {
    return if (scope.coroutineContext[Job] == null) {
        scope.coroutineContext + Job()
    } else {
        scope.coroutineContext
    }
}
