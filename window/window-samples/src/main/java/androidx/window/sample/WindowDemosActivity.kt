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
import androidx.appcompat.app.AppCompatActivity

/**
 * Main activity that launches WindowManager demos.
*/
class WindowDemosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_demos)
    }

    fun showDisplayFeatures(@Suppress("UNUSED_PARAMETER")view: View) {
        val intent = Intent(this, DisplayFeaturesActivity::class.java)
        startActivity(intent)
    }

    fun showSplitLayout(@Suppress("UNUSED_PARAMETER")view: View) {
        val intent = Intent(this, SplitLayoutActivity::class.java)
        startActivity(intent)
    }

    fun showPresentation(@Suppress("UNUSED_PARAMETER")view: View) {
        val intent = Intent(this, PresentationActivity::class.java)
        startActivity(intent)
    }
}