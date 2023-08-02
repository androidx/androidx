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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.TwoStatePreference
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

private val TAG = "SettingsActivity"

class SettingsFragmentActivity() : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, SettingsFragment()).commit()
    }
}

/**
 * Toggle States:
 * 1) Value not read from disk. Toggle is disabled in default position.
 * 2) Value read from disk and no pending updates. Toggle is enabled in latest persisted position.
 * 3) Value read from disk but with pending updates. Toggle is disabled in pending position.
 */
class SettingsFragment() : PreferenceFragmentCompat() {
    private val fooToggle: TwoStatePreference by lazy {
        createFooPreference(preferenceManager.context)
    }

    private val PROTO_STORE_FILE_NAME = "datastore_test_app.pb"

    private val settingsStore: DataStore<Settings> by lazy {
        DataStoreFactory.create(
            serializer = SettingsSerializer
        ) { File(requireActivity().applicationContext.filesDir, PROTO_STORE_FILE_NAME) }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val preferences = preferenceManager.createPreferenceScreen(preferenceManager.context)
        preferences.addPreference(fooToggle)
        preferenceScreen = preferences
    }

    @Suppress("OPT_IN_MARKER_ON_OVERRIDE_WARNING")
    @SuppressLint("SyntheticAccessor")
    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            // Read the initial value from disk
            val settings: Settings = try {
                settingsStore.data.first()
            } catch (ex: IOException) {
                Log.e(TAG, "Could not read settings.", ex)
                // Show error to user here, or try re-reading.
                return@launchWhenStarted
            }

            // Set the toggle to the value read from disk and enable the toggle.
            fooToggle.isChecked = settings.foo
            fooToggle.isEnabled = true

            fooToggle.changeFlow.flatMapLatest { (_: Preference?, newValue: Any?) ->
                val isChecked = newValue as Boolean

                fooToggle.isEnabled = false // Disable the toggle until the write is completed
                fooToggle.isChecked = isChecked // Set the disabled toggle to the pending value

                try {
                    settingsStore.setFoo(isChecked)
                } catch (ex: IOException) { // setFoo can only throw IOExceptions
                    Log.e(TAG, "Could not write settings", ex)
                    // Show error to user here
                }
                settingsStore.data // Switch to data flow since it is the source of truth.
            }.collect {
                // We update the toggle to the latest persisted value - whether or not the
                // update succeeded. If the write failed, this will reset to original state.
                fooToggle.isChecked = it.foo
                fooToggle.isEnabled = true
            }
        }
    }

    private suspend fun DataStore<Settings>.setFoo(foo: Boolean) = updateData {
        it.toBuilder().setFoo(foo).build()
    }

    private fun createFooPreference(context: Context) = SwitchPreference(context).apply {
        isEnabled = false // Start out disabled
        isPersistent = false // Disable SharedPreferences
        title = "Foo title"
        summary = "Summary of Foo toggle"
    }
}

@ExperimentalCoroutinesApi
private val Preference.changeFlow: Flow<Pair<Preference?, Any?>>
    get() = callbackFlow {
        this@changeFlow.setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
            this@callbackFlow.launch {
                send(Pair(preference, newValue))
            }
            false // Do not update the state of the toggle.
        }

        awaitClose { this@changeFlow.onPreferenceChangeListener = null }
    }

private object SettingsSerializer : Serializer<Settings> {
    override val defaultValue: Settings = Settings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Settings {
        try {
            return Settings.parseFrom(input)
        } catch (ipbe: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", ipbe)
        }
    }

    override suspend fun writeTo(t: Settings, output: OutputStream) = t.writeTo(output)
}