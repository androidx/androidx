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

import android.graphics.PixelFormat
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.camera.viewfinder.core.impl.RefCounted
import androidx.camera.viewfinder.core.impl.SurfaceControlCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope

private const val TAG = "VfExternalSurface"

private class ViewfinderExternalSurfaceHolder(
    initialSurface: Surface,
    width: Int,
    height: Int,
    initialParent: SurfaceControlCompat,
) : ViewfinderSurfaceHolder {
    private val surfaceControl: SurfaceControlCompat =
        SurfaceControlCompat.create(
            initialParent,
            width,
            height,
            "ViewfinderExternalSurfaceHolder-${hashCode()}",
        )

    override val refCountedSurface: RefCounted<Surface>

    var isDetached = false
        private set

    init {
        val surface = surfaceControl.newSurface() ?: initialSurface
        refCountedSurface = RefCounted {
            surfaceControl.detach()
            // Only release the surface if it's not the parent surface. i.e., it's a new surface
            // we've created from SurfaceControl.
            // On some API levels, SurfaceControlCompat is a no-op wrapper, and we don't have
            // control over the surface lifecycle and should leave it to the SurfaceView.
            if (surface != initialSurface) {
                surface.release()
            }
        }
        refCountedSurface.initialize(surface)
    }

    override fun detach() {
        if (!isDetached) {
            surfaceControl.detach()
            // Release for refCountedSurface.initialize()
            refCountedSurface.release()
            isDetached = true
        }
    }

    fun tryAttach(parent: SurfaceControlCompat): Boolean {
        check(isDetached) { "tryAttach() can only be called when detached" }
        return refCountedSurface.acquire()?.let {
            if (surfaceControl.reparent(parent)) {
                Log.d(TAG, "Reattached $it to $parent")
                isDetached = false
                true
            } else {
                // In this else-condition, it's likely the API level doesn't support SurfaceControl
                // Release the refcount we just acquired.
                Log.d(TAG, "Unable to attach $it to $parent")
                refCountedSurface.release()
                false
            }
        } == true
    }
}

/** Implementation of [BaseViewfinderExternalSurfaceState] for [ViewfinderExternalSurface]. */
private class ViewfinderExternalSurfaceState(scope: CoroutineScope) :
    BaseViewfinderExternalSurfaceState(scope), SurfaceHolder.Callback {

    var lastWidth = -1
    var lastHeight = -1
    lateinit var surfaceView: SurfaceView
    var viewfinderSurfaceHolder: ViewfinderExternalSurfaceHolder? = null

    fun initInternal(surfaceView: SurfaceView) {
        this.surfaceView = surfaceView
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val frame = holder.surfaceFrame
        lastWidth = frame.width()
        lastHeight = frame.height()

        val parent = SurfaceControlCompat.wrap(surfaceView)
        val oldSurfaceHolder = viewfinderSurfaceHolder
        if (oldSurfaceHolder == null || !oldSurfaceHolder.tryAttach(parent)) {
            val newSurfaceHolder =
                ViewfinderExternalSurfaceHolder(holder.surface, lastWidth, lastHeight, parent)
            viewfinderSurfaceHolder = newSurfaceHolder
            dispatchSurfaceCreated(newSurfaceHolder)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        lastWidth = width
        lastHeight = height
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        viewfinderSurfaceHolder?.detach()
    }
}

@Composable
private fun rememberViewfinderExternalSurfaceState(): ViewfinderExternalSurfaceState {
    val scope = rememberCoroutineScope()
    return remember { ViewfinderExternalSurfaceState(scope) }
}

/**
 * This is a modified version of the [androidx.compose.foundation.AndroidExternalSurface]
 * composable.
 *
 * It has been adapted to:
 * - Remove functionality not required by the Viewfinder, such as direct callbacks for
 *   `onSurfaceCreated` and `onSurfaceDestroyed` with raw [Surface] objects.
 * - Remove ZOrder API as it isn't used by the Viewfinder.
 * - Add specific functionality to support the Viewfinder's API guarantees. Notably, it ensures that
 *   underlying surface resources are not released prematurely, but rather managed in coordination
 *   with the [androidx.camera.viewfinder.core.ViewfinderSurfaceSessionScope] to guarantee the
 *   surface is valid until the session scope is completed (on supported API levels).
 */
@Composable
internal fun ViewfinderExternalSurface(
    modifier: Modifier = Modifier,
    isOpaque: Boolean = true,
    surfaceSize: IntSize = IntSize.Zero,
    isSecure: Boolean = false,
    onInit: ViewfinderExternalSurfaceScope.() -> Unit,
) {
    val state = rememberViewfinderExternalSurfaceState()

    AndroidView(
        factory = { context ->
            object : SurfaceView(context) {
                var attachedState: ViewfinderExternalSurfaceState? = null
                    set(value) {
                        if (value == null) {
                            field?.let { holder.removeCallback(it) }
                        } else {
                            holder.addCallback(value)
                        }
                        field = value
                    }
            }
        },
        modifier = modifier,
        onReset = {
            it.attachedState?.let { oldState ->
                // Ensure the old surface is detached
                oldState.viewfinderSurfaceHolder?.detach()
            }
            it.attachedState = null
        },
        update = { view ->
            if (view.attachedState !== state) {
                state.initInternal(view)
                view.attachedState = state
                state.onInit()
            }

            if (surfaceSize != IntSize.Zero) {
                view.holder.setFixedSize(surfaceSize.width, surfaceSize.height)
            } else {
                view.holder.setSizeFromLayout()
            }

            view.holder.setFormat(
                if (isOpaque) {
                    PixelFormat.OPAQUE
                } else {
                    PixelFormat.TRANSLUCENT
                }
            )

            view.setSecure(isSecure)
        },
    )
}
