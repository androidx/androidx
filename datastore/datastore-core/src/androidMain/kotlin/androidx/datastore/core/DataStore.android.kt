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

package androidx.datastore.core

import androidx.datastore.core.handlers.ReThrowCorruptionHandler
import androidx.tracing.Tracer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow

actual interface DataStore<T> {
    actual val data: Flow<T>

    actual suspend fun updateData(transform: suspend (t: T) -> T): T

    actual class Builder<T>
    actual constructor(private val storage: Storage<T>, private val context: CoroutineContext) {
        private var corruptionHandler: CorruptionHandler<T> = ReThrowCorruptionHandler()
        private var migrations: List<DataMigration<T>> = emptyList()
        private var tracer: DataStoreTracer? = null

        /**
         * Sets the [CorruptionHandler] for the DataStore.
         *
         * This handler is invoked if the [Storage] layer throws a [CorruptionException]. Defaults
         * to [ReThrowCorruptionHandler].
         *
         * @param handler the corruption handler.
         * @return this [Builder] instance.
         */
        @Suppress("MissingGetterMatchingBuilder")
        actual fun setCorruptionHandler(handler: CorruptionHandler<T>): Builder<T> = apply {
            this.corruptionHandler = handler
        }

        /**
         * Adds [DataMigration]s to the DataStore.
         *
         * Migrations are run in the order they are added before any data is returned to the user.
         *
         * @param migrations the list of migrations.
         * @return this [Builder] instance.
         */
        @Suppress("MissingGetterMatchingBuilder")
        actual fun addMigrations(migrations: List<DataMigration<T>>): Builder<T> = apply {
            this.migrations += migrations
        }

        /**
         * Sets the [Tracer] for Android.
         *
         * @param tracer The [Tracer] to use.
         * @return this [Builder] instance.
         */
        // TODO(b/486189894): When androidx.tracing becomes available in all target platforms
        //  supported by DataStore, move `setTracer()` to common.
        @Suppress("MissingGetterMatchingBuilder")
        fun setTracer(tracer: DataStoreTracer): Builder<T> = apply { this.tracer = tracer }

        /**
         * Validates the configuration and builds the [Builder] instance.
         *
         * @return a new DataStore instance.
         */
        actual fun build(): DataStore<T> {
            return DataStoreImpl(
                storage = storage,
                corruptionHandler = corruptionHandler,
                initTasksList = listOf(DataMigrationInitializer.getInitializer(migrations)),
                context = context,
                tracer = tracer,
            )
        }
    }
}
