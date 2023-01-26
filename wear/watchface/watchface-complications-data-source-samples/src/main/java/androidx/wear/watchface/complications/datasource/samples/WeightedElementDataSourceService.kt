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
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.WeightedElementsComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

/** Trivial example of serving [WeightedElementsComplicationData]. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class WeightedElementDataSourceService : ComplicationDataSourceService() {

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        listener.onComplicationData(
            WeightedElementsComplicationData.Builder(
                listOf(
                    WeightedElementsComplicationData.Element(1.0f, Color.RED),
                    WeightedElementsComplicationData.Element(1.0f, Color.GREEN),
                    WeightedElementsComplicationData.Element(2.0f, Color.BLUE),
                    WeightedElementsComplicationData.Element(3.0f, Color.YELLOW)
                ),
                plainText("Example weighted elements")
            ).setText(plainText("Calories"))
                .build()
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.WEIGHTED_ELEMENTS ->
            WeightedElementsComplicationData.Builder(
                listOf(
                    WeightedElementsComplicationData.Element(1.0f, Color.RED),
                    WeightedElementsComplicationData.Element(2.0f, Color.GREEN),
                    WeightedElementsComplicationData.Element(3.0f, Color.BLUE),
                ),
                plainText("Example weighted elements")
            ).setText(plainText("Calories"))
                .build()

        else -> null
    }
}