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

package androidx.camera.integration.uiwidgets.rotations

import android.content.Context
import android.hardware.display.DisplayManager

class OrientationConfigChangesOverriddenActivity : CameraActivity() {

    private val mDisplayManager by lazy {
        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    private val mDisplayListener by lazy {
        object : DisplayManager.DisplayListener {
            override fun onDisplayChanged(displayId: Int) {
                val display = mDisplayManager.getDisplay(displayId)
                if (display != null) {
                    val rotation = display.rotation
                    mImageAnalysis.targetRotation = rotation
                    mImageCapture.targetRotation = rotation
                }
            }

            override fun onDisplayAdded(displayId: Int) {
            }

            override fun onDisplayRemoved(displayId: Int) {
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mDisplayManager.registerDisplayListener(mDisplayListener, null)
    }

    override fun onStop() {
        super.onStop()
        mDisplayManager.unregisterDisplayListener(mDisplayListener)
    }
}
