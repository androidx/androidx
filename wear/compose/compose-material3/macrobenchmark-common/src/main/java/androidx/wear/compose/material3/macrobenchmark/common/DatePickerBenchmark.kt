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

package androidx.wear.compose.material3.macrobenchmark.common

import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.DatePickerType
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@RequiresApi(Build.VERSION_CODES.O)
object DatePickerBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            var showDatePicker by remember { mutableStateOf(false) }
            var datePickerDate by remember { mutableStateOf(LocalDate.of(2024, 9, 2)) }
            val formatter =
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(LocalConfiguration.current.locales[0])
            val minDate = LocalDate.of(2022, 10, 30)
            val maxDate = LocalDate.of(2025, 2, 4)
            if (showDatePicker) {
                DatePicker(
                    initialDate = datePickerDate, // Initialize with last picked date on reopen
                    onDatePicked = {
                        datePickerDate = it
                        showDatePicker = false
                    },
                    minValidDate = minDate,
                    maxValidDate = maxDate,
                    datePickerType = DatePickerType.YearMonthDay
                )
            } else {
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.semantics { contentDescription = CONTENT_DESCRIPTION },
                    label = { Text("Selected Date") },
                    secondaryLabel = { Text(datePickerDate.format(formatter)) },
                    icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit") },
                )
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            device
                .wait(Until.findObject(By.desc(CONTENT_DESCRIPTION)), FIND_OBJECT_TIMEOUT_MS)
                .click()
            device.waitForIdle()
            SystemClock.sleep(500)
            repeat(3) { columnIndex ->
                repeat(20) {
                    val endY = device.displayHeight * 9 / 10 // scroll down
                    device.swipe(
                        device.displayWidth / 2,
                        device.displayHeight / 2,
                        device.displayWidth / 2,
                        endY,
                        10
                    )
                    device.waitForIdle()
                    SystemClock.sleep(500)
                }
                if (columnIndex < 2) {
                    device.wait(Until.findObject(By.desc("Next")), FIND_OBJECT_TIMEOUT_MS).click()
                    device.waitForIdle()
                    SystemClock.sleep(500)
                } else {
                    device
                        .wait(Until.findObject(By.desc("Confirm")), FIND_OBJECT_TIMEOUT_MS)
                        .click()
                    device.waitForIdle()
                    SystemClock.sleep(500)
                }
            }
        }
}
