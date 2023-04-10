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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ImmersiveList

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppImmersiveList(modifier: Modifier = Modifier) {
    ImmersiveList(
        modifier = modifier
            .height(500.dp)
            .fillMaxSize(),
        background = { index, _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                val movie = topPicksForYou[index]
                LandscapeImageBackground(movie)
            }
        }
    ) {
        AppLazyRow(
            title = "",
            items = topPicksForYou,
            modifier = Modifier
        ) { movie, index, modifier ->
            ImageCard(
                movie,
//                aspectRatio = 2f / 3,
                modifier = modifier.immersiveListItem(index)
            )
        }
    }
}
