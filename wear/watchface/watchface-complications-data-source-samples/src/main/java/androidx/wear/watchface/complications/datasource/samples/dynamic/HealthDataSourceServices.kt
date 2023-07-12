/*
 * Copyright 2023 The Android Open Source Project
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
import android.os.Build
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.PlatformHealthSources
import androidx.wear.protolayout.expression.PlatformHealthSources.DynamicHeartRateAccuracy
import androidx.wear.protolayout.expression.PlatformHealthSources.HEART_RATE_ACCURACY_MEDIUM
import androidx.wear.protolayout.expression.PlatformHealthSources.heartRateAccuracy
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.DynamicComplicationText
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.samples.R

object HealthDataSourceServices {
    class Calories :
        Base(
            max = 1000f,
            title = "Calories",
        ) {
        override val value: DynamicFloat?
            get() =
                if (checkSelfPermission(permission.ACTIVITY_RECOGNITION) == PERMISSION_GRANTED) {
                    PlatformHealthSources.dailyCalories()
                } else {
                    null
                }
    }

    class Distance :
        Base(
            max = 1000f,
            title = "Distance",
        ) {
        override val value: DynamicFloat?
            get() =
                if (checkSelfPermission(permission.ACTIVITY_RECOGNITION) == PERMISSION_GRANTED) {
                    PlatformHealthSources.dailyDistanceMeters()
                } else {
                    null
                }
    }

    class Floors :
        Base(
            max = 1000f,
            title = "Floors",
        ) {
        override val value: DynamicFloat?
            get() =
                if (checkSelfPermission(permission.ACTIVITY_RECOGNITION) == PERMISSION_GRANTED) {
                    PlatformHealthSources.dailyFloors()
                } else {
                    null
                }
    }

    class HeartRate :
        Base(
            max = 200f,
            title = "HR",
        ) {
        override val value: DynamicFloat?
            get() =
                if (checkSelfPermission(permission.BODY_SENSORS) == PERMISSION_GRANTED) {
                    DynamicFloat.onCondition(
                            heartRateAccuracy()
                                .gte(DynamicHeartRateAccuracy.constant(HEART_RATE_ACCURACY_MEDIUM))
                        )
                        .use(PlatformHealthSources.heartRateBpm())
                        .elseUse(0f)
                } else {
                    null
                }

        override val text: DynamicString
            get() {
                require(checkSelfPermission(permission.BODY_SENSORS) == PERMISSION_GRANTED)
                return DynamicString.onCondition(
                        heartRateAccuracy()
                            .gte(DynamicHeartRateAccuracy.constant(HEART_RATE_ACCURACY_MEDIUM))
                    )
                    .use(PlatformHealthSources.heartRateBpm().format())
                    .elseUse(getString(R.string.dynamic_data_low_accuracy))
            }
    }

    class Steps :
        Base(
            max = 1000f,
            title = "Steps",
        ) {
        override val value: DynamicFloat?
            get() =
                if (checkSelfPermission(permission.ACTIVITY_RECOGNITION) == PERMISSION_GRANTED) {
                    PlatformHealthSources.dailySteps().asFloat()
                } else {
                    null
                }
    }

    abstract class Base(
        private val max: Float,
        title: String,
    ) : ComplicationDataSourceService() {
        private val title = PlainComplicationText.Builder(title).build()

        /** Returns [DynamicFloat] or `null` if missing permissions. */
        abstract val value: DynamicFloat?

        /** Only read if [value] is non-null. */
        open val text: DynamicString
            get() = value!!.format()

        override fun onComplicationRequest(
            request: ComplicationRequest,
            listener: ComplicationRequestListener
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val text =
                    PlainComplicationText.Builder(getString(R.string.dynamic_data_not_supported))
                        .build()
                listener.onComplicationData(
                    RangedValueComplicationData.Builder(
                            value = 0f,
                            min = 0f,
                            max = max,
                            contentDescription = text
                        )
                        .setTitle(title)
                        .setText(text)
                        .build()
                )
                return
            }

            val value = value
            if (value == null) {
                // Missing permission.
                val text =
                    PlainComplicationText.Builder(getString(R.string.dynamic_data_no_permission))
                        .build()

                listener.onComplicationData(
                    RangedValueComplicationData.Builder(
                            value = 0f,
                            min = 0f,
                            max = max,
                            contentDescription = text,
                        )
                        .setTitle(title)
                        .setText(text)
                        .build()
                )
                return
            }

            val text =
                DynamicComplicationText(this.text, getString(R.string.dynamic_data_not_supported))
            val fallbackText =
                PlainComplicationText.Builder(
                        getString(R.string.dynamic_data_not_available_or_ready)
                    )
                    .build()

            listener.onComplicationData(
                RangedValueComplicationData.Builder(
                        dynamicValue =
                            DynamicFloat.onCondition(value.gt(max))
                                .use(DynamicFloat.constant(max))
                                .elseUse(value),
                        fallbackValue = 0f,
                        min = 0f,
                        max = max,
                        contentDescription = text,
                    )
                    .setTitle(title)
                    .setText(text)
                    .setDynamicValueInvalidationFallback(
                        RangedValueComplicationData.Builder(
                                value = 0f,
                                min = 0f,
                                max = max,
                                contentDescription = fallbackText,
                            )
                            .setTitle(title)
                            .setText(fallbackText)
                            .build()
                    )
                    .build()
            )
        }

        override fun getPreviewData(type: ComplicationType): ComplicationData {
            val value = max / 4
            val text = PlainComplicationText.Builder(value.toInt().toString()).build()
            return RangedValueComplicationData.Builder(
                    value = value,
                    min = 0f,
                    max = max,
                    contentDescription = text,
                )
                .setTitle(title)
                .setText(text)
                .build()
        }
    }
}
