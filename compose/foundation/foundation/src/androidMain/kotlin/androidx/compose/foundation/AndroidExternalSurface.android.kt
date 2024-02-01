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

package androidx.compose.foundation

import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * [SurfaceScope] is a scoped environment provided by [AndroidExternalSurface] and
 * [AndroidEmbeddedExternalSurface] to handle [Surface] lifecycle events.
 *
 * @sample androidx.compose.foundation.samples.AndroidExternalSurfaceColors
 */
interface SurfaceScope {
    /**
     * Invokes [onChanged] when the surface's geometry (width and height) changes.
     * Always invoked on the main thread.
     */
    fun Surface.onChanged(onChanged: Surface.(width: Int, height: Int) -> Unit)

    /**
     * Invokes [onDestroyed] when the surface is destroyed. All rendering into
     * the surface should stop immediately after [onDestroyed] is invoked.
     * Always invoked on the main thread.
     */
    fun Surface.onDestroyed(onDestroyed: Surface.() -> Unit)
}

/**
 * [SurfaceCoroutineScope] is a scoped environment provided by
 * [AndroidExternalSurface] and [AndroidEmbeddedExternalSurface] when a new [Surface] is
 * created. This environment is a coroutine scope that also provides access to
 * a [SurfaceScope] environment which can itself be used to handle other [Surface]
 * lifecycle events.
 *
 * @see SurfaceScope
 * @see AndroidExternalSurfaceScope
 *
 * @sample androidx.compose.foundation.samples.AndroidExternalSurfaceColors
 */
interface SurfaceCoroutineScope : SurfaceScope, CoroutineScope

/**
 * [AndroidExternalSurfaceScope] is a scoped environment provided when an
 * [AndroidExternalSurface] or [AndroidEmbeddedExternalSurface] is first initialized.
 * This environment can be used to register a lambda to invoke when a new [Surface]
 * associated with the [AndroidExternalSurface]/[AndroidEmbeddedExternalSurface]
 * is created.
 */
interface AndroidExternalSurfaceScope {
    /**
     * Invokes [onSurface] when a new [Surface] is created. The [onSurface] lambda
     * is invoked on the main thread as part of a [SurfaceCoroutineScope] to provide
     * a coroutine context. Always invoked on the main thread.
     *
     * @param onSurface Callback invoked when a new [Surface] is created. The initial
     *                  dimensions of the surface are provided.
     */
    fun onSurface(
        onSurface: suspend SurfaceCoroutineScope.(surface: Surface, width: Int, height: Int) -> Unit
    )
}

/**
 * Base class for [AndroidExternalSurface] and [AndroidEmbeddedExternalSurface] state. This class
 * provides methods to properly dispatch lifecycle events on [Surface] creation,
 * change, and destruction. Surface creation is treated as a coroutine launch,
 * using the specified [scope] as the parent. This scope must be the main thread scope.
 */
private abstract class BaseAndroidExternalSurfaceState(val scope: CoroutineScope) :
    AndroidExternalSurfaceScope, SurfaceScope {

    private var onSurface:
        (suspend SurfaceCoroutineScope.(surface: Surface, width: Int, height: Int) -> Unit)? = null
    private var onSurfaceChanged: (Surface.(width: Int, height: Int) -> Unit)? = null
    private var onSurfaceDestroyed: (Surface.() -> Unit)? = null

    private var job: Job? = null

    override fun onSurface(
        onSurface: suspend SurfaceCoroutineScope.(surface: Surface, width: Int, height: Int) -> Unit
    ) {
        this.onSurface = onSurface
    }

    override fun Surface.onChanged(onChanged: Surface.(width: Int, height: Int) -> Unit) {
        onSurfaceChanged = onChanged
    }

    override fun Surface.onDestroyed(onDestroyed: Surface.() -> Unit) {
        onSurfaceDestroyed = onDestroyed
    }

    /**
     * Dispatch a surface creation event by launching a new coroutine in [scope].
     * Any previous job from a previous surface creation dispatch is cancelled.
     */
    fun dispatchSurfaceCreated(surface: Surface, width: Int, height: Int) {
        if (onSurface != null) {
            job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                job?.cancelAndJoin()
                val receiver =
                    object : SurfaceCoroutineScope,
                        SurfaceScope by this@BaseAndroidExternalSurfaceState,
                        CoroutineScope by this {}
                onSurface?.invoke(receiver, surface, width, height)
            }
        }
    }

    /**
     * Dispatch a surface change event, providing the surface's new width and height.
     * Must be invoked from the main thread.
     */
    fun dispatchSurfaceChanged(surface: Surface, width: Int, height: Int) {
        onSurfaceChanged?.invoke(surface, width, height)
    }

    /**
     * Dispatch a surface destruction event. Any pending job from [dispatchSurfaceCreated]
     * is cancelled before dispatching the event. Must be invoked from the main thread.
     */
    fun dispatchSurfaceDestroyed(surface: Surface) {
        onSurfaceDestroyed?.invoke(surface)
        job?.cancel()
        job = null
    }
}

