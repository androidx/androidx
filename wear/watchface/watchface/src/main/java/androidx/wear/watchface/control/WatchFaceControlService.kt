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

import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.wear.watchface.IndentingPrintWriter
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.data.CrashInfoParcel
import androidx.wear.watchface.control.data.DefaultProviderPoliciesParams
import androidx.wear.watchface.control.data.GetComplicationSlotMetadataParams
import androidx.wear.watchface.control.data.GetUserStyleFlavorsParams
import androidx.wear.watchface.control.data.GetUserStyleSchemaParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.IdTypeAndDefaultProviderPolicyWireFormat
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.data.ComplicationSlotMetadataWireFormat
import androidx.wear.watchface.editor.EditorService
import androidx.wear.watchface.runBlockingWithTracing
import androidx.wear.watchface.style.data.UserStyleFlavorsWireFormat
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
import androidx.wear.watchface.utility.AsyncTraceEvent
import androidx.wear.watchface.utility.TraceEvent
import java.io.FileDescriptor
import java.io.PrintWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

/**
 * A service for creating and controlling watch face instances.
 *
 */
@RequiresApi(27)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class WatchFaceControlService : Service() {
    private var watchFaceInstanceServiceStub: IWatchFaceInstanceServiceStub? = null

    public companion object {
        public const val ACTION_WATCHFACE_CONTROL_SERVICE: String =
            "com.google.android.wearable.action.WATCH_FACE_CONTROL"
        internal const val TAG = "IWatchFaceInstanceServiceStub"
    }

    override fun onBind(intent: Intent?): IBinder? =
        TraceEvent("WatchFaceControlService.onBind").use {
            if (ACTION_WATCHFACE_CONTROL_SERVICE == intent?.action) {
                if (watchFaceInstanceServiceStub == null) {
                    watchFaceInstanceServiceStub = createServiceStub()
                }
                watchFaceInstanceServiceStub
            } else {
                null
            }
        }

    open fun createWatchFaceService(watchFaceName: ComponentName): WatchFaceService? {
        return try {
            val watchFaceServiceClass = Class.forName(watchFaceName.className) ?: return null
            if (!WatchFaceService::class.java.isAssignableFrom(watchFaceServiceClass)) {
                return null
            }
            watchFaceServiceClass.getConstructor().newInstance() as WatchFaceService
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "createWatchFaceService failed for $watchFaceName", e)
            null
        }
    }

    @VisibleForTesting
    public open fun createServiceStub(): IWatchFaceInstanceServiceStub =
        TraceEvent("WatchFaceControlService.createServiceStub").use {
            IWatchFaceInstanceServiceStub(this, MainScope())
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

    override fun onDestroy() {
        super.onDestroy()
        watchFaceInstanceServiceStub?.onDestroy()
    }
}

