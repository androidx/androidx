/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.composer
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.material.DataRow
import androidx.ui.material.DataTable

@Model
private data class Dessert(
    val name: String,
    val calories: Int,
    val fat: Double,
    val carbs: Int,
    val protein: Double,
    val sodium: Int,
    val calcium: Int,
    val iron: Int,
    var selected: Boolean = false
) {
    fun onSelectedChange(newValue: Boolean) {
        selected = newValue
    }
}

private val headers = listOf(
    "Dessert",
    "Calories",
    "Fat (g)",
    "Carbs (g)",
    "Protein (g)",
    "Sodium (mg)",
    "Calcium (%)",
    "Iron (%)"
)

private val desserts = listOf(
    Dessert("Frozen yogurt", 159, 6.0, 24, 4.0, 87, 14, 1),
    Dessert("Ice cream sandwich", 237, 9.0, 37, 4.3, 129, 8, 1),
    Dessert("Eclair", 262, 16.0, 24, 6.0, 337, 6, 7),
    Dessert("Cupcake", 305, 3.7, 67, 4.3, 413, 3, 8),
    Dessert("Gingerbread", 356, 16.0, 49, 3.9, 327, 7, 16),
    Dessert("Jelly bean", 375, 0.0, 94, 0.0, 50, 0, 0),
    Dessert("Lollipop", 392, 0.2, 98, 0.0, 38, 0, 2),
    Dessert("Honeycomb", 408, 3.2, 87, 6.5, 562, 0, 45),
    Dessert("Donut", 452, 25.0, 51, 4.9, 326, 2, 22),
    Dessert("KitKat", 518, 26.0, 65, 7.0, 54, 12, 6)
)

@Sampled
@Composable
fun SimpleDataTable() {
    DataTable(
        columns = headers.size,
        header = { j -> Text(text = headers[j]) },
        numeric = { j -> j != 0 },
        rows = desserts.map { dessert ->
            DataRow(
                children = { j ->
                    Text(
                        text = when (j) {
                            0 -> dessert.name
                            1 -> dessert.calories.toString()
                            2 -> dessert.fat.toString()
                            3 -> dessert.carbs.toString()
                            4 -> dessert.protein.toString()
                            5 -> dessert.sodium.toString()
                            6 -> dessert.calcium.toString() + "%"
                            else -> dessert.iron.toString() + "%"
                        }
                    )
                },
                selected = dessert.selected,
                onSelectedChange = { dessert.onSelectedChange(it) }
            )
        }
    )
}
