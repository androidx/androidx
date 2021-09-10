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
import androidx.annotation.VisibleForTesting
import androidx.wear.utility.AsyncTraceEvent
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.IndentingPrintWriter
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.data.CrashInfoParcel
import androidx.wear.watchface.control.data.DefaultProviderPoliciesParams
import androidx.wear.watchface.control.data.GetComplicationSlotMetadataParams
import androidx.wear.watchface.control.data.GetUserStyleSchemaParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.IdTypeAndDefaultProviderPolicyWireFormat
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.data.ComplicationSlotMetadataWireFormat
import androidx.wear.watchface.editor.EditorService
import androidx.wear.watchface.runBlockingOnHandlerWithTracing
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
import java.io.FileDescriptor
import java.io.PrintWriter

/**
 * A service for creating and controlling watch face instances.
 *
 * @hide
 */
@RequiresApi(27)
@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class WatchFaceControlService : Service() {
    private val watchFaceInstanceServiceStub by lazy { createServiceStub() }

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

    @VisibleForTesting
    public open fun createServiceStub(): IWatchFaceInstanceServiceStub =
        TraceEvent("WatchFaceControlService.createServiceStub").use {
            IWatchFaceInstanceServiceStub(this, Handler(Looper.getMainLooper()))
        }

    @VisibleForTesting
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

/** @hide */
@RequiresApi(27)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class IWatchFaceInstanceServiceStub(
    private val context: Context,
    private val uiThreadHandler: Handler
) : IWatchFaceControlService.Stub() {
    override fun getApiVersion(): Int = IWatchFaceControlService.API_VERSION

    override fun getInteractiveWatchFaceInstance(instanceId: String): IInteractiveWatchFace? =
        TraceEvent("IWatchFaceInstanceServiceStub.getInteractiveWatchFaceInstance").use {
            // This call is thread safe so we don't need to trampoline via the UI thread.
            InteractiveInstanceManager.getAndRetainInstance(instanceId)
        }

    override fun createHeadlessWatchFaceInstance(
        params: HeadlessWatchFaceInstanceParams
    ): IHeadlessWatchFace? = TraceEvent(
        "IWatchFaceInstanceServiceStub.createHeadlessWatchFaceInstance"
    ).use {
        val engine = createHeadlessEngine(params.watchFaceName, context)
        engine?.let {
            // This is serviced on a background thread so it should be fine to block.
            uiThreadHandler.runBlockingOnHandlerWithTracing("createHeadlessInstance") {
                // However the WatchFaceService.createWatchFace method needs to be run on the UI
                // thread.
                it.createHeadlessInstance(params)
            }
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

    override fun getOrCreateInteractiveWatchFace(
        params: WallpaperInteractiveWatchFaceInstanceParams,
        callback: IPendingInteractiveWatchFace
    ): IInteractiveWatchFace? {
        val asyncTraceEvent =
            AsyncTraceEvent("IWatchFaceInstanceServiceStub.getOrCreateInteractiveWatchFaceWCS")
        return InteractiveInstanceManager
            .getExistingInstanceOrSetPendingWallpaperInteractiveWatchFaceInstance(
                InteractiveInstanceManager.PendingWallpaperInteractiveWatchFaceInstance(
                    params,
                    // Wrapped IPendingInteractiveWatchFace to support tracing.
                    object : IPendingInteractiveWatchFace.Stub() {
                        override fun getApiVersion() = callback.apiVersion

                        override fun onInteractiveWatchFaceCreated(
                            iInteractiveWatchFaceWcs: IInteractiveWatchFace?
                        ) {
                            asyncTraceEvent.close()
                            callback.onInteractiveWatchFaceCreated(iInteractiveWatchFaceWcs)
                        }

                        override fun onInteractiveWatchFaceCrashed(exception: CrashInfoParcel) {
                            asyncTraceEvent.close()
                            callback.onInteractiveWatchFaceCrashed(exception)
                        }
                    }
                )
            )
    }

    override fun getEditorService(): EditorService = EditorService.globalEditorService

    override fun getDefaultProviderPolicies(
        params: DefaultProviderPoliciesParams
    ): Array<IdTypeAndDefaultProviderPolicyWireFormat>? = TraceEvent(
        "IWatchFaceInstanceServiceStub.getDefaultProviderPolicies"
    ).use {
        createHeadlessEngine(params.watchFaceName, context)?.let { engine ->
            val result = engine.getDefaultProviderPolicies()
            engine.onDestroy()
            result
        }
    }

    override fun getUserStyleSchema(
        params: GetUserStyleSchemaParams
    ): UserStyleSchemaWireFormat? = TraceEvent(
        "IWatchFaceInstanceServiceStub.getUserStyleSchema"
    ).use {
        createHeadlessEngine(params.watchFaceName, context)?.let { engine ->
            val result = engine.getUserStyleSchemaWireFormat()
            engine.onDestroy()
            result
        }
    }

    override fun getComplicationSlotMetadata(
        params: GetComplicationSlotMetadataParams
    ): Array<ComplicationSlotMetadataWireFormat>? = TraceEvent(
        "IWatchFaceInstanceServiceStub.getComplicationSlotMetadata"
    ).use {
        createHeadlessEngine(params.watchFaceName, context)?.let { engine ->
            val result = engine.getComplicationSlotMetadataWireFormats()
            engine.onDestroy()
            result
        }
    }
}
