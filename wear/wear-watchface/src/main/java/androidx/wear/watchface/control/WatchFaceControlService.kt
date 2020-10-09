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

package androidx.wear.watchface.control

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.wearable.watchface.Constants
import android.support.wearable.watchface.IWatchFaceService
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.RestrictTo
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.data.ImmutableSystemState
import androidx.wear.watchface.runOnHandler

/**
 *  A service for creating and controlling WatchFaceInstances.
 *
 *  @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class WatchFaceControlService : Service() {
    private val watchFaceInstanceServiceStub = IWatchFaceInstanceServiceStub(this)

    /** @hide */
    companion object {
        const val ACTION_WATCHFACE_CONTROL_SERVICE =
            "com.google.android.wearable.action.WATCH_FACE_CONTROL"
    }

    override fun onBind(intent: Intent?): IBinder? =
        if (ACTION_WATCHFACE_CONTROL_SERVICE == intent?.action) {
            watchFaceInstanceServiceStub
        } else {
            null
        }

    override fun onDestroy() {
        super.onDestroy()
        watchFaceInstanceServiceStub.onDestroy()
    }

    // Required for testing.
    internal fun setContext(context: Context) {
        attachBaseContext(context)
    }
}

private class IWatchFaceInstanceServiceStub(
    private val context: Context
) : IWatchFaceControlService.Stub() {
    private val instances = HashSet<IWatchFaceInstanceStub>()

    override fun getApiVersion() = IWatchFaceControlService.API_VERSION

    override fun createWatchFaceInstance(watchFaceName: ComponentName): IWatchFaceInstance? {
        // Attempt to construct the class for the specified watchFaceName, failing if it either
        // doesn't exist or isn't a [WatchFaceService].
        val watchFaceServiceClass = Class.forName(watchFaceName.className) ?: return null
        if (!WatchFaceService::class.java.isAssignableFrom(WatchFaceService::class.java)) {
            return null
        }
        val watchFaceService =
            watchFaceServiceClass.getConstructor().newInstance() as WatchFaceService
        watchFaceService.setContext(context)
        val instance = IWatchFaceInstanceStub(
            watchFaceService
        )
        instances.add(instance)
        return instance
    }

    fun onDestroy() {
        instances.forEach { it.destroy() }
        instances.clear()
    }
}

private class IWatchFaceInstanceStub(
    watchFaceService: WatchFaceService
) : IWatchFaceInstance.Stub() {
    private val uiThreadHandler = Handler(Looper.getMainLooper())
    private var engine: WatchFaceService.EngineWrapper? = uiThreadHandler.runOnHandler {
        watchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
    }

    override fun getApiVersion() = IWatchFaceInstance.API_VERSION

    override fun initWithoutSurface(
        iWatchFaceService: IWatchFaceService,
        immutableSystemState: ImmutableSystemState,
        width: Int,
        height: Int
    ) {
        uiThreadHandler.runOnHandler {
            // We don't want to allow the watch face to animate so we don't need to set a
            // useable SurfaceHolder.
            engine!!.allowWatchfaceToAnimate = false
            engine!!.onSurfaceChanged(
                object : SurfaceHolder {
                    override fun setType(type: Int) {
                        throw NotImplementedError()
                    }

                    override fun getSurface(): Surface {
                        throw NotImplementedError()
                    }

                    override fun setSizeFromLayout() {
                        throw NotImplementedError()
                    }

                    override fun lockCanvas(): Canvas {
                        throw NotImplementedError()
                    }

                    override fun lockCanvas(dirty: Rect?): Canvas {
                        throw NotImplementedError()
                    }

                    override fun getSurfaceFrame() = Rect(0, 0, width, height)

                    override fun setFixedSize(width: Int, height: Int) {
                        throw NotImplementedError()
                    }

                    override fun removeCallback(callback: SurfaceHolder.Callback?) {
                        throw NotImplementedError()
                    }

                    override fun isCreating(): Boolean {
                        throw NotImplementedError()
                    }

                    override fun addCallback(callback: SurfaceHolder.Callback?) {
                        throw NotImplementedError()
                    }

                    override fun setFormat(format: Int) {
                        throw NotImplementedError()
                    }

                    override fun setKeepScreenOn(screenOn: Boolean) {
                        throw NotImplementedError()
                    }

                    override fun unlockCanvasAndPost(canvas: Canvas?) {
                        throw NotImplementedError()
                    }
                },
                0,
                width,
                height
            )
        }

        registerIWatchFaceService(iWatchFaceService)
        sendImmutableProperties(immutableSystemState)
    }

    override fun initWithSurface(
        iWatchFaceService: IWatchFaceService,
        immutableSystemState: ImmutableSystemState,
        surface: Surface,
        format: Int,
        width: Int,
        height: Int
    ) {
        onSurfaceChanged(surface, format, width, height)
        registerIWatchFaceService(iWatchFaceService)
        sendImmutableProperties(immutableSystemState)
    }

    override fun onSurfaceChanged(
        surface: Surface,
        format: Int,
        width: Int,
        height: Int
    ) {
        uiThreadHandler.runOnHandler {
            // We want the watchface to render asynchronously onto the provided surface, so we
            // need to set a minimal SurfaceHolder.
            engine!!.allowWatchfaceToAnimate = true
            engine!!.onSurfaceChanged(
                object : SurfaceHolder {
                    override fun setType(type: Int) {
                        throw NotImplementedError()
                    }

                    override fun getSurface() = surface

                    override fun setSizeFromLayout() {
                        throw NotImplementedError()
                    }

                    override fun lockCanvas() = surface.lockCanvas(surfaceFrame)

                    override fun lockCanvas(dirty: Rect?) = surface.lockCanvas(dirty)

                    override fun getSurfaceFrame() = Rect(0, 0, width, height)

                    override fun setFixedSize(width: Int, height: Int) {
                        throw NotImplementedError()
                    }

                    override fun removeCallback(callback: SurfaceHolder.Callback?) {
                        throw NotImplementedError()
                    }

                    override fun isCreating() = false

                    override fun addCallback(callback: SurfaceHolder.Callback?) {
                        throw NotImplementedError()
                    }

                    override fun setFormat(format: Int) {
                        throw NotImplementedError()
                    }

                    override fun setKeepScreenOn(screenOn: Boolean) {
                        throw NotImplementedError()
                    }

                    override fun unlockCanvasAndPost(canvas: Canvas?) =
                        surface.unlockCanvasAndPost(canvas)
                },
                format,
                width,
                height
            )
        }
    }

    private fun registerIWatchFaceService(iWatchFaceService: IWatchFaceService) {
        uiThreadHandler.runOnHandler {
            engine!!.onCommand(
                Constants.COMMAND_SET_BINDER,
                /* x = */ 0,
                /* y = */ 0,
                /* z = */ 0,
                Bundle().apply {
                    putBinder(Constants.EXTRA_BINDER, iWatchFaceService.asBinder())
                },
                false
            )
            Unit
        }
    }

    private fun sendImmutableProperties(immutableSystemState: ImmutableSystemState) {
        uiThreadHandler.runOnHandler {
            engine!!.onPropertiesChanged(
                Bundle().apply {
                    putBoolean(
                        Constants.PROPERTY_LOW_BIT_AMBIENT,
                        immutableSystemState.hasLowBitAmbient
                    )
                    putBoolean(
                        Constants.PROPERTY_BURN_IN_PROTECTION,
                        immutableSystemState.hasBurnInProtection
                    )
                }
            )
        }
    }

    override fun destroy() {
        uiThreadHandler.runOnHandler {
            engine!!.onDestroy()
            engine = null
        }
    }
}