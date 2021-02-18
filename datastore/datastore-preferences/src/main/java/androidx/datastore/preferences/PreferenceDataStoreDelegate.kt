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

package androidx.datastore.preferences

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Creates a property delegate for a single process DataStore. This should only be called once
 * in a file (at the top level), and all usages of the DataStore should use a reference the same
 * Instance. The receiver type for the property delegate must be an instance of [Context].
 *
 * Example usage:
 * ```
 * val Context.myDataStore by preferencesDataStore("filename")
 *
 * class SomeClass(val context: Context) {
 *    suspend fun update() = context.myDataStore.edit {...}
 * }
 * ```
 *
 *
 * @param name The name of the preferences. The preferences will be stored in a file obtained
 * by calling: File(context.filesDir, "datastore/" + name + ".preferences_pb")
 * @param corruptionHandler The corruptionHandler is invoked if DataStore encounters a
 * [androidx.datastore.core.CorruptionException] when attempting to read data. CorruptionExceptions
 * are thrown by serializers when data can not be de-serialized.
 * @param migrations are run before any access to data can occur. Each producer and migration
 * may be run more than once whether or not it already succeeded (potentially because another
 * migration failed or a write to disk failed.)
 * @param scope The scope in which IO operations and transform functions will execute.
 *
 * @return a property delegate that manages a datastore as a singleton.
 */
@JvmOverloads
public fun preferencesDataStore(
    name: String,
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    migrations: List<DataMigration<Preferences>> = listOf(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
): ReadOnlyProperty<Context, DataStore<Preferences>> {
    return PreferenceDataStoreSingletonDelegate(name, corruptionHandler, migrations, scope)
}

/**
 * Delegate class to manage Preferences DataStore as a singleton.
 */
internal class PreferenceDataStoreSingletonDelegate internal constructor(
    private val name: String,
    private val corruptionHandler: ReplaceFileCorruptionHandler<Preferences>?,
    private val migrations: List<DataMigration<Preferences>>,
    private val scope: CoroutineScope
) : ReadOnlyProperty<Context, DataStore<Preferences>> {

    private val lock = Any()

    @GuardedBy("lock")
    @Volatile
    private var INSTANCE: DataStore<Preferences>? = null

    /**
     * Gets the instance of the DataStore.
     *
     * @param thisRef must be an instance of [Context]
     * @param property not used
     */
    override fun getValue(thisRef: Context, property: KProperty<*>): DataStore<Preferences> {
        return INSTANCE ?: synchronized(lock) {
            if (INSTANCE == null) {
                INSTANCE = PreferenceDataStoreFactory.create(
                    corruptionHandler = corruptionHandler,
                    migrations = migrations,
                    scope = scope
                ) {
                    File(
                        thisRef.applicationContext.filesDir, "datastore/$name.preferences_pb"
                    )
                }
            }
            INSTANCE!!
        }
    }
}