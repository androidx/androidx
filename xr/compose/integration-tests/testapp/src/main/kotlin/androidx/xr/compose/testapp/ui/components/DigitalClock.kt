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

package androidx.xr.compose.testapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.testapp.ui.theme.Purple40
import androidx.xr.compose.testapp.ui.theme.PurpleGrey80
import java.util.Calendar
import kotlin.text.padStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class DigitalClock {
    private var hours = MutableStateFlow<Int>(0)
    private var seconds = MutableStateFlow<Int>(0)
    private var minutes = MutableStateFlow<Int>(0)

    private fun timeNow() {
        val currentTime = Calendar.getInstance()
        minutes.value = currentTime.get(Calendar.MINUTE)
        hours.value = currentTime.get(Calendar.HOUR_OF_DAY)
        seconds.value = currentTime.get(Calendar.SECOND)
    }

    @Composable
    fun CreateDigitalClock() {
        // Clock
        val ss: Int by seconds.collectAsState()
        val mm: Int by minutes.collectAsState()
        val hh: Int by hours.collectAsState()

        timeNow()

        LaunchedEffect(Unit) {
            while (true) {
                delay(500)
                timeNow()
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxSize().background(color = Purple40),
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            "${hh.toString().padStart(2, '0')}:${
                            mm.toString().padStart(2, '0')
                        }:${ss.toString().padStart(2, '0')}",
                        style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold),
                        color = PurpleGrey80,
                    )
                }
            }
        }
    }
}
