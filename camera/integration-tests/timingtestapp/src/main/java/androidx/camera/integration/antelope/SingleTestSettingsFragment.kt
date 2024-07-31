/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

/** Fragment that shows the settings for the "Single test" option */
class SingleTestSettingsFragment(
    internal val cameraNames: Array<String>,
    internal val cameraIds: Array<String>
) : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.single_test_settings, rootKey)

        val cameraPref =
            preferenceManager.findPreference<ListPreference>(
                getString(R.string.settings_single_test_camera_key)
            )
        cameraPref?.entries = cameraNames
        cameraPref?.entryValues = cameraIds
        if (cameraIds.isNotEmpty()) cameraPref?.setDefaultValue(cameraIds[0])

        if (null == cameraPref?.value) cameraPref?.value = cameraIds[0]

        // En/disable needed controls
        toggleNumTests()
        togglePreviewBuffer()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key.equals(getString(R.string.settings_single_test_type_key))) {
            toggleNumTests()
            togglePreviewBuffer()
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    /**
     * Some tests do not allow for multiple repetitions, If one of these selected, disable the
     * number of tests control.
     */
    fun toggleNumTests() {
        val typePref =
            preferenceManager.findPreference<ListPreference>(
                getString(R.string.settings_single_test_type_key)
            )
        val numberPref =
            preferenceManager.findPreference<ListPreference>(
                getString(R.string.settings_numtests_key)
            )
        when (typePref?.value) {
            "INIT",
            "PREVIEW",
            "SWITCH_CAMERA",
            "PHOTO" -> {
                numberPref?.isEnabled = false
            }
            else -> {
                numberPref?.isEnabled = true
            }
        }
    }

    /**
     * Some tests do not require the preview stream to run, If one of these selected, disable the
     * preview buffer control.
     */
    fun togglePreviewBuffer() {
        val typePref =
            preferenceManager.findPreference<ListPreference>(
                getString(R.string.settings_single_test_type_key)
            )
        val previewPref =
            preferenceManager.findPreference<ListPreference>(
                getString(R.string.settings_previewbuffer_key)
            )
        when (typePref?.value) {
            "INIT" -> {
                previewPref?.isEnabled = false
            }
            else -> {
                previewPref?.isEnabled = true
            }
        }
    }
}
