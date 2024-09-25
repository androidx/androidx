/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.window

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.core.view.WindowCompat

@Suppress("DEPRECATION") // Old ways of setting edge to edge are deprecated
class TestActivityEdgeToEdge : TestActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (Build.VERSION.SDK_INT >= 30) {
            windowInsetsController.hide(android.view.WindowInsets.Type.statusBars())
            windowInsetsController.hide(android.view.WindowInsets.Type.navigationBars())
            windowInsetsController.hide(android.view.WindowInsets.Type.displayCutout())
            windowInsetsController.hide(android.view.WindowInsets.Type.systemBars())
        }

        setContentView(androidx.window.test.R.layout.activity_edge_to_edge)

        val rootView = findViewById<View>(androidx.window.test.R.id.view_home).rootView
        rootView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN

        if (Build.VERSION.SDK_INT >= 28) {
            val params = LayoutParams()
            params.layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            window.attributes = params
        }
        this.actionBar?.hide()
    }
}
