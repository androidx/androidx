/*
 * Copyright 2026 The Android Open Source Project
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

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.camera.viewfinder.core.impl.RefCounted
import androidx.camera.viewfinder.core.impl.SurfaceControlCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope

private const val TAG = "VfExternalSurface"

private class ViewfinderExternalSurfaceHolder(
    initialSurface: Surface,
    private var width: Int,
    private var height: Int,
    format: Int,
    initialParent: SurfaceControlCompat,
) : ViewfinderSurfaceHolder {
    // Intermediate layer between the SurfaceView and the layer receiving the frames. Rotating the
    // content is done by transforming the geometry of this layer, since the buffer transform of the
    // frame layer is controlled by the frame producer.
    private val rotationSurfaceControl: SurfaceControlCompat =
        SurfaceControlCompat.create(
            initialParent,
            width,
            height,
            "ViewfinderExternalSurfaceHolder-Rotation-${hashCode()}",
            format,
        )

    private val surfaceControl: SurfaceControlCompat =
        SurfaceControlCompat.create(
            rotationSurfaceControl,
            width,
            height,
            "ViewfinderExternalSurfaceHolder-${hashCode()}",
            format,
        )

    override val refCountedSurface: RefCounted<Surface>

    var isDetached = false
        private set

    private var contentRotationDegrees = 0

    init {
        val surface = surfaceControl.newSurface() ?: initialSurface
        refCountedSurface = RefCounted {
            rotationSurfaceControl.detach()
            // Only release the surface if it's not the parent surface. i.e., it's a new surface
            // we've created from SurfaceControl.
            // On some API levels, SurfaceControlCompat is a no-op wrapper, and we don't have
            // control over the surface lifecycle and should leave it to the SurfaceView.
            if (surface != initialSurface) {
                surface.release()
            }
            surfaceControl.release()
            rotationSurfaceControl.release()
        }
        refCountedSurface.initialize(surface)
    }

    override fun detach() {
        if (!isDetached) {
            rotationSurfaceControl.detach()
            // Release for refCountedSurface.initialize()
            refCountedSurface.release()
            isDetached = true
        }
    }

    fun tryAttach(parent: SurfaceControlCompat): Boolean {
        check(isDetached) { "tryAttach() can only be called when detached" }
        return refCountedSurface.acquire()?.let {
            if (rotationSurfaceControl.reparent(parent)) {
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

    /**
     * Updates the buffer size and content rotation of the underlying [SurfaceControlCompat] layers,
     * re-applying geometry if either the dimensions or rotation changed while attached. The content
     * is rotated clockwise by [rotationDegrees] and scaled to fit the bounds of the surface. This
     * is a no-op on API levels where [SurfaceControlCompat] is not supported.
     */
    fun updateSizeAndRotation(width: Int, height: Int, rotationDegrees: Int, syncView: View?) {
        val sizeChanged = width != this.width || height != this.height
        if (sizeChanged) {
            this.width = width
            this.height = height
            if (width > 0 && height > 0) {
                rotationSurfaceControl.setBufferSize(width, height)
                surfaceControl.setBufferSize(width, height)
            }
        }
        if (isDetached || width <= 0 || height <= 0) return
        if (sizeChanged || rotationDegrees != contentRotationDegrees) {
            if (
                rotationSurfaceControl.setContentRotation(rotationDegrees, width, height, syncView)
            ) {
                contentRotationDegrees = rotationDegrees
            }
        }
    }
}

/**
 * [SurfaceView] host for [ViewfinderExternalSurface] that tracks its bound
 * [ViewfinderExternalSurfaceState] and whether it has been abandoned on API 29-30 after surface
 * destruction.
 */
private class ViewfinderSurfaceView(context: Context) : SurfaceView(context) {
    var isAbandoned = false

    var attachedState: ViewfinderExternalSurfaceState? = null
        set(value) {
            if (field !== value) {
                field?.let { holder.removeCallback(it) }
                value?.let { holder.addCallback(it) }
                field = value
            }
        }
}

