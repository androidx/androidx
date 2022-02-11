/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.complications.datasource.samples

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import java.time.ZonedDateTime

/**
 * A minimal immediate complication data source. Typically this would be used to surface sensor
 * data rather than the time.
 */
class SynchronousDataSourceService : ComplicationDataSourceService() {

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val time = ZonedDateTime.now()
        listener.onComplicationData(
            if (request.immediateResponseRequired) {
                // Return different data to illustrate responseNeededSoon is true.
                when (request.complicationType) {
                    ComplicationType.SHORT_TEXT ->
                        ShortTextComplicationData.Builder(
                            plainText("S ${time.second}"),
                            ComplicationText.EMPTY
                        ).build()

                    ComplicationType.LONG_TEXT ->
                        LongTextComplicationData.Builder(
                            plainText("Secs ${time.second}"),
                            ComplicationText.EMPTY
                        ).build()

                    else -> null
                }
            } else {
                when (request.complicationType) {
                    ComplicationType.SHORT_TEXT ->
                        ShortTextComplicationData.Builder(
                            plainText("M ${time.minute}"),
                            ComplicationText.EMPTY
                        ).build()

                    ComplicationType.LONG_TEXT ->
                        LongTextComplicationData.Builder(
                            plainText("Mins ${time.minute}"),
                            ComplicationText.EMPTY
                        ).build()

                    else -> null
                }
            }
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(
                    plainText("S 10"),
                    ComplicationText.EMPTY
                ).build()

            ComplicationType.LONG_TEXT ->
                LongTextComplicationData.Builder(
                    plainText("Secs 10"),
                    ComplicationText.EMPTY
                ).build()

            else -> null
        }
    }
}