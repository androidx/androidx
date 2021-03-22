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

package androidx.wear.complications.provider.samples

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.wear.complications.ComplicationProviderService
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.LongTextComplicationData
import androidx.wear.complications.data.ShortTextComplicationData
import java.util.concurrent.Executors

/** A minimal complication provider which reports the ID of the complication asynchronously. */
class AsynchronousProviderService : ComplicationProviderService() {
    val executor = Executors.newFixedThreadPool(5)

    override fun onComplicationUpdate(
        complicationId: Int,
        type: ComplicationType,
        listener: ComplicationUpdateListener
    ) {
        executor.execute {
            listener.onUpdateComplication(
                when (type) {
                    ComplicationType.SHORT_TEXT ->
                        ShortTextComplicationData.Builder(plainText("# $complicationId")).build()

                    ComplicationType.LONG_TEXT ->
                        LongTextComplicationData.Builder(
                            plainText(
                                SpannableString("hello $complicationId").apply {
                                    setSpan(
                                        ForegroundColorSpan(Color.RED),
                                        0,
                                        5,
                                        Spanned.SPAN_INCLUSIVE_INCLUSIVE
                                    )
                                }
                            )
                        ).build()

                    else -> null
                }
            )
        }
    }

    override fun getPreviewData(type: ComplicationType) = when (type) {
        ComplicationType.SHORT_TEXT ->
            ShortTextComplicationData.Builder(plainText("# 123")).build()

        ComplicationType.LONG_TEXT ->
            LongTextComplicationData.Builder(
                plainText(
                    SpannableString("hello 123").apply {
                        setSpan(
                            ForegroundColorSpan(Color.RED),
                            0,
                            5,
                            Spanned.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    }
                )
            ).build()

        else
        -> null
    }
}
