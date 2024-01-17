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

package androidx.graphics.opengl

import android.app.Activity
import android.os.Bundle
import android.view.SurfaceView
import android.view.TextureView
import android.widget.LinearLayout

class GLTestActivity : Activity() {

    companion object {
        const val TARGET_WIDTH = 30
        const val TARGET_HEIGHT = 20
    }

    lateinit var surfaceView: SurfaceView
    lateinit var textureView: TextureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surfaceView = SurfaceView(this)
        textureView = TextureView(this)
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            weightSum = 2f
        }
        val layoutParams = LinearLayout.LayoutParams(TARGET_WIDTH, TARGET_HEIGHT)

        ll.addView(surfaceView, layoutParams)
        ll.addView(textureView, layoutParams)

        setContentView(ll)
    }
}
