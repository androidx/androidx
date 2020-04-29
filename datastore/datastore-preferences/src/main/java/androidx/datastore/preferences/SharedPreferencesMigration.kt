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
import android.content.SharedPreferences
import androidx.datastore.DataMigration
import java.io.File
import java.io.IOException

/**
 * A DataMigration which migrates SharedPreferences to DataStore.
 *
 * Note: this accesses the SharedPreferences using MODE_PRIVATE.
 */
class SharedPreferencesMigration private constructor() {
    companion object {
        internal val MIGRATE_ALL_KEYS = null

        @JvmStatic
        fun create(
            /** Context used for getting SharedPreferences. */
            context: Context,
            /** The name of the SharedPreferences. */
            sharedPreferencesName: String,
            /**
             * The list of keys to migrate. The keys will be mapped to datastore.Preferences with
             * their same values. If the key is already present in the new Preferences, the key
             * will not be migrated again. If the key is not present in the SharedPreferences it
             * will not be migrated.
             *
             * If keysToMigrate is null, all keys will be migrated from the existing
             * SharedPreferences.
             */
            keysToMigrate: Set<String>? = MIGRATE_ALL_KEYS,
            /**
             * If enabled and the SharedPreferences are empty (i.e. no remaining keys) after this
             * migration runs, the leftover SharedPreferences file is deleted. Note that this
             * cleanup runs only if the migration itself runs, i.e., if the keys were never in
             * SharedPreferences to begin with then the (potentially) empty SharedPreferences
             * won't be cleaned up by this option.
             */
            deleteEmptyPreferences: Boolean = true
        ): () -> DataMigration<Preferences> = {
            SharedPreferencesDataMigration(
                context,
                sharedPreferencesName,
                keysToMigrate?.toMutableSet(),
                deleteEmptyPreferences
            )
        }
    }

    private class SharedPreferencesDataMigration internal constructor(
        private val context: Context,
        private val sharedPreferencesName: String,
        keysToMigrate: MutableSet<String>?,
        private val deleteEmptyPreferences: Boolean
    ) : DataMigration<Preferences> {
        private val sharedPrefs: SharedPreferences by lazy {
            context.getSharedPreferences(
                sharedPreferencesName, Context.MODE_PRIVATE
            )
        }

        private val keySet: MutableSet<String> by lazy {
            keysToMigrate ?: sharedPrefs.all.keys.toMutableSet()
        }

        override suspend fun shouldMigrate(currentData: Preferences): Boolean {
            // Don't migrate keys that have already been migrated.
            keySet.removeAll { key -> currentData.contains(key) }
            if (keySet.isEmpty()) {
                return false
            }

            return keySet.any(sharedPrefs::contains)
        }

        override suspend fun migrate(currentData: Preferences): Preferences {
            val preferencesToMigrate = sharedPrefs.all.filter { (key, _) -> key in keySet }

            val preferencesBuilder = currentData.toBuilder()
            for ((key, value) in preferencesToMigrate) {
                when (value) {
                    is Boolean -> preferencesBuilder.setBoolean(key, value)
                    is Float -> preferencesBuilder.setFloat(key, value)
                    is Int -> preferencesBuilder.setInt(key, value)
                    is Long -> preferencesBuilder.setLong(key, value)
                    is String -> preferencesBuilder.setString(key, value)
                    is Set<*> ->
                        @Suppress("UNCHECKED_CAST")
                        preferencesBuilder.setStringSet(key,
                            value.mapTo(mutableSetOf()) { it as String }
                        )
                }
            }

            return preferencesBuilder.build()
        }

        @Throws(IOException::class)
        override suspend fun cleanUp() {
            val sharedPrefsEditor = sharedPrefs.edit()

            for (key in keySet) {
                sharedPrefsEditor.remove(key)
            }

            if (!sharedPrefsEditor.commit()) {
                throw IOException(
                    "Unable to delete migrated keys from SharedPreferences: $sharedPreferencesName"
                )
            }

            if (deleteEmptyPreferences && sharedPrefs.all.isEmpty()) {
                deleteSharedPreferences(context, sharedPreferencesName)
            }

            keySet.clear()
        }

        private fun deleteSharedPreferences(context: Context, name: String) {
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                if (!context.deleteSharedPreferences(name)) {
                    throw IOException("Unable to delete SharedPreferences: $name")
                }
                return
            }

            // Context.deleteSharedPreferences is SDK 24+, so we have to reproduce the definition
            // here
            val prefsFile = getSharedPrefsFile(context, name)
            val prefsBackup = getSharedPrefsBackup(prefsFile)

            prefsFile.delete()
            prefsBackup.delete()

            // If either of the files still exist - we failed to delete.
            if (prefsFile.exists()) {
                throw IOException("Unable to delete shared preferences file: $prefsFile")
            }

            if (prefsBackup.exists()) {
                throw IOException("Unable to delete shared preferences backup files: $prefsBackup")
            }
        }

        private fun getSharedPrefsFile(context: Context, name: String): File {
            // ContextImpl.getSharedPreferencesPath is private, so we have to reproduce the
            // definition here
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            return File(prefsDir, "$name.xml")
        }

        private fun getSharedPrefsBackup(prefsFile: File): File {
            // SharedPreferencesImpl.makeBackupFile is private, so we have to reproduce the
            // definition here
            return File(prefsFile.path + ".bak")
        }
    }
}
