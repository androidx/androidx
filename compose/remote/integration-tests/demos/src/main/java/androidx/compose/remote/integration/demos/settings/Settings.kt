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

package androidx.compose.remote.integration.demos.settings

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

const val SETTINGS_NAME = "settings_name"

const val LAYOUT_DIRECTION_KEY = "layout_direction_key"
const val LAYOUT_DIRECTION_LTR = 0
const val LAYOUT_DIRECTION_RTL = 1

val Context.dataStore by preferencesDataStore(name = SETTINGS_NAME)

val LAYOUT_DIRECTION_PREF_KEY = intPreferencesKey(LAYOUT_DIRECTION_KEY)
