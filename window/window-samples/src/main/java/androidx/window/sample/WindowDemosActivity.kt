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

package androidx.window.sample

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.window.sample.BaseSampleActivity.Companion.BACKEND_TYPE_DEVICE_DEFAULT
import androidx.window.sample.BaseSampleActivity.Companion.BACKEND_TYPE_EXTRA
import androidx.window.sample.BaseSampleActivity.Companion.BACKEND_TYPE_MID_SCREEN_FOLD

/**
 * Main activity that launches WindowManager demos. Allows the user to choose the backend to use
 * with the [androidx.window.WindowManager] library interface, which can be helpful if the test
 * device does not report any display features.
 */
class WindowDemosActivity : AppCompatActivity() {
    private var selectedBackend = BACKEND_TYPE_DEVICE_DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_demos)

        val radioGroup = findViewById<RadioGroup>(R.id.backendRadioGroup)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.deviceDefaultRadioButton ->
                    selectedBackend = BACKEND_TYPE_DEVICE_DEFAULT
                R.id.midFoldRadioButton ->
                    selectedBackend = BACKEND_TYPE_MID_SCREEN_FOLD
            }
        }

        if (savedInstanceState != null) {
            selectedBackend = savedInstanceState.getInt(BACKEND_TYPE_EXTRA,
                BACKEND_TYPE_DEVICE_DEFAULT)
        }
        when (selectedBackend) {
            BACKEND_TYPE_DEVICE_DEFAULT ->
                radioGroup.check(R.id.deviceDefaultRadioButton)
            BACKEND_TYPE_MID_SCREEN_FOLD ->
                radioGroup.check(R.id.midFoldRadioButton)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BACKEND_TYPE_EXTRA, selectedBackend)
    }

    fun showDisplayFeatures(@Suppress("UNUSED_PARAMETER")view: View) {
        val intent = Intent(this, DisplayFeaturesActivity::class.java)
        intent.putExtra(BACKEND_TYPE_EXTRA, selectedBackend)
        startActivity(intent)
    }

    fun showSplitLayout(@Suppress("UNUSED_PARAMETER")view: View) {
        val intent = Intent(this, SplitLayoutActivity::class.java)
        intent.putExtra(BACKEND_TYPE_EXTRA, selectedBackend)
        startActivity(intent)
    }

    fun showPresentation(@Suppress("UNUSED_PARAMETER")view: View) {
        val intent = Intent(this, PresentationActivity::class.java)
        intent.putExtra(BACKEND_TYPE_EXTRA, selectedBackend)
        startActivity(intent)
    }
}