private class AndroidExternalSurfaceState(scope: CoroutineScope) :
    BaseAndroidExternalSurfaceState(scope), SurfaceHolder.Callback {

    var lastWidth = -1
    var lastHeight = -1

    override fun surfaceCreated(holder: SurfaceHolder) {
        val frame = holder.surfaceFrame
        lastWidth = frame.width()
        lastHeight = frame.height()

        dispatchSurfaceCreated(holder.surface, lastWidth, lastHeight)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (lastWidth != width || lastHeight != height) {
            lastWidth = width
            lastHeight = height

            dispatchSurfaceChanged(holder.surface, width, height)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        dispatchSurfaceDestroyed(holder.surface)
    }
}

@Composable
private fun rememberAndroidExternalSurfaceState(): AndroidExternalSurfaceState {
    val scope = rememberCoroutineScope()
    return remember { AndroidExternalSurfaceState(scope) }
}

/**
 * Defines the z-order of an [AndroidExternalSurface]. When using an
 * [AndroidExternalSurface], a new [Surface] is created and displayed as a separate
 * window layer whose position in the windows layer stack relative to the parent
 * window is decided by its z-order. This class provides constants to set that
 * z-order.
 */
@JvmInline
value class AndroidExternalSurfaceZOrder private constructor(val zOrder: Int) {
    companion object {
        /**
         * The [Surface]'s window layer is positioned behind the parent window.
         */
        val Behind = AndroidExternalSurfaceZOrder(0)
        /**
         * The [Surface]'s window layer is positioned behind the parent window but
         * above other [Surface] window layers marked [Behind].
         */
        val MediaOverlay = AndroidExternalSurfaceZOrder(1)
        /**
         * The [Surface]'s window layer is positioned above the parent window.
         */
        val OnTop = AndroidExternalSurfaceZOrder(2)
    }
}

/**
 * Provides a dedicated drawing [Surface] as a separate layer positioned by default
 * behind the window holding the [AndroidExternalSurface] composable. Because
 * [AndroidExternalSurface] uses a separate window layer, graphics composition is handled
 * by the system compositor which can bypass the GPU and provide better performance and
 * power usage characteristics compared to [AndroidEmbeddedExternalSurface]. It is therefore
 * recommended to use [AndroidExternalSurface] over [AndroidEmbeddedExternalSurface] whenever
 * possible.
 *
 * The [Surface] provided can be used to present content that's external to Compose, such as
 * a video stream (from a camera or a media player), OpenGL, Vulkan...The provided [Surface]
 * can be rendered into using a thread different from the main thread.
 *
 * The z-ordering of the surface can be controlled using the [zOrder] parameter:
 *
 * - [AndroidExternalSurfaceZOrder.Behind]: positions the surface behind the window
 * - [AndroidExternalSurfaceZOrder.MediaOverlay]: positions the surface behind the window but
 *   above other [AndroidExternalSurfaceZOrder.Behind] surfaces
 * - [AndroidExternalSurfaceZOrder.OnTop]: positions the surface above the window
 *
 * The drawing surface is opaque by default, which can be controlled with the [isOpaque]
 * parameter. When the surface is transparent, you may need to change the z-order to
 * see something behind the surface.
 *
 * To start rendering, the caller must first acquire the [Surface] when it's created.
 * This is achieved by providing the [onInit] lambda, which allows the caller to
 * register an appropriate [AndroidExternalSurfaceScope.onSurface] callback. The [onInit]
 * lambda can also be used to initialize/cache resources needed once a surface is
 * available.
 *
 * After acquiring a surface, the caller can start rendering into it. Rendering into a
 * surface can be done from any thread.
 *
 * It is recommended to register the [SurfaceScope.onChanged] and [SurfaceScope.onDestroyed]
 * callbacks to properly handle the lifecycle of the surface and react to dimension
 * changes. You must ensure that the rendering thread stops interacting with the surface
 * when the [SurfaceScope.onDestroyed] callback is invoked.
 *
 * If a [surfaceSize] is specified (set to non-[IntSize.Zero]), the surface will use
 * the specified size instead of the layout size of this composable. The surface will
 * be stretched at render time to fit the layout size. This can be used for instance to
 * render at a lower resolution for performance reasons.
 *
 * @param modifier Modifier to be applied to the [AndroidExternalSurface]
 * @param isOpaque Whether the managed surface should be opaque or transparent.
 * @param surfaceSize Sets the surface size independently of the layout size of
 *                    this [AndroidExternalSurface]. If set to [IntSize.Zero], the
 *                    surface size will be equal to the [AndroidExternalSurface]
 *                    layout size.
 * @param zOrder Sets the z-order of the surface relative to its parent window.
 * @param isSecure Control whether the surface view's content should be treated as
 *                 secure, preventing it from appearing in screenshots or from being
 *                 viewed on non-secure displays.
 * @param onInit Lambda invoked on first composition. This lambda can be used to
 *               declare a [AndroidExternalSurfaceScope.onSurface] callback that
 *               will be invoked when a surface is available.
 *
 * @sample androidx.compose.foundation.samples.AndroidExternalSurfaceColors
 */
@Composable
fun AndroidExternalSurface(
    modifier: Modifier = Modifier,
    isOpaque: Boolean = true,
    surfaceSize: IntSize = IntSize.Zero,
    zOrder: AndroidExternalSurfaceZOrder = AndroidExternalSurfaceZOrder.Behind,
    isSecure: Boolean = false,
    onInit: AndroidExternalSurfaceScope.() -> Unit
) {
    val state = rememberAndroidExternalSurfaceState()

    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                state.onInit()
                holder.addCallback(state)
            }
        },
        modifier = modifier,
        onReset = { },
        update = { view ->
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

            when (zOrder) {
                AndroidExternalSurfaceZOrder.Behind -> view.setZOrderOnTop(false)
                AndroidExternalSurfaceZOrder.MediaOverlay -> view.setZOrderMediaOverlay(true)
                AndroidExternalSurfaceZOrder.OnTop -> view.setZOrderOnTop(true)
            }

            view.setSecure(isSecure)
        }
    )
}

