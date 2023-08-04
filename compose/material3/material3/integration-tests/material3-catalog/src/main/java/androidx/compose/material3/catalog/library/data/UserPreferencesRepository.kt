/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.catalog.library.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(private val context: Context) {
    private companion object {
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore("user_preferences")
        val FAVORITE_ROUTE = stringPreferencesKey("favorite_route")
    }

    suspend fun saveFavoriteRoute(favoriteRoute: String?) {
        context.dataStore.edit { preferences ->
            if (favoriteRoute == null) {
                preferences.remove(FAVORITE_ROUTE)
            } else {
                preferences[FAVORITE_ROUTE] = favoriteRoute
            }
        }
    }

    suspend fun getFavoriteRoute(): String? = context.dataStore.data
        .map { preferences -> preferences[FAVORITE_ROUTE] }
        .first()
}
