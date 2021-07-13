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

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.Display.DEFAULT_DISPLAY
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.FoldingFeature
import androidx.window.WindowInfoRepo
import androidx.window.WindowLayoutInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Demo activity that reacts to foldable device state change and shows content on the outside
 * display when the device is folded.
 */
class PresentationActivity : AppCompatActivity() {
    private val TAG = "FoldablePresentation"

    private lateinit var windowInfoRepo: WindowInfoRepo
    private var presentation: DemoPresentation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_foldin)

        windowInfoRepo = WindowInfoRepo.create(this)

        lifecycleScope.launch(Dispatchers.Main) {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Safely collect from windowInfoRepo when the lifecycle is STARTED
                // and stops collection when the lifecycle is STOPPED
                windowInfoRepo.windowLayoutInfo
                    .collect { newLayoutInfo ->
                        // New posture information
                        updateCurrentState(newLayoutInfo)
                    }
            }
        }
    }

    internal fun startPresentation(context: Context) {
        if (presentation != null) {
            val message = "Trying to show presentation that's already showing"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            Log.w(TAG, message)
            return
        }

        // Look for a built-in display on the outer side of the foldable device to show our
        // Presentation. Unfortunately, there are no direct methods to get the right one, but it's
        // possible to make a pretty good guess by filtering out obviously invalid candidates.
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val presentationDisplays = displayManager.displays
        for (display in presentationDisplays) {
            if (display.displayId == DEFAULT_DISPLAY) {
                // This is the primary device display, but we're looking for a secondary one
                continue
            }
            if (display.flags and Display.FLAG_PRESENTATION != Display.FLAG_PRESENTATION) {
                // This display doesn't support showing Presentation
                continue
            }
            if (display.flags and Display.FLAG_PRIVATE != 0) {
                // Valid system-owned displays will be public
                continue
            }
            if (display.state != Display.STATE_ON) {
                // This display is not ready to show app content right now
                continue
            }

            // This display seems like a good match!
            presentation = DemoPresentation(context, display)
            // Make sure that the window is marked to show on top of lock screen, since we're
            // targeting  the screen on the other side when the device may be closed/locked.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                presentation!!.window?.attributes?.flags =
                    presentation!!.window?.attributes?.flags?.or(
                        android.R.attr.showWhenLocked or android.R.attr.turnScreenOn
                    )
            }
            presentation!!.show()
            break
        }

        if (presentation == null) {
            val message = "No matching display found, Presentation not shown"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            Log.w(TAG, message)
        }
    }

    /**
     * Method to be called from layout XML definition.
     */
    fun startPresentation(view: View) {
        startPresentation(view.context)
    }

    @Suppress("UNUSED_PARAMETER") // Callback defined in xml
    fun stopPresentation(view: View?) {
        presentation?.hide()
        presentation = null
    }

    /**
     * The presentation to show on the secondary display.
     */
    private class DemoPresentation(context: Context?, display: Display?) :
        Presentation(context, display) {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.presentation_second_display)
        }
    }

    /**
     * Updates the display of the current fold feature state.
     */
    internal fun updateCurrentState(info: WindowLayoutInfo) {
        val stateStringBuilder = StringBuilder()

        stateStringBuilder.append(getString(R.string.deviceState))
            .append(": ")

        info.displayFeatures
            .mapNotNull { it as? FoldingFeature }
            .joinToString(separator = "\n") { feature -> feature.state.toString() }

        findViewById<TextView>(R.id.currentState).text = stateStringBuilder.toString()
    }
}
