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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Test

@MediumTest
class LazyStaggeredGridLayoutInfoTest : BaseLazyStaggeredGridWithOrientation(Orientation.Vertical) {

    @Test
    fun contentTypeIsCorrect() {
        val state = LazyStaggeredGridState()
        rule.setContent {
            LazyStaggeredGrid(
                lanes = 1,
                state = state,
                modifier = Modifier.requiredSize(30.dp)
            ) {
                items(2, contentType = { it }) {
                    Box(Modifier.size(10.dp))
                }
                item {
                    Box(Modifier.size(10.dp))
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(state.layoutInfo.visibleItemsInfo.map { it.contentType })
                .isEqualTo(listOf(0, 1, null))
        }
    }
}