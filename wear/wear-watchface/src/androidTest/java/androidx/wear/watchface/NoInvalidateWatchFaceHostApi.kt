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

package androidx.wear.watchface

import android.content.ComponentName
import android.graphics.Rect
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import androidx.wear.watchface.style.data.UserStyleWireFormat
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat

/**
 * This test harness wraps [WatchFaceHost] and makes invalidate do nothing giving the test
 * full control over when frames are produced.
 */
class NoInvalidateWatchFaceHostApi(val api: WatchFaceHostApi) : WatchFaceHostApi {

    companion object {
        fun create(host: WatchFaceHost): WatchFaceHost {
            return WatchFaceHost().apply {
                api = NoInvalidateWatchFaceHostApi(host.api!!)
            }
        }
    }

    override fun getContext() = api.getContext()

    override fun getHandler() = api.getHandler()

    override fun registerWatchFaceType(watchFaceType: Int) {
        api.registerWatchFaceType(watchFaceType)
    }

    override fun registerUserStyleSchema(userStyleSchema: UserStyleSchemaWireFormat) {
        api.registerUserStyleSchema(userStyleSchema)
    }

    override fun setCurrentUserStyle(userStyle: UserStyleWireFormat) {
        api.setCurrentUserStyle(userStyle)
    }

    override fun getStoredUserStyle() =
        api.getStoredUserStyle()

    override fun setComplicationDetails(complicationId: Int, bounds: Rect, boundsType: Int) {
        api.setComplicationDetails(complicationId, bounds, boundsType)
    }

    override fun setComplicationSupportedTypes(complicationId: Int, types: IntArray) {
        api.setComplicationSupportedTypes(complicationId, types)
    }

    override fun setContentDescriptionLabels(labels: Array<ContentDescriptionLabel>) {
        api.setContentDescriptionLabels(labels)
    }

    override fun setActiveComplications(watchFaceComplicationIds: IntArray) {
        api.setActiveComplications(watchFaceComplicationIds)
    }

    override fun setDefaultComplicationProviderWithFallbacks(
        watchFaceComplicationId: Int,
        providers: List<ComponentName>?,
        fallbackSystemProvider: Int,
        type: Int
    ) {
        api.setDefaultComplicationProviderWithFallbacks(
            watchFaceComplicationId,
            providers,
            fallbackSystemProvider,
            type
        )
    }

    override fun invalidate() {
        // Don't do anything!
    }
}