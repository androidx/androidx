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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun TextWeightDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text(text = "Custom Weight") } }
        item { ListSubHeader { Text(text = "Labels") } }
        item { Text(text = "Label Small", style = MaterialTheme.typography.labelSmall) }
        item {
            Text(
                text = "Label Small",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight(800),
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item { Text(text = "Label Medium", style = MaterialTheme.typography.labelMedium) }
        item {
            Text(
                text = "Label Medium",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight(800),
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item { Text(text = "Label Large", style = MaterialTheme.typography.labelLarge) }
        item {
            Text(
                text = "Label Large",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight(800),
            )
        }

        item { ListSubHeader { Text(text = "Body") } }
        item { Text(text = "Body Extra Small", style = MaterialTheme.typography.bodyExtraSmall) }
        item {
            Text(
                text = "Body Extra Small",
                style = MaterialTheme.typography.bodyExtraSmall.copy(fontWeight = FontWeight(800)),
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item { Text(text = "Body Small", style = MaterialTheme.typography.bodySmall) }
        item {
            Text(
                text = "Body Small",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight(800)),
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { Text(text = "Body Medium", style = MaterialTheme.typography.bodyMedium) }
        item {
            Text(
                text = "Body Medium",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight(800)),
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item { Text(text = "Body Large", style = MaterialTheme.typography.bodyLarge) }
        item {
            Text(
                text = "Body Large",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(800)),
            )
        }
    }
}
