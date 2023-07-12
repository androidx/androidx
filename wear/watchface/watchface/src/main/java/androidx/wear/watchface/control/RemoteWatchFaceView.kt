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

package androidx.wear.watchface.control

import android.os.Build
import android.util.Log
import android.view.SurfaceControlViewHost
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.utility.AsyncTraceEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.R)
internal class RemoteWatchFaceView(
    view: SurfaceView,
    private val host: SurfaceControlViewHost,
    private val uiThreadCoroutineScope: CoroutineScope,
    private val renderCallback: (SurfaceHolder, WatchFaceRenderParams) -> Unit
) : IRemoteWatchFaceView.Stub() {
    val deferredSurfaceHolder = CompletableDeferred<SurfaceHolder>()

    companion object {
        const val TAG = "RemoteWatchFaceView"
    }

    init {
        view.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                    Log.d(TAG, "surfaceCreated")
                    deferredSurfaceHolder.complete(surfaceHolder)
                }

                override fun surfaceChanged(
                    surfaceHolder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    Log.d(TAG, "surfaceChanged")
                }

                override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                    Log.d(TAG, "surfaceDestroyed")
                }
            }
        )
    }

    override fun getApiVersion() = IRemoteWatchFaceView.API_VERSION

    override fun renderWatchFace(params: WatchFaceRenderParams) {
        val traceEvent = AsyncTraceEvent("RemoteWatchFaceView.renderWatchFace")
        uiThreadCoroutineScope.launch {
            renderCallback(deferredSurfaceHolder.await(), params)
            traceEvent.close()
        }
    }

    override fun getSurfacePackage(): SurfaceControlViewHost.SurfacePackage {
        return host.surfacePackage!!
    }

    override fun close() {
        host.release()
    }
}
