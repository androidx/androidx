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

import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.annotation.Sampled
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.DataStore
import androidx.datastore.preferences.PreferenceDataStoreFactory
import androidx.datastore.preferences.Preferences
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PreferencesDataStoreActivity : AppCompatActivity() {
    private val TAG = "PreferencesActivity"

    private val PREFERENCE_STORE_FILE_NAME = "datastore_test_app.preferences_pb"
    private val COUNTER_KEY = "counter"

    private val preferenceStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory().create(
            { File(applicationContext.filesDir, PREFERENCE_STORE_FILE_NAME) })
    }

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
                preferenceStore.updateData { currentPrefs ->
                    val currentValue = currentPrefs.getInt(COUNTER_KEY, defaultValue = 0)
                    currentPrefs.toBuilder().setInt(COUNTER_KEY, currentValue - 1).build()
                }
            }
        }

        findViewById<Button>(R.id.counter_inc).setOnClickListener {
            lifecycleScope.launch {
                preferenceStore.updateData { currentPrefs ->
                    val currentValue = currentPrefs.getInt(COUNTER_KEY, defaultValue = 0)
                    currentPrefs.toBuilder().setInt(COUNTER_KEY, currentValue + 1).build()
                }
            }
        }

        lifecycleScope.launch {
            preferenceStore.data
                .catch { e ->
                    if (e is IOException) {
                        Log.e(TAG, "Error reading preferences.", e)
                        emit(Preferences.empty())
                    } else {
                        throw e
                    }
                }
                .map { it.getInt(COUNTER_KEY, defaultValue = 0) }
                .distinctUntilChanged()
                .collect { counterValue ->
                    findViewById<TextView>(R.id.counter_text_view).text = counterValue.toString()
                }
        }
    }
}