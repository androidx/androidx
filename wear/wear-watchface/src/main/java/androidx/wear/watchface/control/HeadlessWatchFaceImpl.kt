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

import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.data.ComplicationScreenshotParams
import androidx.wear.watchface.control.data.WatchfaceScreenshotParams
import androidx.wear.watchface.runOnHandler

/**
 * A headless watch face instance. This doesn't render asynchronously and the exposed API makes it
 * effectively stateless.
 */
@RequiresApi(27)
internal class HeadlessWatchFaceImpl(
    internal var engine: WatchFaceService.EngineWrapper?,
    private val uiThreadHandler: Handler
) : IHeadlessWatchFace.Stub() {

    override fun getApiVersion() = IHeadlessWatchFace.API_VERSION

    override fun takeWatchFaceScreenshot(params: WatchfaceScreenshotParams) =
        uiThreadHandler.runOnHandler { engine!!.takeWatchFaceScreenshot(params) }

    override fun getPreviewReferenceTimeMillis() = engine!!.watchFaceImpl.previewReferenceTimeMillis

    override fun getComplicationState() =
        uiThreadHandler.runOnHandler { engine!!.getComplicationState() }

    override fun takeComplicationScreenshot(params: ComplicationScreenshotParams) =
        uiThreadHandler.runOnHandler { engine!!.takeComplicationScreenshot(params) }

    override fun getUserStyleSchema() =
        engine!!.watchFaceImpl.userStyleRepository.schema.toWireFormat()

    override fun release() {
        engine?.onDestroy()
        engine = null
    }
}
