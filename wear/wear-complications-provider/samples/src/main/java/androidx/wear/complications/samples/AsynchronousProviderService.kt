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

package androidx.wear.complications.samples

import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText
import androidx.wear.complications.ComplicationProviderService
import java.util.concurrent.Executors

/** A minimal complication provider which reports the ID of the complication asynchronously. */
class AsynchronousProviderService : ComplicationProviderService() {
    val executor = Executors.newFixedThreadPool(5)

    override fun onComplicationUpdate(
        complicationId: Int,
        type: Int,
        callback: ComplicationUpdateCallback
    ) {
        executor.execute {
            callback.onUpdateComplication(
                when (type) {
                    ComplicationData.TYPE_SHORT_TEXT ->
                        ComplicationData.Builder(type)
                            .setShortText(ComplicationText.plainText("# $complicationId"))
                            .build()

                    ComplicationData.TYPE_LONG_TEXT ->
                        ComplicationData.Builder(type)
                            .setLongText(ComplicationText.plainText("hello $complicationId"))
                            .build()

                    else -> null
                }
            )
        }
    }

    override fun getPreviewData(type: Int) = when (type) {
        ComplicationData.TYPE_SHORT_TEXT ->
            ComplicationData.Builder(type)
                .setShortText(ComplicationText.plainText("# 123"))
                .build()

        ComplicationData.TYPE_LONG_TEXT ->
            ComplicationData.Builder(type)
                .setLongText(ComplicationText.plainText("hello 123"))
                .build()

        else -> null
    }
}