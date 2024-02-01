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

@file:JvmName("PreferenceDataStoreFile")

package androidx.datastore.preferences

import android.content.Context
import androidx.datastore.dataStoreFile
import java.io.File

/**
 * Generate the File object for Preferences DataStore based on the provided context and name. The
 * file is in the [this.applicationContext.filesDir] + "datastore/" subdirectory with [name].
 * This is public to allow for testing and backwards compatibility (e.g. if moving from the
 * `preferencesDataStore` delegate or context.createDataStore to
 * PreferencesDataStoreFactory).
 *
 * Do NOT use the file outside of DataStore.
 *
 * @this the context of the application used to get the files directory
 * @name the name of the preferences
 */
public fun Context.preferencesDataStoreFile(name: String): File =
    this.dataStoreFile("$name.preferences_pb")
