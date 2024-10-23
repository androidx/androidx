/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.macrobenchmark.common.baselineprofile

import android.os.Build
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.DatePickerType
import androidx.wear.compose.material3.macrobenchmark.common.MacrobenchmarkScreen
import java.time.LocalDate

val DatePickerScreen =
    object : MacrobenchmarkScreen {
        override val content: @Composable BoxScope.() -> Unit
            get() = {
                if (Build.VERSION.SDK_INT >= 26) {
                    val minDate = LocalDate.of(2022, 10, 15)
                    val maxDate = LocalDate.of(2025, 2, 4)
                    DatePicker(
                        initialDate = LocalDate.of(2024, 9, 2),
                        onDatePicked = {},
                        minDate = minDate,
                        maxDate = maxDate,
                        datePickerType = DatePickerType.YearMonthDay
                    )
                }
            }
    }