/** Implementation of [BaseViewfinderExternalSurfaceState] for [ViewfinderExternalSurface]. */
private class ViewfinderExternalSurfaceState(
    scope: CoroutineScope,
    private val onRecreateSurfaceView: () -> Unit,
) : BaseViewfinderExternalSurfaceState(scope), SurfaceHolder.Callback {

    var lastWidth = -1
    var lastHeight = -1
    var surfaceFormat = PixelFormat.OPAQUE
    var surfaceView: ViewfinderSurfaceView? = null
    var viewfinderSurfaceHolder: ViewfinderExternalSurfaceHolder? = null
    var isInitialized = false

    /** The clockwise rotation in degrees to apply to the content of the surface. */
    var contentRotationDegrees = 0
        private set

    fun initInternal(surfaceView: ViewfinderSurfaceView) {
        this.surfaceView = surfaceView
    }

    /**
     * Sets the rotation to apply to the content of the current and any future surface.
     *
     * The change is synchronized with the next frame drawn by the [surfaceView]'s window, where
     * supported.
     */
    fun updateContentRotation(rotationDegrees: Int) {
        contentRotationDegrees = rotationDegrees
        viewfinderSurfaceHolder?.updateSizeAndRotation(
            lastWidth,
            lastHeight,
            rotationDegrees,
            surfaceView,
        )
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val currentSurfaceView =
            checkNotNull(surfaceView) { "surfaceView must be initialized before surfaceCreated" }
        val frame = holder.surfaceFrame
        lastWidth = frame.width()
        lastHeight = frame.height()

        val parent = SurfaceControlCompat.wrap(currentSurfaceView)
        val oldSurfaceHolder = viewfinderSurfaceHolder
        if (oldSurfaceHolder == null || !oldSurfaceHolder.tryAttach(parent)) {
            val newSurfaceHolder =
                ViewfinderExternalSurfaceHolder(
                    holder.surface,
                    lastWidth,
                    lastHeight,
                    surfaceFormat,
                    parent,
                )
            // Apply the rotation immediately, before any frame is produced to the new surface.
            newSurfaceHolder.updateSizeAndRotation(
                lastWidth,
                lastHeight,
                contentRotationDegrees,
                syncView = null,
            )
            viewfinderSurfaceHolder = newSurfaceHolder
            dispatchSurfaceCreated(newSurfaceHolder)
        } else {
            oldSurfaceHolder.updateSizeAndRotation(
                lastWidth,
                lastHeight,
                contentRotationDegrees,
                currentSurfaceView,
            )
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        lastWidth = width
        lastHeight = height
        // Don't adopt the format reported here: SurfaceView reports PixelFormat.OPAQUE as
        // RGB_565, while surfaceFormat must keep the format requested in AndroidView.update.
        viewfinderSurfaceHolder?.updateSizeAndRotation(
            width,
            height,
            contentRotationDegrees,
            surfaceView,
        )
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        viewfinderSurfaceHolder?.detach()
        // On API 29-30, if a SurfaceView is detached and reattached within the same frame at the
        // same window bounds (e.g. via movableContentOf), SurfaceView recreates its SurfaceControl
        // with un-scaled UI-thread geometry without resetting its cached render position, causing
        // PositionUpdateListener.positionChanged to early-return and leave the new SurfaceControl
        // un-scaled (and the previous SurfaceControl not released until positionLost). Abandon
        // this SurfaceView and schedule a fresh instance; API 31+ replaces the
        // PositionUpdateListener whenever it recreates its SurfaceControl.
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.Q..Build.VERSION_CODES.R) {
            surfaceView?.let { view ->
                if (view.holder === holder) {
                    view.isAbandoned = true
                    view.attachedState = null
                    surfaceView = null
                    onRecreateSurfaceView()
                }
            }
        }
    }
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
 * - Allow the content of the surface to be rotated via [contentRotationDegrees] (on supported API
 *   levels). Since a [SurfaceView] cannot be rotated by View transformations, the rotation is
 *   applied to the [android.view.SurfaceControl] hierarchy owned by this composable. The rotated
 *   content is scaled to fit the original bounds of the surface.
 */
@Composable
internal fun ViewfinderExternalSurface(
    modifier: Modifier = Modifier,
    isOpaque: Boolean = true,
    surfaceSize: IntSize = IntSize.Zero,
    isSecure: Boolean = false,
    contentRotationDegrees: Int = 0,
    onInit: ViewfinderExternalSurfaceScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    var surfaceViewGeneration by remember { mutableIntStateOf(0) }
    val state = remember { ViewfinderExternalSurfaceState(scope) { surfaceViewGeneration++ } }

    key(surfaceViewGeneration) {
        AndroidView(
            factory = { context -> ViewfinderSurfaceView(context) },
            modifier = modifier,
            onReset = {
                it.attachedState?.let { oldState ->
                    // Ensure the old surface is detached
                    oldState.viewfinderSurfaceHolder?.detach()
                    if (oldState.surfaceView === it) {
                        oldState.surfaceView = null
                    }
                }
                it.attachedState = null
                it.isAbandoned = false
            },
            update = { view ->
                if (view.isAbandoned) {
                    return@AndroidView
                }
                if (view.attachedState !== state) {
                    state.initInternal(view)
                    view.attachedState = state
                    if (!state.isInitialized) {
                        state.isInitialized = true
                        state.onInit()
                    }
                }

                if (surfaceSize != IntSize.Zero) {
                    view.holder.setFixedSize(surfaceSize.width, surfaceSize.height)
                } else {
                    view.holder.setSizeFromLayout()
                }

                val surfaceFormat =
                    if (isOpaque) {
                        PixelFormat.OPAQUE
                    } else {
                        PixelFormat.TRANSLUCENT
                    }
                state.surfaceFormat = surfaceFormat
                view.holder.setFormat(surfaceFormat)

                view.setSecure(isSecure)

                state.updateContentRotation(contentRotationDegrees)
            },
        )
    }
}
