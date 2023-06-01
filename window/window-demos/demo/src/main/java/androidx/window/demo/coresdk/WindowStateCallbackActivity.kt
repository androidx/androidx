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

package androidx.window.demo.coresdk

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display.DEFAULT_DISPLAY
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.demo.databinding.ActivityCoresdkWindowStateCallbackLayoutBinding
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Activity to show display configuration from different system callbacks. */
class WindowStateCallbackActivity : AppCompatActivity() {

    private lateinit var displayManager: DisplayManager
    private lateinit var handler: Handler

    private lateinit var latestUpdateView: WindowStateView
    private lateinit var displayListenerView: WindowStateView
    private lateinit var activityConfigurationView: WindowStateView
    private lateinit var displayFeatureView: WindowStateView

    /**
     * Runnable to poll configuration every 500ms.
     * To always provide an up-to-date configuration so it can be used to verify the configurations
     * from other callbacks.
     */
    private val updateWindowStateIfChanged = object : Runnable {
        override fun run() {
            latestUpdateView.onWindowStateCallbackInvoked()
            handler.postDelayed(this, 500)
        }
    }

    private val displayListener = object : DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
        }

        override fun onDisplayRemoved(displayId: Int) {
        }

        override fun onDisplayChanged(displayId: Int) {
            if (displayId == DEFAULT_DISPLAY) {
                displayListenerView.onWindowStateCallbackInvoked()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewBinding = ActivityCoresdkWindowStateCallbackLayoutBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        latestUpdateView = viewBinding.latestUpdateView
        displayListenerView = viewBinding.displayListenerView
        activityConfigurationView = viewBinding.activityConfigurationView
        displayFeatureView = viewBinding.displayFeatureView

        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        handler = Handler(Looper.getMainLooper())
        displayManager.registerDisplayListener(displayListener, handler)
        // Collect windowInfo when STARTED and stop when STOPPED.
        lifecycleScope.launch(Dispatchers.Main) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@WindowStateCallbackActivity)
                    .windowLayoutInfo(this@WindowStateCallbackActivity)
                    .collect { _ ->
                        displayFeatureView.onWindowStateCallbackInvoked()
                    }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onResume() {
        super.onResume()
        // Start polling the configuration every 500ms.
        updateWindowStateIfChanged.run()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateWindowStateIfChanged)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        activityConfigurationView.onWindowStateCallbackInvoked()
    }
}