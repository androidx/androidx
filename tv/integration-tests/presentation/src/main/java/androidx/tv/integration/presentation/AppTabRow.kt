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

package androidx.tv.integration.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onSelectedTabIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRestorerModifiers = createCustomInitialFocusRestorerModifiers()

    AlignmentCenter(horizontalAxis = true) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            separator = { Spacer(modifier = Modifier.width(4.dp)) },
            modifier = modifier
                .padding(top = 20.dp)
                .then(focusRestorerModifiers.parentModifier),
//                indicator = @Composable { tabPositions ->
//                    tabPositions.getOrNull(selectedTabIndex)?.let {
//                        TabRowDefaults.PillIndicator(
//                            currentTabPosition = it,
//                            inactiveColor = Color(0xFFE5E1E6),
//                        )
//                    }
//                }
        ) {
            tabs.forEachIndexed { index, tabLabel ->
                key(index) {
                    Tab(
                        selected = selectedTabIndex == index,
                        onFocus = { onSelectedTabIndexChange(index) },
                        colors = TabDefaults.pillIndicatorTabColors(
                            inactiveContentColor = LocalContentColor.current,
//                            selectedContentColor = Color(0xFF313033),
                        ),
                        modifier = Modifier
                            .ifElse(index == 0, focusRestorerModifiers.childModifier),
                    ) {
                        Text(
                            text = tabLabel,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
