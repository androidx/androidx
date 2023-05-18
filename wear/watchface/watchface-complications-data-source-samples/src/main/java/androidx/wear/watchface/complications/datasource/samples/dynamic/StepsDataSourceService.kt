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

import android.Manifest.permission
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.PlatformHealthSources
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.DynamicComplicationText
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

class StepsDataSourceService : ComplicationDataSourceService() {
    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val fallbackText = PlainComplicationText.Builder("--").build()

        if (checkSelfPermission(permission.ACTIVITY_RECOGNITION) != PERMISSION_GRANTED) {
            listener.onComplicationData(
                RangedValueComplicationData.Builder(
                        value = 0f,
                        min = 0f,
                        max = 1000f,
                        contentDescription = fallbackText,
                    )
                    .setText(fallbackText)
                    .build()
            )
            return
        }

        val value = PlatformHealthSources.dailySteps()
        val text = DynamicComplicationText(value.format(), "--")

        listener.onComplicationData(
            RangedValueComplicationData.Builder(
                    dynamicValue =
                        DynamicFloat.onCondition(value.gt(1000))
                            .use(DynamicFloat.constant(1000f))
                            .elseUse(value.asFloat()),
                    fallbackValue = 0f,
                    min = 0f,
                    max = 1000f,
                    contentDescription = text,
                )
                .setText(text)
                .setDynamicValueInvalidationFallback(
                    RangedValueComplicationData.Builder(
                            value = 0f,
                            min = 0f,
                            max = 200f,
                            contentDescription = fallbackText,
                        )
                        .setText(fallbackText)
                        .build()
                )
                .build()
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        val text = PlainComplicationText.Builder("123").build()
        return RangedValueComplicationData.Builder(
                value = 123f,
                min = 0f,
                max = 1000f,
                contentDescription = text,
            )
            .setText(text)
            .build()
    }
}
