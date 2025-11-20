/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.testapp.activitypanel

import android.os.Bundle
import android.widget.TextClock
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.xr.scenecore.testapp.R
import com.google.android.material.appbar.MaterialToolbar

class ActivityPanel : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panel)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.activity_panel_tool_bar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { this.finish() }

        findViewById<TextClock>(R.id.digitalClock).format12Hour = "hh:mm:ss"

        if (intent.extras != null) {
            val toolbarTitle = intent.extras!!.getString("PANEL_NAME", "").toString()
            if (toolbarTitle != "") toolbar.setTitle(toolbarTitle)
            val navigationIcon = intent.extras!!.getBoolean("NAV_ICON", true)
            if (!navigationIcon) {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            }
        }
    }
}