@RequiresApi(27)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class IWatchFaceInstanceServiceStub(
    // We need to explicitly null this object in onDestroy to avoid a memory leak.
    private var service: WatchFaceControlService?,
    private val uiThreadCoroutineScope: CoroutineScope
) : IWatchFaceControlService.Stub() {
    override fun getApiVersion(): Int = IWatchFaceControlService.API_VERSION

    internal companion object {
        const val TAG = "IWatchFaceInstanceServiceStub"
    }

    override fun getInteractiveWatchFaceInstance(instanceId: String): IInteractiveWatchFace? =
        TraceEvent("IWatchFaceInstanceServiceStub.getInteractiveWatchFaceInstance").use {
            // This call is thread safe so we don't need to trampoline via the UI thread.
            InteractiveInstanceManager.getAndRetainInstance(instanceId)
        }

    override fun createHeadlessWatchFaceInstance(
        params: HeadlessWatchFaceInstanceParams
    ): IHeadlessWatchFace? =
        TraceEvent("IWatchFaceInstanceServiceStub.createHeadlessWatchFaceInstance").use {
            createServiceAndHeadlessEngine(params.watchFaceName)?.let { serviceAndEngine ->
                // This is serviced on a background thread so it should be fine to block.
                uiThreadCoroutineScope.runBlockingWithTracing("createHeadlessInstance") {
                    // However the WatchFaceService.createWatchFace method needs to be run on the UI
                    // thread.
                    serviceAndEngine.engine.createHeadlessInstance(params)
                }
            }
        }

    private class ServiceAndEngine(
        val service: WatchFaceService,
        val engine: WatchFaceService.EngineWrapper
    ) {
        fun destroy() {
            try {
                engine.onDestroy()
                service.onDestroy()
            } catch (e: Exception) {
                Log.e(TAG, "ServiceAndEngine.destroy failed due to exception", e)
                throw e
            }
        }
    }

    @SuppressLint("BanUncheckedReflection")
    private fun createServiceAndHeadlessEngine(watchFaceName: ComponentName) =
        TraceEvent("IWatchFaceInstanceServiceStub.createEngine").use {
            // Attempt to construct the class for the specified watchFaceName, failing if it either
            // doesn't exist or isn't a [WatchFaceService].
            val watchFaceService = service?.createWatchFaceService(watchFaceName)

            if (watchFaceService != null) {
                // Set the context and if possible the application for watchFaceService.
                try {
                    val method = Service::class.java.declaredMethods.find { it.name == "attach" }
                    method!!.isAccessible = true
                    method.invoke(
                        watchFaceService,
                        service as Context,
                        null,
                        watchFaceService::class.qualifiedName,
                        null,
                        service!!.application,
                        null
                    )
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "createServiceAndHeadlessEngine can't call attach by reflection, " +
                            "falling back to setContext",
                        e
                    )
                    watchFaceService.setContext(watchFaceService)
                }
                watchFaceService.onCreate()
                val engine =
                    watchFaceService.createHeadlessEngine() as WatchFaceService.EngineWrapper
                ServiceAndEngine(watchFaceService, engine)
            } else {
                null
            }
        }

    override fun getOrCreateInteractiveWatchFace(
        params: WallpaperInteractiveWatchFaceInstanceParams,
        callback: IPendingInteractiveWatchFace
    ): IInteractiveWatchFace? {
        val asyncTraceEvent =
            AsyncTraceEvent("IWatchFaceInstanceServiceStub.getOrCreateInteractiveWatchFaceWCS")
        return try {
            InteractiveInstanceManager
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
        } catch (e: Exception) {
            Log.e(TAG, "getOrCreateInteractiveWatchFace failed ", e)
            throw e
        }
    }

    override fun getEditorService(): EditorService = EditorService.globalEditorService

    override fun getDefaultProviderPolicies(
        params: DefaultProviderPoliciesParams
    ): Array<IdTypeAndDefaultProviderPolicyWireFormat>? =
        createServiceAndHeadlessEngineAndEvaluate(
            params.watchFaceName,
            "IWatchFaceInstanceServiceStub.getDefaultProviderPolicies"
        ) {
            it.engine.getDefaultProviderPolicies()
        }

    override fun getUserStyleSchema(params: GetUserStyleSchemaParams): UserStyleSchemaWireFormat? =
        createServiceAndHeadlessEngineAndEvaluate(
            params.watchFaceName,
            "IWatchFaceInstanceServiceStub.getUserStyleSchema"
        ) {
            it.engine.getUserStyleSchemaWireFormat()
        }

    override fun getComplicationSlotMetadata(
        params: GetComplicationSlotMetadataParams
    ): Array<ComplicationSlotMetadataWireFormat>? =
        createServiceAndHeadlessEngineAndEvaluate(
            params.watchFaceName,
            "IWatchFaceInstanceServiceStub.getComplicationSlotMetadata"
        ) {
            it.engine.getComplicationSlotMetadataWireFormats()
        }

    override fun hasComplicationCache() = true

    override fun getUserStyleFlavors(
        params: GetUserStyleFlavorsParams
    ): UserStyleFlavorsWireFormat? =
        createServiceAndHeadlessEngineAndEvaluate(
            params.watchFaceName,
            "IWatchFaceInstanceServiceStub.getUserStyleFlavors"
        ) {
            it.engine.getUserStyleFlavorsWireFormat()
        }

    private fun <T> createServiceAndHeadlessEngineAndEvaluate(
        watchFaceName: ComponentName,
        functionName: String,
        function: (serviceAndEngine: ServiceAndEngine) -> T
    ): T? =
        TraceEvent(functionName).use {
            return try {
                createServiceAndHeadlessEngine(watchFaceName)?.let { serviceAndEngine ->
                    try {
                        function(serviceAndEngine)
                    } finally {
                        serviceAndEngine.destroy()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "$functionName failed due to exception", e)
                throw e
            }
        }

    fun onDestroy() {
        service = null
    }
}
