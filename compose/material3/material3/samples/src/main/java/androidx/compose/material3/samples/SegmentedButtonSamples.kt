/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Sampled
@Composable
@Preview
fun SegmentedButtonSingleSelectSample() {
    var selectedIndex by remember { mutableStateOf(0) }
    val options = listOf("Day", "Month", "Week")
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onClick = { selectedIndex = index },
                selected = index == selectedIndex
            ) {
                Text(label)
            }
        }
    }
}

@Sampled
@Composable
@Preview
fun SegmentedButtonMultiSelectSample() {
    val checkedList = remember { mutableStateListOf<Int>() }
    val options = listOf("Favorites", "Trending", "Saved")
    val icons =
        listOf(
            Icons.Filled.StarBorder,
            Icons.AutoMirrored.Filled.TrendingUp,
            Icons.Filled.BookmarkBorder
        )
    MultiChoiceSegmentedButtonRow {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = index in checkedList) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                },
                onCheckedChange = {
                    if (index in checkedList) {
                        checkedList.remove(index)
                    } else {
                        checkedList.add(index)
                    }
                },
                checked = index in checkedList
            ) {
                Text(label)
            }
        }
    }
}
