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
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.runOnHandler

/**
 *  A service for creating and controlling WatchFaceInstances.
 *
 *  @hide
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
        if (ACTION_WATCHFACE_CONTROL_SERVICE == intent?.action) {
            watchFaceInstanceServiceStub
        } else {
            null
        }

    // Required for testing
    public fun setContext(context: Context) {
        attachBaseContext(context)
    }
}

@RequiresApi(27)
private class IWatchFaceInstanceServiceStub(
    private val context: Context,
    private val uiThreadHandler: Handler
) : IWatchFaceControlService.Stub() {
    override fun getApiVersion() = IWatchFaceControlService.API_VERSION

    override fun getInteractiveWatchFaceInstanceSysUI(instanceId: String) =
        uiThreadHandler.runOnHandler {
            InteractiveInstanceManager.getAndRetainInstance(instanceId)?.createSysUiApi()
        }

    override fun createHeadlessWatchFaceInstance(
        params: HeadlessWatchFaceInstanceParams
    ): IHeadlessWatchFace? =
        uiThreadHandler.runOnHandler {
            createEngine(params.watchFaceName, context)?.createHeadlessInstance(params)
        }

    private fun createEngine(
        watchFaceName: ComponentName,
        context: Context
    ): WatchFaceService.EngineWrapper? {
        // Attempt to construct the class for the specified watchFaceName, failing if it either
        // doesn't exist or isn't a [WatchFaceService].
        val watchFaceServiceClass = Class.forName(watchFaceName.className) ?: return null
        if (!WatchFaceService::class.java.isAssignableFrom(WatchFaceService::class.java)) {
            return null
        }
        val watchFaceService =
            watchFaceServiceClass.getConstructor().newInstance() as WatchFaceService
        watchFaceService.setContext(context)
        return watchFaceService.onCreateEngine() as WatchFaceService.EngineWrapper
    }

    override fun getOrCreateInteractiveWatchFaceWCS(
        params: WallpaperInteractiveWatchFaceInstanceParams,
        callback: IPendingInteractiveWatchFaceWCS
    ) = InteractiveInstanceManager
        .getExistingInstanceOrSetPendingWallpaperInteractiveWatchFaceInstance(
            InteractiveInstanceManager.PendingWallpaperInteractiveWatchFaceInstance(
                params,
                callback
            )
        )
}
