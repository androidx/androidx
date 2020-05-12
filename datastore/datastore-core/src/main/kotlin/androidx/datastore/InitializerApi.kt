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

package androidx.datastore

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

/**
 * The initializer API allows changes to be made to store before data is accessed through
 * data or updateData.
 *
 * Initializers are executed in the order in which they are added. They must be idempotent
 * since they are run each time the DataStore starts, and they may be run multiple times by a
 * single instance if a downstream initializer fails.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
interface InitializerApi<T> {
    suspend fun updateData(transform: suspend (t: T) -> T): T
}