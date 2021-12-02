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

package androidx.window.sample.demos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.window.sample.DisplayFeaturesConfigChangeActivity
import androidx.window.sample.DisplayFeaturesNoConfigChangeActivity
import androidx.window.sample.PresentationActivity
import androidx.window.sample.R
import androidx.window.sample.R.string.display_features_config_change
import androidx.window.sample.R.string.display_features_no_config_change
import androidx.window.sample.R.string.show_all_display_features_config_change_description
import androidx.window.sample.R.string.show_all_display_features_no_config_change_description
import androidx.window.sample.SplitLayoutActivity
import androidx.window.sample.WindowMetricsActivity

/**
 * Main activity that launches WindowManager demos.
*/
class WindowDemosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_demos)
        val demoItems = listOf(
            DemoItem(
                buttonTitle = getString(display_features_config_change),
                description = getString(show_all_display_features_config_change_description),
                clazz = DisplayFeaturesConfigChangeActivity::class.java
            ),
            DemoItem(
                buttonTitle = getString(display_features_no_config_change),
                description = getString(show_all_display_features_no_config_change_description),
                clazz = DisplayFeaturesNoConfigChangeActivity::class.java
            ),
            DemoItem(
                buttonTitle = getString(R.string.window_metrics),
                description = getString(R.string.window_metrics_description),
                clazz = WindowMetricsActivity::class.java
            ),
            DemoItem(
                buttonTitle = getString(R.string.split_layout),
                description = getString(R.string.split_layout_demo_description),
                clazz = SplitLayoutActivity::class.java
            ),
            DemoItem(
                buttonTitle = getString(R.string.presentation),
                description = getString(R.string.presentation_demo_description),
                clazz = PresentationActivity::class.java
            )
        )
        val recyclerView = findViewById<RecyclerView>(R.id.demo_recycler_view)

        recyclerView.adapter = DemoAdapter(demoItems)
    }
}