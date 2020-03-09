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
import androidx.ui.material.DataTable
import androidx.ui.material.DefaultDataTablePagination
import androidx.ui.material.DefaultDataTableSorting

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
)

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

private val extraDesserts = listOf(
    Dessert("Frozen yogurt", 159, 6.0, 24, 4.0, 87, 14, 1),
    Dessert("Ice cream sandwich", 237, 9.0, 37, 4.3, 129, 8, 1),
    Dessert("Eclair", 262, 16.0, 24, 6.0, 337, 6, 7),
    Dessert("Cupcake", 305, 3.7, 67, 4.3, 413, 3, 8),
    Dessert("Gingerbread", 356, 16.0, 49, 3.9, 327, 7, 16),
    Dessert("Jelly bean", 375, 0.0, 94, 0.0, 50, 0, 0),
    Dessert("Lollipop", 392, 0.2, 98, 0.0, 38, 0, 2),
    Dessert("Honeycomb", 408, 3.2, 87, 6.5, 562, 0, 45),
    Dessert("Donut", 452, 25.0, 51, 4.9, 326, 2, 22),
    Dessert("KitKat", 518, 26.0, 65, 7.0, 54, 12, 6),

    Dessert("Frozen yogurt with sugar", 168, 6.0, 26, 4.0, 87, 14, 1),
    Dessert("Ice cream sandwich with sugar", 246, 9.0, 39, 4.3, 129, 8, 1),
    Dessert("Eclair with sugar", 271, 16.0, 26, 6.0, 337, 6, 7),
    Dessert("Cupcake with sugar", 314, 3.7, 69, 4.3, 413, 3, 8),
    Dessert("Gingerbread with sugar", 345, 16.0, 51, 3.9, 327, 7, 16),
    Dessert("Jelly bean with sugar", 364, 0.0, 96, 0.0, 50, 0, 0),
    Dessert("Lollipop with sugar", 401, 0.2, 100, 0.0, 38, 0, 2),
    Dessert("Honeycomb with sugar", 417, 3.2, 89, 6.5, 562, 0, 45),
    Dessert("Donut with sugar", 461, 25.0, 53, 4.9, 326, 2, 22),
    Dessert("KitKat with sugar", 527, 26.0, 67, 7.0, 54, 12, 6),

    Dessert("Frozen yogurt with honey", 223, 6.0, 36, 4.0, 87, 14, 1),
    Dessert("Ice cream sandwich with honey", 301, 9.0, 49, 4.3, 129, 8, 1),
    Dessert("Eclair with honey", 326, 16.0, 36, 6.0, 337, 6, 7),
    Dessert("Cupcake with honey", 369, 3.7, 79, 4.3, 413, 3, 8),
    Dessert("Gingerbread with honey", 420, 16.0, 61, 3.9, 327, 7, 16),
    Dessert("Jelly bean with honey", 439, 0.0, 106, 0.0, 50, 0, 0),
    Dessert("Lollipop with honey", 456, 0.2, 110, 0.0, 38, 0, 2),
    Dessert("Honeycomb with honey", 472, 3.2, 99, 6.5, 562, 0, 45),
    Dessert("Donut with honey", 516, 25.0, 63, 4.9, 326, 2, 22),
    Dessert("KitKat with honey", 582, 26.0, 77, 7.0, 54, 12, 6),

    Dessert("Frozen yogurt with milk", 262, 8.4, 36, 12.0, 194, 44, 1),
    Dessert("Ice cream sandwich with milk", 339, 11.4, 49, 12.3, 236, 38, 1),
    Dessert("Eclair with milk", 365, 18.4, 36, 14.0, 444, 36, 7),
    Dessert("Cupcake with milk", 408, 6.1, 79, 12.3, 520, 33, 8),
    Dessert("Gingerbread with milk", 459, 18.4, 61, 11.9, 434, 37, 16),
    Dessert("Jelly bean with milk", 478, 2.4, 106, 8.0, 157, 30, 0),
    Dessert("Lollipop with milk", 495, 2.6, 110, 8.0, 145, 30, 2),
    Dessert("Honeycomb with milk", 511, 5.6, 99, 14.5, 669, 30, 45),
    Dessert("Donut with milk", 555, 27.4, 63, 12.9, 433, 32, 22),
    Dessert("KitKat with milk", 621, 28.4, 77, 15.0, 161, 42, 6),

    Dessert("Coconut slice and frozen yogurt", 318, 21.0, 31, 5.5, 96, 14, 7),
    Dessert("Coconut slice and ice cream sandwich", 396, 24.0, 44, 5.8, 138, 8, 7),
    Dessert("Coconut slice and eclair", 421, 31.0, 31, 7.5, 346, 6, 13),
    Dessert("Coconut slice and cupcake", 464, 18.7, 74, 5.8, 422, 3, 14),
    Dessert("Coconut slice and gingerbread", 515, 31.0, 56, 5.4, 316, 7, 22),
    Dessert("Coconut slice and jelly bean", 534, 15.0, 101, 1.5, 59, 0, 6),
    Dessert("Coconut slice and lollipop", 551, 15.2, 105, 1.5, 47, 0, 8),
    Dessert("Coconut slice and honeycomb", 567, 18.2, 94, 8.0, 571, 0, 51),
    Dessert("Coconut slice and donut", 611, 40.0, 58, 6.4, 335, 2, 28),
    Dessert("Coconut slice and KitKat", 677, 41.0, 72, 8.5, 63, 12, 12)
)

