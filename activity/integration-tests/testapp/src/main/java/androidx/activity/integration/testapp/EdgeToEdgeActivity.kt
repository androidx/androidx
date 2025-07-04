/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.activity.integration.testapp

import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.DialogFragment

class EdgeToEdgeActivity : AppCompatActivity(R.layout.edge_to_edge_activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= 21) {
            installSplashScreen()
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        findViewById<View>(R.id.default_config).setOnClickListener {
            // The default style.
            // API 29+: Transparent on gesture nav, Auto scrim on 3-button nav (same as default).
            // API 26-28: Transparent status. Light or dark scrim on nav.
            // API 23-25: Transparent status. Dark scrim on nav.
            // API 21,22: Dark scrim (system default).
            enableEdgeToEdge()
        }
        findViewById<View>(R.id.auto_config).setOnClickListener {
            // API 29+: Transparent on gesture nav, Auto scrim on 3-button nav (same as default).
            // API 23-28: Yellow bars.
            // API 21,22: Dark scrim (system default).
            val style =
                SystemBarStyle.auto(
                    lightScrim = Color.argb(0x64, 0xff, 0xeb, 0x3b),
                    darkScrim = Color.argb(0x64, 0x4a, 0x14, 0x8c),
                )
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
        findViewById<View>(R.id.custom_config).setOnClickListener {
            // API 29+: Transparent on gesture nav, Auto scrim on 3-button nav (same as default).
            // API 23-28: Yellow bars.
            // API 21,22: Dark scrim (system default).
            val style =
                SystemBarStyle.auto(
                    lightScrim = Color.argb(0x64, 0xff, 0xeb, 0x3b),
                    darkScrim = Color.argb(0x64, 0x4a, 0x14, 0x8c),
                    detectDarkMode = { false },
                )
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
        findViewById<View>(R.id.transparent_config).setOnClickListener {
            // API 23+: Transparent regardless of the nav mode.
            // API 21,22: Dark scrim (system default).
            val style = SystemBarStyle.dark(Color.TRANSPARENT)
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
        findViewById<View>(R.id.purple_config).setOnClickListener {
            // API 23+: Purple.
            // API 21,22: Dark scrim (system default).
            val style = SystemBarStyle.dark(scrim = Color.argb(0x64, 0x4a, 0x14, 0x8c))
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
        findViewById<View>(R.id.yellow_config).setOnClickListener {
            // API 23+: Yellow.
            // API 21,22: Dark scrim (system default).
            val style =
                SystemBarStyle.light(
                    scrim = Color.argb(0x64, 0xff, 0xeb, 0x3b),
                    darkScrim = Color.rgb(0xf5, 0x7f, 0x17),
                )
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
        findViewById<View>(R.id.light_mode).setOnClickListener { setDarkMode(false) }
        findViewById<View>(R.id.dark_mode).setOnClickListener { setDarkMode(true) }
        findViewById<View>(R.id.show_dialog).setOnClickListener {
            EdgeToEdgeDialogFragment().show(supportFragmentManager, null)
        }
    }

    private fun setDarkMode(darkMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}

class EdgeToEdgeDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Demo Dialog")
            .setMessage("Hello, world!")
            .setPositiveButton(android.R.string.ok, { dialog, _ -> dialog.dismiss() })
            .create()
    }
}
