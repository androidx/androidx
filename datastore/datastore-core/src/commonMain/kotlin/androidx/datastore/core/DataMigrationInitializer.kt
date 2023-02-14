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

/**
 * Returns an initializer function created from a list of DataMigrations.
 */
internal class DataMigrationInitializer<T>() {
    companion object {
        /**
         * Creates an initializer from DataMigrations for use with DataStore.
         *
         * @param migrations A list of migrations that will be included in the initializer.
         * @return The initializer which includes the data migrations returned from the factory
         * functions.
         */
        fun <T> getInitializer(migrations: List<DataMigration<T>>):
            suspend (api: InitializerApi<T>) -> Unit = { api ->
                runMigrations(migrations, api)
            }

        private suspend fun <T> runMigrations(
            migrations: List<DataMigration<T>>,
            api: InitializerApi<T>
        ) {
            val cleanUps = mutableListOf<suspend () -> Unit>()

            api.updateData { startingData ->
                migrations.fold(startingData) { data, migration ->
                    if (migration.shouldMigrate(data)) {
                        cleanUps.add { migration.cleanUp() }
                        migration.migrate(data)
                    } else {
                        data
                    }
                }
            }

            var cleanUpFailure: Throwable? = null

            cleanUps.forEach { cleanUp ->
                try {
                    cleanUp()
                } catch (exception: Throwable) {
                    if (cleanUpFailure == null) {
                        cleanUpFailure = exception
                    } else {
                        cleanUpFailure!!.addSuppressed(exception)
                    }
                }
            }

            // If we encountered a failure on cleanup, throw it.
            cleanUpFailure?.let { throw it }
        }
    }
}