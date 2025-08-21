/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import java.time.DayOfWeek

@Sampled
fun stateSetSample() {
    @Composable
    fun DaysForAlarm() {
        val days = remember { mutableStateSetOf<DayOfWeek>() }

        Column(Modifier.selectableGroup()) {
            DayOfWeek.entries.forEach { dayOfWeek ->
                Row(
                    modifier =
                        Modifier.toggleable(
                            value = dayOfWeek in days,
                            role = Role.Checkbox,
                            onValueChange = {
                                if (it) {
                                    days.add(dayOfWeek)
                                } else {
                                    days.remove(dayOfWeek)
                                }
                            },
                        )
                ) {
                    Checkbox(checked = dayOfWeek in days, onCheckedChange = null)
                    Text(text = dayOfWeek.name)
                }
            }
        }
    }
}
