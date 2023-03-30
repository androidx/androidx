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

package androidx.wear.watchface.complications.datasource.samples

import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.wear.watchface.complications.data.ColorRamp
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

/** Trivial example of serving [RangedValueComplicationData] with a non-interpolated ramp. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class NonInterpolatedColorRampDataSourceService : ComplicationDataSourceService() {

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        listener.onComplicationData(
            RangedValueComplicationData.Builder(
                    value = 75f,
                    min = 0.0f,
                    max = 100.0f,
                    plainText("Example")
                )
                .setText(plainText("Example"))
                .setValueType(RangedValueComplicationData.TYPE_RATING)
                .setColorRamp(
                    ColorRamp(
                        intArrayOf(Color.GREEN, Color.YELLOW, Color.RED),
                        interpolated = false
                    )
                )
                .build()
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        when (type) {
            ComplicationType.RANGED_VALUE ->
                RangedValueComplicationData.Builder(
                        value = 10f,
                        min = 0.0f,
                        max = 100.0f,
                        plainText("Example")
                    )
                    .setText(plainText("Example"))
                    .setValueType(RangedValueComplicationData.TYPE_RATING)
                    .setColorRamp(
                        ColorRamp(
                            intArrayOf(Color.GREEN, Color.YELLOW, Color.RED),
                            interpolated = false
                        )
                    )
                    .build()
            else -> null
        }
}
