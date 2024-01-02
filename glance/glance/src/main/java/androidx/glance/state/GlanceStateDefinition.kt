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

package androidx.glance.state

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Configuration definition for [GlanceState]. This defines where the data is stored and how the
 * underlying data store is created. Use a unique [GlanceStateDefinition] to get a [GlanceState], once
 * defined, the data should be updated using the state directly, this definition should not change.
 */
interface GlanceStateDefinition<T> {

    /**
     * This file indicates the location of the persisted data.
     *
     * @param context The context used to create the file directory
     * @param fileKey The unique string key used to name and identify the data file corresponding to
     * a remote UI. Each remote UI has a unique UI key, used to key the data for that UI.
     */
    fun getLocation(context: Context, fileKey: String): File

    /**
     * Creates the underlying data store.
     *
     * @param context The context used to create locate the file directory
     * @param fileKey The unique string key used to name and identify the data file corresponding to
     * a remote UI. Each remote UI has a unique UI key, used to key the data for that UI.
     */
    suspend fun getDataStore(context: Context, fileKey: String): DataStore<T>
}

/**
 * Interface for an object that manages configuration for glanceables using the given
 * GlanceStateDefinition.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ConfigManager {
    /**
     * Returns the stored data associated with the given UI key string.
     *
     * @param definition the configuration that defines this state.
     * @param fileKey identifies the data file associated with the store, must be unique for any
     * remote UI in the app.
     */
    suspend fun <T> getValue(
        context: Context,
        definition: GlanceStateDefinition<T>,
        fileKey: String
    ): T

    /**
     * Updates the underlying data by applying the provided update block.
     *
     * @param definition the configuration that defines this state.
     * @param fileKey identifies the date file associated with the store, must be unique for any
     * remote UI in the app.
     */
    suspend fun <T> updateValue(
        context: Context,
        definition: GlanceStateDefinition<T>,
        fileKey: String,
        updateBlock: suspend (T) -> T
    ): T

    /**
     * Delete the file underlying the [DataStore] and remove local references to the [DataStore].
     */
    suspend fun deleteStore(
        context: Context,
        definition: GlanceStateDefinition<*>,
        fileKey: String
    )
}

/**
 * Data store for data specific to the glanceable view. Stored data should include information
 * relevant to the representation of views, but not surface specific view data. For example, the
 * month displayed on a calendar rather than actual calendar entries.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object GlanceState : ConfigManager {
    override suspend fun <T> getValue(
        context: Context,
        definition: GlanceStateDefinition<T>,
        fileKey: String
    ): T = getDataStore(context, definition, fileKey).data.first()

    override suspend fun <T> updateValue(
        context: Context,
        definition: GlanceStateDefinition<T>,
        fileKey: String,
        updateBlock: suspend (T) -> T
    ): T = getDataStore(context, definition, fileKey).updateData(updateBlock)

    override suspend fun deleteStore(
        context: Context,
        definition: GlanceStateDefinition<*>,
        fileKey: String
    ) {
        mutex.withLock {
            dataStores.remove(fileKey)
            val location = definition.getLocation(context, fileKey)
            location.delete()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> getDataStore(
        context: Context,
        definition: GlanceStateDefinition<T>,
        fileKey: String
    ): DataStore<T> =
        mutex.withLock {
            dataStores.getOrPut(fileKey) {
                definition.getDataStore(context, fileKey)
            } as DataStore<T>
        }

    private val mutex = Mutex()

    // TODO: Move to a single, global source to manage the data lifecycle
    private val dataStores: MutableMap<String, DataStore<*>> = mutableMapOf()
}

/**
 * Base class helping the creation of a state using DataStore's [Preferences].
 */
object PreferencesGlanceStateDefinition : GlanceStateDefinition<Preferences> {
    private var coroutineScope: CoroutineScope? = null
    override fun getLocation(context: Context, fileKey: String): File =
        context.preferencesDataStoreFile(fileKey)

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Preferences> {
        return coroutineScope?.let {
            PreferenceDataStoreFactory.create(scope = it) {
                context.preferencesDataStoreFile(
                    fileKey
                )
            }
        } ?: PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(fileKey) }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun setCoroutineScope(scope: CoroutineScope) {
        coroutineScope = scope
    }
}
