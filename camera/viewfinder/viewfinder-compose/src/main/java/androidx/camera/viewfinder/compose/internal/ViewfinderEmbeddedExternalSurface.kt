/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.viewfinder.compose.internal

import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.camera.viewfinder.core.impl.RefCounted
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope

private const val TAG = "VfEmbeddedSurface"

private class ViewfinderEmbeddedExternalSurfaceHolder(private val surfaceTexture: SurfaceTexture) :
    ViewfinderSurfaceHolder {
    override val refCountedSurface: RefCounted<Surface> = RefCounted {
        it.release()
        surfaceTexture.release()
    }

    var isDetached = false
        private set

    init {
        refCountedSurface.initialize(Surface(surfaceTexture))
    }

    override fun detach() {
        if (!isDetached) {
            refCountedSurface.release()
            isDetached = true
        }
    }

    fun tryAttach(textureView: TextureView) {
        if (isDetached) {
            refCountedSurface.acquire()?.let {
                textureView.setSurfaceTexture(surfaceTexture)
                Log.d(TAG, "Reattached $surfaceTexture to $textureView")
                isDetached = false
            }
                ?: run {
                    Log.d(
                        TAG,
                        "Unable to reattach $surfaceTexture to $textureView. Already released.",
                    )
                }
        } else {
            Log.d(TAG, "Unable to reattach $surfaceTexture to $textureView. Still attached.")
        }
    }
}

private class ViewfinderEmbeddedExternalSurfaceState(scope: CoroutineScope) :
    BaseViewfinderExternalSurfaceState(scope), TextureView.SurfaceTextureListener {

    var surfaceSize = IntSize.Zero
    val matrix = android.graphics.Matrix()

    var viewfinderSurfaceHolder: ViewfinderEmbeddedExternalSurfaceHolder? = null

    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        val newViewfinderSurfaceHolder = ViewfinderEmbeddedExternalSurfaceHolder(surfaceTexture)
        viewfinderSurfaceHolder = newViewfinderSurfaceHolder

        if (surfaceSize != IntSize.Zero) {
            surfaceTexture.setDefaultBufferSize(surfaceSize.width, surfaceSize.height)
        }

        dispatchSurfaceCreated(newViewfinderSurfaceHolder)
    }

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        if (surfaceSize != IntSize.Zero) {
            surfaceTexture.setDefaultBufferSize(surfaceSize.width, surfaceSize.height)
        }
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        viewfinderSurfaceHolder?.detach()
        // If the composable hasn't yet been disposed, this surface could be reattached, so we won't
        // stop the surface job here.

        // Do not release the SurfaceTexture. It will be released by the refCountedSurface when
        // the ref count reaches zero.
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // onSurfaceTextureUpdated is called when the content of the SurfaceTexture
        // has changed, which is not relevant to us since we are the producer here
    }

    fun tryReattachViewfinderSurfaceHolder(textureView: TextureView) {
        viewfinderSurfaceHolder?.tryAttach(textureView)
    }
}

@Composable
private fun rememberViewfinderEmbeddedExternalSurfaceState():
    ViewfinderEmbeddedExternalSurfaceState {
    val scope = rememberCoroutineScope()
    return remember { ViewfinderEmbeddedExternalSurfaceState(scope) }
}

/**
 * This is a modified version of the [androidx.compose.foundation.AndroidEmbeddedExternalSurface]
 * composable.
 *
 * It has been adapted to:
 * - Remove functionality not required by the Viewfinder, such as direct callbacks for
 *   `onSurfaceCreated` and `onSurfaceDestroyed` with raw [Surface] objects.
 * - Add specific functionality to support the Viewfinder's API guarantees. Notably, it ensures that
 *   underlying surface resources are not released prematurely, but rather managed in coordination
 *   with the [androidx.camera.viewfinder.core.ViewfinderSurfaceSessionScope] to guarantee the
 *   surface is valid until the session scope is completed.
 */
@Composable
internal fun ViewfinderEmbeddedExternalSurface(
    modifier: Modifier = Modifier,
    isOpaque: Boolean = true,
    surfaceSize: IntSize = IntSize.Zero,
    transform: Matrix? = null,
    onInit: ViewfinderExternalSurfaceScope.() -> Unit,
) {
    val state = rememberViewfinderEmbeddedExternalSurfaceState()

    AndroidView(
        factory = {
            object : TextureView(it) {
                override fun onAttachedToWindow() {
                    super.onAttachedToWindow()
                    (surfaceTextureListener as? ViewfinderEmbeddedExternalSurfaceState)
                        ?.tryReattachViewfinderSurfaceHolder(this)
                }
            }
        },
        modifier = modifier,
        onReset = { view ->
            (view.surfaceTextureListener as? ViewfinderEmbeddedExternalSurfaceState)?.let { oldState
                ->
                // Ensure the old surface is detached
                state.viewfinderSurfaceHolder?.detach()
            }
            view.surfaceTextureListener = null
        },
        update = { view ->
            if (surfaceSize != IntSize.Zero) {
                view.surfaceTexture?.setDefaultBufferSize(surfaceSize.width, surfaceSize.height)
            }
            state.surfaceSize = surfaceSize
            if (view.surfaceTextureListener !== state) {
                state.onInit()
                view.surfaceTextureListener = state
            }

            view.isOpaque = isOpaque
            // If transform is null, we'll call setTransform(null) which sets the
            // identity transform on the TextureView
            view.setTransform(transform?.let { state.matrix.apply { setFrom(transform) } })
        },
    )
}
