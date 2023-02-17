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

package androidx.wear.watchface.client.test

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.client.InteractiveWatchFaceClient
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.style.WatchFaceLayer
import java.time.Instant
import java.util.concurrent.CountDownLatch

/** Test fixture for testing renderWatchFaceToSurface. */
class SurfaceRenderingTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SurfaceRenderingView(this))
    }
}

class SurfaceRenderingView(context: Context) : TextureView(context) {
    var surface: Surface? = null

    companion object {
        var interactiveInstance: InteractiveWatchFaceClient? = null
        var complications: Map<Int, ComplicationData>? = null
        var view: SurfaceRenderingView? = null
        var renderDoneLatch: CountDownLatch? = null
    }

    @SuppressLint("NewApi") // renderWatchFaceToSurface
    fun renderToSurface() {
        interactiveInstance?.renderWatchFaceToSurface(
            RenderParameters(DrawMode.INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null),
            Instant.ofEpochMilli(1234567),
            null,
            complications!!,
            surface!!
        )
    }

    init {
        layoutParams = ViewGroup.LayoutParams(400, 400)

        view = this
        surfaceTextureListener = object : SurfaceTextureListener {
            @SuppressLint("NewApi")
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                surface = Surface(surfaceTexture)
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture) = true

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                renderDoneLatch!!.countDown()
            }
        }
    }
}