private class AndroidEmbeddedExternalSurfaceState(scope: CoroutineScope) :
    BaseAndroidExternalSurfaceState(scope), TextureView.SurfaceTextureListener {

    var surfaceSize = IntSize.Zero
    val matrix = android.graphics.Matrix()

    private var surfaceTextureSurface: Surface? = null

    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        var w = width
        var h = height

        if (surfaceSize != IntSize.Zero) {
            w = surfaceSize.width
            h = surfaceSize.height
            surfaceTexture.setDefaultBufferSize(w, h)
        }

        val surface = Surface(surfaceTexture)
        surfaceTextureSurface = surface

        dispatchSurfaceCreated(surface, w, h)
    }

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        var w = width
        var h = height

        if (surfaceSize != IntSize.Zero) {
            w = surfaceSize.width
            h = surfaceSize.height
            surfaceTexture.setDefaultBufferSize(w, h)
        }

        dispatchSurfaceChanged(surfaceTextureSurface!!, w, h)
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        dispatchSurfaceDestroyed(surfaceTextureSurface!!)
        surfaceTextureSurface = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // onSurfaceTextureUpdated is called when the content of the SurfaceTexture
        // has changed, which is not relevant to us since we are the producer here
    }
}

@Composable
private fun rememberAndroidEmbeddedExternalSurfaceState(): AndroidEmbeddedExternalSurfaceState {
    val scope = rememberCoroutineScope()
    return remember { AndroidEmbeddedExternalSurfaceState(scope) }
}

