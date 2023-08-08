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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

/** Trivial example of serving [GoalProgressComplicationData]. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class GoalProgressDataSourceService : ComplicationDataSourceService() {

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        listener.onComplicationData(
            GoalProgressComplicationData.Builder(
                    value = 12345.0f,
                    targetValue = 10000.0f,
                    plainText("12345 steps")
                )
                .setText(plainText("12345"))
                .setTitle(plainText("Steps"))
                .build()
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        when (type) {
            ComplicationType.GOAL_PROGRESS ->
                GoalProgressComplicationData.Builder(
                        value = 1024.0f,
                        targetValue = 10000.0f,
                        plainText("Steps complication")
                    )
                    .setText(plainText("1024"))
                    .setTitle(plainText("Steps"))
                    .build()
            else -> null
        }
}
