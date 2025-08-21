/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.frontend.capture

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.SurfaceView
import android.widget.FrameLayout

/** Implement a secondary display view hierarchy, hosting a compose host */
class SecondaryDisplay(outerContext: Context?, display: Display?) :
    Presentation(outerContext, display) {

    val remoteLifecycleOwner = RemoteLifecycleOwner()
    public lateinit var resizeLayout: ResizableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resizeLayout = ResizableLayout(context)
        resizeLayout.layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        setContentView(resizeLayout)
        remoteLifecycleOwner.onCreate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        remoteLifecycleOwner.attachToDecorView(window?.decorView)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        remoteLifecycleOwner.onDestroy()
    }

    fun addView(surfaceView: SurfaceView) {
        resizeLayout.addView(surfaceView)
    }
}