private val mutableDesserts = mutableListOf(
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
        numeric = { j -> j != 0 }
    ) {
        headerRow(text = { j -> headers[j] })

        desserts.forEach { dessert ->
            dataRow(
                text = { j ->
                    when (j) {
                        0 -> dessert.name
                        1 -> dessert.calories.toString()
                        2 -> dessert.fat.toString()
                        3 -> dessert.carbs.toString()
                        4 -> dessert.protein.toString()
                        5 -> dessert.sodium.toString()
                        6 -> dessert.calcium.toString() + "%"
                        else -> dessert.iron.toString() + "%"
                    }
                },
                selected = dessert.selected,
                onSelectedChange = { dessert.selected = it }
            )
        }
    }
}

@Sampled
@Composable
fun DataTableWithPagination() {
    DataTable(
        columns = headers.size,
        numeric = { j -> j != 0 },
        pagination = DefaultDataTablePagination(
            initialPage = 1,
            initialRowsPerPage = 7,
            availableRowsPerPage = listOf(7, 14, 28)
        )
    ) {
        headerRow(text = { j -> headers[j] })

        extraDesserts.forEach { dessert ->
            dataRow(
                text = { j ->
                    when (j) {
                        0 -> dessert.name
                        1 -> dessert.calories.toString()
                        2 -> dessert.fat.toString()
                        3 -> dessert.carbs.toString()
                        4 -> dessert.protein.toString()
                        5 -> dessert.sodium.toString()
                        6 -> dessert.calcium.toString() + "%"
                        else -> dessert.iron.toString() + "%"
                    }
                },
                selected = dessert.selected,
                onSelectedChange = { dessert.selected = it }
            )
        }
    }
}

@Sampled
@Composable
fun DataTableWithSorting() {
    DataTable(
        columns = headers.size,
        numeric = { j -> j != 0 },
        sorting = DefaultDataTableSorting(
            sortableColumns = setOf(1, 2, 3, 4, 5, 6, 7),
            onSortRequest = { sortColumn, ascending ->
                mutableDesserts.sortWith(object : Comparator<Dessert> {
                    override fun compare(p0: Dessert, p1: Dessert): Int {
                        val comparison = when (sortColumn) {
                            0 -> compareValues(p0.name, p1.name)
                            1 -> compareValues(p0.calories, p1.calories)
                            2 -> compareValues(p0.fat, p1.fat)
                            3 -> compareValues(p0.carbs, p1.carbs)
                            4 -> compareValues(p0.protein, p1.protein)
                            5 -> compareValues(p0.sodium, p1.sodium)
                            6 -> compareValues(p0.calcium, p1.calcium)
                            else -> compareValues(p0.iron, p1.iron)
                        }
                        return if (ascending) comparison else -comparison
                    }
                })
            }
        )
    ) {
        headerRow(text = { j -> headers[j] })

        mutableDesserts.forEach { dessert ->
            dataRow(
                text = { j ->
                    when (j) {
                        0 -> dessert.name
                        1 -> dessert.calories.toString()
                        2 -> dessert.fat.toString()
                        3 -> dessert.carbs.toString()
                        4 -> dessert.protein.toString()
                        5 -> dessert.sodium.toString()
                        6 -> dessert.calcium.toString() + "%"
                        else -> dessert.iron.toString() + "%"
                    }
                },
                selected = dessert.selected,
                onSelectedChange = { dessert.selected = it }
            )
        }
    }
}
