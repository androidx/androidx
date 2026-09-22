/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.demos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun CardDemo() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Material 3 Card Demos", style = MaterialTheme.typography.headlineMedium)

        // 1. Custom Styled Card (Simulating Styleable Customization)
        Text("1. Custom Styled Card", style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Custom Card", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Container: TertiaryContainer, Shape: 24dp, Border: 2dp Tertiary, Elevation: 4dp",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // 2. Stateful Interactive Card (Pressed & Disabled)
        Text(
            "2. Stateful Card (Pressed & Disabled States)",
            style = MaterialTheme.typography.titleMedium,
        )
        var isCardEnabled by remember { mutableStateOf(true) }
        var interactiveTapCount by remember { mutableIntStateOf(0) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Card Enabled: $isCardEnabled")
            Switch(checked = isCardEnabled, onCheckedChange = { isCardEnabled = it })
        }

        Card(
            onClick = { interactiveTapCount++ },
            modifier = Modifier.fillMaxWidth(),
            enabled = isCardEnabled,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceDim,
                ),
            elevation =
                CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 8.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Interactive Stateful Card", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isCardEnabled)
                        "Tapped $interactiveTapCount times. Press and hold to see pressed elevation change."
                    else "Card is currently disabled (visual disabled state).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // 3. Three Canonical Variants
        Text("3. Canonical Variants", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ElevatedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Elevated", style = MaterialTheme.typography.titleSmall)
                    Text("Tonal shadow", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Outlined", style = MaterialTheme.typography.titleSmall)
                    Text("Border line", style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Filled", style = MaterialTheme.typography.titleSmall)
                    Text("Surface tint", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 4. Selectable Card Group
        Text("4. Selectable Card Group", style = MaterialTheme.typography.titleMedium)
        var selectedOption by remember { mutableStateOf("Tier 1") }
        val options =
            listOf("Tier 1" to "Basic features - $4.99/mo", "Tier 2" to "Pro features - $9.99/mo")
        // The whole card is the selection target, so the cards are wrapped in a selectable group
        // and the RadioButton is a non-clickable indicator. This exposes a single a11y node per
        // option instead of two overlapping click targets.
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            options.forEach { (title, subtitle) ->
                val isSelected = selectedOption == title
                OutlinedCard(
                    modifier =
                        Modifier.fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { selectedOption = title },
                            ),
                    border =
                        BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color =
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    colors =
                        CardDefaults.outlinedCardColors(
                            containerColor =
                                if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // 5. Action Card
        Text("5. Card with Actions", style = MaterialTheme.typography.titleMedium)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Photo Collection", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Browse through high-resolution curated photography from across the globe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = { /* explore */ }) { Text("Explore") }
                    Row {
                        IconButton(onClick = { /* favorite */ }) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Favorite")
                        }
                        IconButton(onClick = { /* share */ }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                    }
                }
            }
        }
    }
}
