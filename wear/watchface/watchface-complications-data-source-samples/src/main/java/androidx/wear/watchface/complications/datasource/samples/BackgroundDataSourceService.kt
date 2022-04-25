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

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData

const val UPDATE_CADEANCE_MS = 10000L

var counter = 0
var updateRequester: ComplicationDataSourceUpdateRequester? = null

/** Example where we push updates to a counter every 10 seconds. */
class BackgroundDataSourceService : ComplicationDataSourceService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        if (updateRequester == null) {
            updateRequester = ComplicationDataSourceUpdateRequester.create(
                this,
                ComponentName(this, BackgroundDataSourceService::class.java)
            )
        }
    }

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        // Start requesting background updates.
        backgroundUpdate()
    }

    private fun backgroundUpdate() {
        counter++
        updateRequester?.requestUpdateAll()
        handler.postDelayed(this::backgroundUpdate, UPDATE_CADEANCE_MS)
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        listener.onComplicationData(
            when (request.complicationType) {
                ComplicationType.SHORT_TEXT ->
                    ShortTextComplicationData.Builder(
                        plainText("# $counter"),
                        ComplicationText.EMPTY
                    ).build()

                ComplicationType.LONG_TEXT ->
                    LongTextComplicationData.Builder(
                        plainText("Count $counter"),
                        ComplicationText.EMPTY
                    ).build()

                else -> null
            }
        )
    }

    override fun getPreviewData(type: ComplicationType) = when (type) {
        ComplicationType.SHORT_TEXT ->
            ShortTextComplicationData.Builder(
                plainText("# 123"),
                ComplicationText.EMPTY
            ).build()

        ComplicationType.LONG_TEXT ->
            LongTextComplicationData.Builder(
                plainText("Count 123"),
                ComplicationText.EMPTY
            ).build()

        else -> null
    }
}
