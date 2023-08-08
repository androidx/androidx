/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.graphics.lowlatency

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.S)
class SampleInkViewActivity : Activity() {

    private var inkView: View? = null
    private var toggle: Button? = null
    private var container: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addInkViews()
    }

    private fun addInkViews() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            toggle = Button(this).apply {
                layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    gravity = Gravity.BOTTOM or Gravity.RIGHT
                }
                setOnClickListener {
                    toggleLowLatencyView()
                }
            }
            container = FrameLayout(this).apply {
                addView(toggle)
                setBackgroundColor(Color.BLACK)
            }
            toggleLowLatencyView()
            setContentView(container)
        }
    }

    private fun toggleLowLatencyView() {
        inkView?.let { view -> container?.removeView(view) }
        if (inkView == null || inkView is InkSurfaceView) {
            inkView = InkCanvasView(this)
            toggle?.text = "Canvas"
        } else if (inkView is InkCanvasView) {
            inkView = InkSurfaceView(this)
            toggle?.text = "OpenGL"
        }
        container?.addView(inkView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        container?.bringChildToFront(toggle)
    }
}
