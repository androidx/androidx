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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.wear.utility.AsyncTraceEvent
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.IndentingPrintWriter
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.editor.EditorService
import androidx.wear.watchface.runOnHandlerWithTracing
import kotlinx.coroutines.runBlocking
import java.io.FileDescriptor
import java.io.PrintWriter

/**
 * A service for creating and controlling watch face instances.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@RequiresApi(27)
public class WatchFaceControlService : Service() {
    private val watchFaceInstanceServiceStub =
        IWatchFaceInstanceServiceStub(this, Handler(Looper.getMainLooper()))

    /** @hide */
    public companion object {
        public const val ACTION_WATCHFACE_CONTROL_SERVICE: String =
            "com.google.android.wearable.action.WATCH_FACE_CONTROL"
    }

    override fun onBind(intent: Intent?): IBinder? =
        TraceEvent("WatchFaceControlService.onBind").use {
            if (ACTION_WATCHFACE_CONTROL_SERVICE == intent?.action) {
                watchFaceInstanceServiceStub
            } else {
                null
            }
        }

    // Required for testing
    public fun setContext(context: Context) {
        attachBaseContext(context)
    }

    @UiThread
    override fun dump(fd: FileDescriptor, writer: PrintWriter, args: Array<String>) {
        val indentingPrintWriter = IndentingPrintWriter(writer)
        indentingPrintWriter.println("WatchFaceControlService:")
        InteractiveInstanceManager.dump(indentingPrintWriter)
        HeadlessWatchFaceImpl.dump(indentingPrintWriter)
        indentingPrintWriter.flush()
    }
}

/**
 * Factory for use by on watch face editors to create [IWatchFaceControlService].
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(27)
public class WatchFaceControlServiceFactory {
    public companion object {
        @JvmStatic
        public fun createWatchFaceControlService(
            context: Context,
            uiThreadHandler: Handler
        ): IWatchFaceControlService = IWatchFaceInstanceServiceStub(context, uiThreadHandler)
    }
}

@RequiresApi(27)
private class IWatchFaceInstanceServiceStub(
    private val context: Context,
    private val uiThreadHandler: Handler
) : IWatchFaceControlService.Stub() {
    override fun getApiVersion() = IWatchFaceControlService.API_VERSION

    override fun getInteractiveWatchFaceInstanceSysUI(instanceId: String) =
        TraceEvent("IWatchFaceInstanceServiceStub.getInteractiveWatchFaceInstanceSysUI").use {
            // This call is thread safe so we don't need to trampoline via the UI thread.
            InteractiveInstanceManager.getAndRetainInstance(instanceId)?.createSysUiApi()
        }

    override fun createHeadlessWatchFaceInstance(
        params: HeadlessWatchFaceInstanceParams
    ): IHeadlessWatchFace? = uiThreadHandler.runOnHandlerWithTracing(
        "IWatchFaceInstanceServiceStub.createHeadlessWatchFaceInstance"
    ) {
        val engine = createHeadlessEngine(params.watchFaceName, context)
        engine?.let {
            // This is serviced on a background thread so it should be fine to block.
            runBlocking { it.createHeadlessInstance(params) }
        }
    }

    private fun createHeadlessEngine(
        watchFaceName: ComponentName,
        context: Context
    ) = TraceEvent("IWatchFaceInstanceServiceStub.createEngine").use {
        // Attempt to construct the class for the specified watchFaceName, failing if it either
        // doesn't exist or isn't a [WatchFaceService].
        try {
            val watchFaceServiceClass = Class.forName(watchFaceName.className) ?: return null
            if (!WatchFaceService::class.java.isAssignableFrom(WatchFaceService::class.java)) {
                null
            } else {
                val watchFaceService =
                    watchFaceServiceClass.getConstructor().newInstance() as WatchFaceService
                watchFaceService.setContext(context)
                val engine =
                    watchFaceService.createHeadlessEngine() as WatchFaceService.EngineWrapper
                engine
            }
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    override fun getOrCreateInteractiveWatchFaceWCS(
        params: WallpaperInteractiveWatchFaceInstanceParams,
        callback: IPendingInteractiveWatchFaceWCS
    ): IInteractiveWatchFaceWCS? {
        val asyncTraceEvent =
            AsyncTraceEvent("IWatchFaceInstanceServiceStub.getOrCreateInteractiveWatchFaceWCS")
        return InteractiveInstanceManager
            .getExistingInstanceOrSetPendingWallpaperInteractiveWatchFaceInstance(
                InteractiveInstanceManager.PendingWallpaperInteractiveWatchFaceInstance(
                    params,
                    // Wrapped IPendingInteractiveWatchFaceWCS to support tracing.
                    object : IPendingInteractiveWatchFaceWCS.Stub() {
                        override fun getApiVersion() = callback.apiVersion

                        override fun onInteractiveWatchFaceWcsCreated(
                            iInteractiveWatchFaceWcs: IInteractiveWatchFaceWCS?
                        ) {
                            asyncTraceEvent.close()
                            callback.onInteractiveWatchFaceWcsCreated(iInteractiveWatchFaceWcs)
                        }
                    }
                )
            )
    }

    override fun getEditorService() = EditorService.globalEditorService
}