/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberScrollFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Sampled
@Composable
@Preview
fun ScrollFieldSample() {
    val minVal = 1000
    val maxVal = 2000
    val itemCount = (maxVal - minVal) + 1

    val state = rememberScrollFieldState(itemCount = itemCount, index = 0)
    var selectedValue by remember { mutableIntStateOf(minVal) }

    Row(
        modifier =
            Modifier.background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(28.dp),
                )
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScrollField(
            state = state,
            modifier = Modifier.size(width = 192.dp, height = 160.dp),
            contentDescription = "Select value between 1000 and 2000",
            fieldAccessibilityDescription = { index -> (minVal + index).toString() },
            field = { index, isSelected ->
                val valueToShow = minVal + index
                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(
                        text = valueToShow.toString(),
                        style =
                            if (isSelected) {
                                MaterialTheme.typography.displayLargeEmphasized
                            } else {
                                MaterialTheme.typography.displayMedium
                            },
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                    )
                }
            },
        )
    }
}

@Sampled
@Composable
@Preview
fun TimeScrollFieldSample() {
    val hourCount = 24
    val minuteCount = 60

    val hourState = rememberScrollFieldState(itemCount = hourCount, index = 12)
    val minuteState = rememberScrollFieldState(itemCount = minuteCount, index = 30)

    var selectedHour by remember { mutableIntStateOf(12) }
    var selectedMinute by remember { mutableIntStateOf(30) }

    Row(
        modifier =
            Modifier.background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(28.dp),
                )
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScrollField(
            state = hourState,
            modifier = Modifier.size(width = 100.dp, height = 120.dp),
            contentDescription = "Select hour",
            fieldAccessibilityDescription = { index ->
                if (index == 1) "$index hour" else "$index hours"
            },
        )

        Text(
            text = ":",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-4).dp).semantics { hideFromAccessibility() },
        )

        ScrollField(
            state = minuteState,
            modifier = Modifier.size(width = 100.dp, height = 120.dp),
            contentDescription = "Select minute",
            fieldAccessibilityDescription = { index ->
                if (index == 1) "$index minute" else "$index minutes"
            },
        )
    }
}

@Sampled
@Composable
@Preview
fun UnitScrollFieldSample() {
    val amountCount = 100 // 0.1 to 10.0
    val unitCount = 2 // L, oz

    val amountState = rememberScrollFieldState(itemCount = amountCount, index = 26) // 2.7
    val unitState = rememberScrollFieldState(itemCount = unitCount, index = 0) // L

    val units = listOf("L", "oz")

    Row(
        modifier =
            Modifier.background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(28.dp),
                )
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScrollField(
            state = amountState,
            modifier = Modifier.size(width = 120.dp, height = 160.dp),
            contentDescription = "Select amount",
            fieldAccessibilityDescription = { index -> ((index + 1) / 10.0).toString() },
            field = { index, isSelected ->
                val amount = (index + 1) / 10.0
                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "${(amount * 10).toInt() / 10.0}",
                        style =
                            if (isSelected) {
                                MaterialTheme.typography.displayLargeEmphasized
                            } else {
                                MaterialTheme.typography.displayMedium
                            },
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                    )
                }
            },
        )

        ScrollField(
            state = unitState,
            modifier = Modifier.size(width = 120.dp, height = 160.dp),
            contentDescription = "Select unit",
            fieldAccessibilityDescription = { index -> units[index] },
            field = { index, isSelected ->
                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(
                        text = units[index],
                        style =
                            if (isSelected) {
                                MaterialTheme.typography.displayLargeEmphasized
                            } else {
                                MaterialTheme.typography.displayMedium
                            },
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                    )
                }
            },
        )
    }
}
