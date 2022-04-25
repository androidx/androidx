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
package androidx.datastore.core

import androidx.datastore.core.handlers.NoOpCorruptionHandler
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

// we can rename this since it is internal but also used a lot in tests so nice to have :)
@Suppress("FunctionName")
internal fun <T> SingleProcessDataStore(
    produceFile: () -> File,
    serializer: Serializer<T>,
    initTasksList: List<suspend (api: InitializerApi<T>) -> Unit> = emptyList(),
    corruptionHandler: CorruptionHandler<T> = NoOpCorruptionHandler<T>(),
    scope: CoroutineScope = CoroutineScope(ioDispatcher() + SupervisorJob())
): SingleProcessDataStore<T> = SingleProcessDataStore(
    storage = FileStorage(produceFile, serializer),
    initTasksList = initTasksList,
    corruptionHandler = corruptionHandler,
    scope = scope
)