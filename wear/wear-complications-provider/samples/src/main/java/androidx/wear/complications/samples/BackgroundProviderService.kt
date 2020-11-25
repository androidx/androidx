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

import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import androidx.wear.complications.ComplicationProviderService
import androidx.wear.complications.ProviderUpdateRequester
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.LongTextComplicationData
import androidx.wear.complications.data.ShortTextComplicationData

const val UPDATE_CADEANCE_MS = 10000L

var counter = 0
var updateRequester: ProviderUpdateRequester? = null

/** Example where we push updates to a counter every 10 seconds. */
class BackgroundProviderService : ComplicationProviderService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        if (updateRequester == null) {
            updateRequester = ProviderUpdateRequester(
                this,
                ComponentName(this, BackgroundProviderService::class.java)
            )
        }
    }

    override fun onComplicationActivated(complicationId: Int, type: Int) {
        // Start requesting background updates.
        backgroundUpdate()
    }

    private fun backgroundUpdate() {
        counter++
        updateRequester?.requestUpdateAll()
        handler.postDelayed(this::backgroundUpdate, UPDATE_CADEANCE_MS)
    }

    override fun onComplicationUpdate(
        complicationId: Int,
        type: ComplicationType,
        callback: ComplicationUpdateCallback
    ) {
        callback.onUpdateComplication(
            when (type) {
                ComplicationType.SHORT_TEXT ->
                    ShortTextComplicationData.Builder(
                        ComplicationText.plain("# $counter")
                    ).build()

                ComplicationType.LONG_TEXT ->
                    LongTextComplicationData.Builder(
                        ComplicationText.plain("Count $counter")
                    ).build()

                else -> null
            }
        )
    }

    override fun getPreviewData(type: ComplicationType) = when (type) {
        ComplicationType.SHORT_TEXT ->
            ShortTextComplicationData.Builder(
                ComplicationText.plain("# 123")
            ).build()

        ComplicationType.LONG_TEXT ->
            LongTextComplicationData.Builder(
                ComplicationText.plain("Count 123")
            ).build()

        else -> null
    }
}