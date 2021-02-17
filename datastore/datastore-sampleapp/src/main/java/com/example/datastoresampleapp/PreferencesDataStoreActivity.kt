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

package com.example.datastoresampleapp

import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.annotation.Sampled
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

val Context.prefsDs by preferencesDataStore("datastore_test_app")

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PreferencesDataStoreActivity : AppCompatActivity() {
    private val TAG = "PreferencesActivity"

    private val PREFERENCE_STORE_FILE_NAME = "datastore_test_app"
    private val COUNTER_KEY = intPreferencesKey("counter")

    @Sampled
    override fun onCreate(savedInstanceState: Bundle?) {
        // Strict mode allows us to check that no writes or reads are blocking the UI thread.
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .penaltyDeath()
                .build()
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpPreferenceStoreUi()
    }

    @Sampled
    private fun setUpPreferenceStoreUi() {
        // Using preferenceStore:
        findViewById<Button>(R.id.counter_dec).setOnClickListener {
            lifecycleScope.launch {
                prefsDs.edit { prefs ->
                    prefs[COUNTER_KEY] = (prefs[COUNTER_KEY] ?: 0) - 1
                }
            }
        }

        findViewById<Button>(R.id.counter_inc).setOnClickListener {
            lifecycleScope.launch {
                prefsDs.edit { prefs ->
                    prefs[COUNTER_KEY] = (prefs[COUNTER_KEY] ?: 0) + 1
                }
            }
        }

        lifecycleScope.launch {
            prefsDs.data
                .catch { e ->
                    if (e is IOException) {
                        Log.e(TAG, "Error reading preferences.", e)
                        emit(emptyPreferences())
                    } else {
                        throw e
                    }
                }
                .map { it[COUNTER_KEY] ?: 0 }
                .distinctUntilChanged()
                .collect { counterValue ->
                    findViewById<TextView>(R.id.counter_text_view).text = counterValue.toString()
                }
        }
    }
}