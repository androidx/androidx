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

package androidx.preference.tests.helpers

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.test.R

/**
 * Helper activity that inflates a preference hierarchy defined in a given XML resource with a
 * [PreferenceFragmentCompat] to aid testing.
 */
class PreferenceTestHelperActivity : AppCompatActivity() {

    /**
     * Inflates the given XML resource and returns the created PreferenceFragmentCompat.
     *
     * @param preferenceLayoutId The XML resource ID to inflate. Defaults to an empty hierarchy.
     * @return The PreferenceFragmentCompat that contains the inflated hierarchy
     */
    fun setupPreferenceHierarchy(@LayoutRes preferenceLayoutId: Int = R.xml.test_selectable) =
        TestFragment(preferenceLayoutId).apply {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, this)
                .commitNow()
        }

    class TestFragment(private val preferenceLayoutId: Int) : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(preferenceLayoutId, rootKey)
        }
    }
}
