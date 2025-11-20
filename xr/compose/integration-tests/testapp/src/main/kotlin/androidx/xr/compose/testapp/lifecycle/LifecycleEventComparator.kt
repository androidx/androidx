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
package androidx.xr.compose.testapp.lifecycle

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LifecycleEventComparator(
    activityName: String,
    expectedEvents: List<String>,
    actualEvents: List<String>,
) {
    Log.i(
        TAG,
        "[${activityName.replace("activities.", "")}] ${if(expectedEvents == actualEvents) "[PASS]" else "[FAIL]"} Lifecycle sequence: $expectedEvents",
    )

    val customTextStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 30.sp)

    CompositionLocalProvider(LocalTextStyle provides customTextStyle) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Text("Expected Events:", Modifier.width(250.dp), fontWeight = FontWeight.SemiBold)
                Text(
                    text = expectedEvents.joinToString(", "),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Row {
                Text("Actual Events:", Modifier.width(250.dp), fontWeight = FontWeight.SemiBold)
                Text(
                    text = actualEvents.joinToString(", "),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Row {
                Text("Result:", Modifier.width(250.dp), fontWeight = FontWeight.SemiBold)
                if (expectedEvents == actualEvents) {
                    Text(
                        text = "[Pass]",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50),
                    )
                } else {
                    Text(text = "[Fail]", fontWeight = FontWeight.SemiBold, color = Color.Red)
                }
            }
        }
    }
}
