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

package androidx.graphics

import android.graphics.HardwareRenderer
import android.graphics.RenderNode
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi

/**
 * Class that handles drawing content of a RenderNode into a SurfaceTexture
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class SurfaceTextureRenderer(
    /**
     * Target RenderNode of the content that is to be drawn
     */
    private val renderNode: RenderNode,

    /**
     * Width of the SurfaceTexture
     */
    width: Int,

    /**
     * Height of the SurfaceTexture
     */
    height: Int,

    /**
     * Handler used to send SurfaceTexture#OnFrameAvailableListener callbacks
     */
    private val handler: Handler,

    /**
     * Callback invoked when a new image frame is available on the underlying SurfaceTexture
     */
    private val frameAvailable: (SurfaceTexture) -> Unit
) {

    // Workaround: b/272751501
    // For some reason, SurfaceTexture instances that are created in detached mode get gc'ed
    // prematurely after the application is idle for ~10 or more seconds. This issue appears in
    // multiple versions of Android. However, if we use a subclass of SurfaceTexture we seem to
    // not run into this. It appears that there is some internal package name checking of
    // android.graphics.SurfaceTexture API within jni in the platform and using a subclass seems
    // to prevent work around this issue.
    //
    // An alternative solution is to create a SurfaceTexture in attached mode by providing a
    // placeholder texture identifier then having the consuming GLThread call detachFromGLContext
    // and attachToGLContext with a freshly created texture id.
    //
    // Currently we go with the original option as it may not be explicit to implementations that
    // the initial detach is necessary here.
    private class RenderSurfaceTexture(singleBufferMode: Boolean) : SurfaceTexture(singleBufferMode)

    private var mIsReleased = false

    private val mSurfaceTexture = RenderSurfaceTexture(false).apply {
        setDefaultBufferSize(width, height)
        setOnFrameAvailableListener({ surfaceTexture -> frameAvailable(surfaceTexture) }, handler)
    }

    private val mTextureSurface = Surface(mSurfaceTexture)
    private val mHardwareRenderer = HardwareRenderer().apply {
        setSurface(mTextureSurface)
        setContentRoot(renderNode)
        start()
    }

    fun renderFrame() {
        if (!mIsReleased) {
            mHardwareRenderer.apply {
                createRenderRequest()
                    .setWaitForPresent(false)
                    .syncAndDraw()
            }
        } else {
            Log.w(
                TAG, "Attempt to renderFrame when SurfaceTextureRenderer has already " +
                "been released")
        }
    }

    /**
     * Releases all resources of the SurfaceTextureRenderer instances. Attempts to use this
     * object after this call has been made will be ignored.
     */
    fun release() {
        if (!mIsReleased) {
            mHardwareRenderer.stop()
            mHardwareRenderer.destroy()
            mTextureSurface.release()
            if (!mSurfaceTexture.isReleased) {
                mSurfaceTexture.release()
            }
            mIsReleased = true
        } else {
            Log.w(
                TAG, "Attempt to release a SurfaceTextureRenderer that has " +
                "already been released")
        }
    }

    companion object {
        private val TAG = "SurfaceTextureRenderer"
    }
}
