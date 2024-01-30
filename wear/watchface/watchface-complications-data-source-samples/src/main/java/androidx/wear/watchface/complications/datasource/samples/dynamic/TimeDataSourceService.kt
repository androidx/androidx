/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface.complications.datasource.samples.dynamic

import android.os.Build
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicDuration
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.DynamicComplicationText
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import java.time.Instant.EPOCH

class TimeDataSourceService : ComplicationDataSourceService() {
    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val text = PlainComplicationText.Builder("--").build()
            listener.onComplicationData(
                RangedValueComplicationData.Builder(
                        value = 0f,
                        min = 0f,
                        max = 9f,
                        contentDescription = text
                    )
                    .setText(text)
                    .build()
            )
            return
        }

        val epochDuration: DynamicDuration =
            DynamicInstant.withSecondsPrecision(EPOCH)
                .durationUntil(DynamicInstant.platformTimeWithSecondsPrecision())
        val text =
            DynamicComplicationText(
                epochDuration.minutesPart
                    .format()
                    .concat(DynamicString.constant(":"))
                    .concat(epochDuration.secondsPart.format()),
                "--"
            )

        listener.onComplicationData(
            RangedValueComplicationData.Builder(
                    dynamicValue = epochDuration.secondsPart.rem(10f),
                    fallbackValue = 0f,
                    min = 0f,
                    max = 9f,
                    contentDescription = text,
                )
                .setText(text)
                .build()
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        val text = PlainComplicationText.Builder("12:42").build()
        return RangedValueComplicationData.Builder(
                value = 2f,
                min = 0f,
                max = 9f,
                contentDescription = text,
            )
            .setText(text)
            .build()
    }
}