/**
 * Provides a dedicated drawing [Surface] embedded directly in the UI hierarchy.
 * Unlike [AndroidExternalSurface], [AndroidEmbeddedExternalSurface] positions its
 * surface as a regular element inside the composable hierarchy. This means that graphics
 * composition is handled like any other UI widget, using the GPU. This can lead
 * to increased power and memory bandwidth usage compared to [AndroidExternalSurface].
 * It is therefore recommended to use [AndroidExternalSurface] over
 * [AndroidEmbeddedExternalSurface] whenever possible.
 *
 * [AndroidEmbeddedExternalSurface] can however be useful when interactions with other widgets
 * is necessary, for instance if the surface needs to be "sandwiched" between two
 * other widgets, or if it must participate in visual effects driven by
 * a `Modifier.graphicsLayer{}`.
 *
 * The [Surface] provided can be used to present content that's external to Compose,
 * such as a video stream (from a camera or a media player), OpenGL, Vulkan...
 * The provided [Surface] can be rendered into using a thread different from the main
 * thread.
 *
 * The drawing surface is opaque by default, which can be controlled with the [isOpaque]
 * parameter.
 *
 * To start rendering, the caller must first acquire the [Surface] when it's created.
 * This is achieved by providing the [onInit] lambda, which allows the caller to
 * register an appropriate [AndroidExternalSurfaceScope.onSurface] callback. The [onInit]
 * lambda can also be used to initialize/cache resources needed once a surface is
 * available.
 *
 * After acquiring a surface, the caller can start rendering into it. Rendering into a
 * surface can be done from any thread.
 *
 * It is recommended to register the [SurfaceScope.onChanged] and [SurfaceScope.onDestroyed]
 * callbacks to properly handle the lifecycle of the surface and react to dimension
 * changes. You must ensure that the rendering thread stops interacting with the surface
 * when the [SurfaceScope.onDestroyed] callback is invoked.
 *
 * If a [surfaceSize] is specified (set to non-[IntSize.Zero]), the surface will use
 * the specified size instead of the layout size of this composable. The surface will
 * be stretched at render time to fit the layout size. This can be used for instance to
 * render at a lower resolution for performance reasons.
 *
 * @param modifier Modifier to be applied to the [AndroidExternalSurface]
 * @param isOpaque Whether the managed surface should be opaque or transparent. If
 *                 transparent and [isMediaOverlay] is `false`, the surface will
 *                 be positioned above the parent window.
 * @param surfaceSize Sets the surface size independently of the layout size of
 *                    this [AndroidExternalSurface]. If set to [IntSize.Zero], the
 *                    surface size will be equal to the [AndroidExternalSurface]
 *                    layout size.
 * @param transform Sets the transform to apply to the [Surface]. Some transforms
 *                  might prevent the content from drawing all the pixels contained
 *                  within this Composable's bounds. In such situations, make sure
 *                  to set [isOpaque] to `false`.
 * @param onInit Lambda invoked on first composition. This lambda can be used to
 *               declare a [AndroidExternalSurfaceScope.onSurface] callback that
 *               will be invoked when a surface is available.
 *
 * @sample androidx.compose.foundation.samples.AndroidEmbeddedExternalSurfaceColors
 */
@Composable
fun AndroidEmbeddedExternalSurface(
    modifier: Modifier = Modifier,
    isOpaque: Boolean = true,
    surfaceSize: IntSize = IntSize.Zero,
    transform: Matrix? = null,
    onInit: AndroidExternalSurfaceScope.() -> Unit
) {
    val state = rememberAndroidEmbeddedExternalSurfaceState()

    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                state.surfaceSize = surfaceSize
                state.onInit()
                surfaceTextureListener = state
            }
        },
        modifier = modifier,
        onReset = { },
        update = { view ->
            if (surfaceSize != IntSize.Zero) {
                view.surfaceTexture?.setDefaultBufferSize(surfaceSize.width, surfaceSize.height)
            }
            state.surfaceSize = surfaceSize
            view.isOpaque = isOpaque
            // If transform is null, we'll call setTransform(null) which sets the
            // identity transform on the TextureView
            view.setTransform(transform?.let { state.matrix.apply { setFrom(transform) } })
        }
    )
}
