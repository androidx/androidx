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
import androidx.annotation.UiThread
import androidx.wear.watchface.IndentingPrintWriter
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.control.data.ComplicationRenderParams
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.runBlockingOnHandlerWithTracing
import androidx.wear.watchface.runOnHandlerWithTracing

/**
 * A headless watch face instance. This doesn't render asynchronously and the exposed API makes it
 * effectively stateless.
 */
@RequiresApi(27)
internal class HeadlessWatchFaceImpl(
    internal var engine: WatchFaceService.EngineWrapper?,
    private val uiThreadHandler: Handler
) : IHeadlessWatchFace.Stub() {

    internal companion object {
        @UiThread
        fun dump(indentingPrintWriter: IndentingPrintWriter) {
            indentingPrintWriter.println("HeadlessWatchFace instances:")
            indentingPrintWriter.increaseIndent()
            for (instance in headlessInstances) {
                require(instance.uiThreadHandler.looper.isCurrentThread) {
                    "dump must be called from the UIThread"
                }
                indentingPrintWriter.println("HeadlessWatchFaceImpl:")
                indentingPrintWriter.increaseIndent()
                instance.engine?.dump(indentingPrintWriter)
                indentingPrintWriter.decreaseIndent()
            }
            indentingPrintWriter.decreaseIndent()
        }

        private val headlessInstances = HashSet<HeadlessWatchFaceImpl>()
    }

    init {
        uiThreadHandler.runOnHandlerWithTracing("HeadlessWatchFaceImpl.init") {
            headlessInstances.add(this)
        }
    }

    override fun getApiVersion() = IHeadlessWatchFace.API_VERSION

    override fun renderWatchFaceToBitmap(params: WatchFaceRenderParams) =
        uiThreadHandler.runBlockingOnHandlerWithTracing(
            "HeadlessWatchFaceImpl.renderWatchFaceToBitmap"
        ) {
            engine!!.renderWatchFaceToBitmap(params)
        }

    override fun getPreviewReferenceTimeMillis() = engine!!.watchFaceImpl.previewReferenceTimeMillis

    override fun getComplicationState() =
        uiThreadHandler.runBlockingOnHandlerWithTracing(
            "HeadlessWatchFaceImpl.getComplicationState"
        ) {
            engine!!.getComplicationState()
        }

    override fun renderComplicationToBitmap(params: ComplicationRenderParams) =
        uiThreadHandler.runBlockingOnHandlerWithTracing(
            "HeadlessWatchFaceImpl.renderComplicationToBitmap"
        ) {
            engine!!.renderComplicationToBitmap(params)
        }

    override fun getUserStyleSchema() =
        engine!!.watchFaceImpl.currentUserStyleRepository.schema.toWireFormat()

    override fun release() {
        uiThreadHandler.runOnHandlerWithTracing("HeadlessWatchFaceImpl.release") {
            headlessInstances.remove(this)
            engine?.onDestroy()
            engine = null
        }
    }
}
