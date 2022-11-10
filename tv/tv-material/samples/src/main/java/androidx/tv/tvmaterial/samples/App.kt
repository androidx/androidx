/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.tv.tvmaterial.samples

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    var selectedTab by remember { mutableStateOf(navigationMap[Navigation.FeaturedCarousel]) }

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TopNavigation(updateSelectedTab = { selectedTab = it })
        when (reverseNavigationMap[selectedTab]) {
            Navigation.FeaturedCarousel -> FeaturedCarousel()
            Navigation.ImmersiveList -> SampleImmersiveList()
            Navigation.LazyRowsAndColumns -> LazyRowsAndColumns()
            else -> { }
        }
    }
}