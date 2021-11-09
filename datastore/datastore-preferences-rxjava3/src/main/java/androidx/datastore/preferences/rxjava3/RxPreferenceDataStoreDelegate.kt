/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.datastore.preferences.rxjava3

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.datastore.core.DataMigration
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.rxjava3.RxDataStore
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Creates a property delegate for a single process Preferences DataStore. This should only be
 * called once in a file (at the top level), and all usages of the DataStore should use a
 * reference the same Instance. The receiver type for the property delegate must be an instance
 * of [Context].
 *
 * This should only be used from a single application in a single classloader in a single process.
 *
 * Example usage:
 * ```
 * val Context.myRxDataStore by rxPreferencesDataStore("filename", serializer)
 *
 * class SomeClass(val context: Context) {
 *    fun update(): Single<Preferences> = context.myRxDataStore.updateDataAsync {...}
 * }
 * ```
 *
 *
 * @param name The name of the preferences. The preferences will be stored in a file in the
 * "datastore/" subdirectory in the application context's files directory and is generated using
 * [preferencesDataStoreFile].
 * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
 * [androidx.datastore.core.CorruptionException] when attempting to read data. CorruptionExceptions
 * are thrown by serializers when data can not be de-serialized.
 * @param produceMigrations produce the migrations. The ApplicationContext is passed in to these
 * callbacks as a parameter. DataMigrations are run before any access to data can occur. Each
 * producer and migration may be run more than once whether or not it already succeeded
 * (potentially because another migration failed or a write to disk failed.)
 * @param scheduler The scope in which IO operations and transform functions will execute.
 *
 * @return a property delegate that manages a datastore as a singleton.
 */
@Suppress("MissingJvmstatic")
public fun rxPreferencesDataStore(
    name: String,
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    produceMigrations: (Context) -> List<DataMigration<Preferences>> = { listOf() },
    scheduler: Scheduler = Schedulers.io()
): ReadOnlyProperty<Context, RxDataStore<Preferences>> {
    return RxDataStoreSingletonDelegate(name, corruptionHandler, produceMigrations, scheduler)
}

/**
 * Delegate class to manage DataStore as a singleton.
 */
internal class RxDataStoreSingletonDelegate internal constructor(
    private val fileName: String,
    private val corruptionHandler: ReplaceFileCorruptionHandler<Preferences>?,
    private val produceMigrations: (Context) -> List<DataMigration<Preferences>>,
    private val scheduler: Scheduler
) : ReadOnlyProperty<Context, RxDataStore<Preferences>> {

    private val lock = Any()

    @GuardedBy("lock")
    @Volatile
    private var INSTANCE: RxDataStore<Preferences>? = null

    /**
     * Gets the instance of the DataStore.
     *
     * @param thisRef must be an instance of [Context]
     * @param property not used
     */
    override fun getValue(thisRef: Context, property: KProperty<*>): RxDataStore<Preferences> {
        return INSTANCE ?: synchronized(lock) {
            if (INSTANCE == null) {
                val applicationContext = thisRef.applicationContext

                INSTANCE = with(RxPreferenceDataStoreBuilder(applicationContext, fileName)) {
                    setIoScheduler(scheduler)
                    @Suppress("NewApi", "ClassVerificationFailure") // b/187418647
                    produceMigrations(applicationContext).forEach {
                        addDataMigration(it)
                    }
                    corruptionHandler?.let { setCorruptionHandler(it) }
                    build()
                }
            }
            INSTANCE!!
        }
    }
}